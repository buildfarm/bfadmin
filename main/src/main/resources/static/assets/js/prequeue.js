/**
 * BuildFarm Prequeue JavaScript
 * Handles async data loading, table sorting and delete operations for the prequeue table
 */

document.addEventListener('DOMContentLoaded', function() {
    // Load prequeue data asynchronously
    loadPrequeueData();
});

// Global variables
let allOperations = [];
let sortDirection = {};

// Load prequeue data from async API
async function loadPrequeueData() {
    try {
        const response = await fetch('/api/prequeue/data');
        const data = await response.json();
        
        if (data.error) {
            showError(data.error);
            return;
        }
        
        // Store the data
        allOperations = data.operations || [];
        
        // Update UI
        updatePrequeueSummary();
        loadOperationsTable();
        
    } catch (error) {
        console.error('Failed to load prequeue data:', error);
        showError('Failed to load prequeue data: ' + error.message);
    }
}

// Update prequeue summary
function updatePrequeueSummary() {
    const operationCount = document.getElementById('operationCount');
    const noOperationsAlert = document.getElementById('noPrequeueOperationsAlert');
    
    if (allOperations.length === 0) {
        noOperationsAlert.classList.remove('d-none');
        operationCount.textContent = '0 Operations';
        return;
    }
    
    operationCount.textContent = `${allOperations.length} Operations`;
}

// Load operations table
function loadOperationsTable() {
    const loading = document.getElementById('prequeueLoading');
    const container = document.getElementById('prequeueTableContainer');
    const noOperationsAlert = document.getElementById('noPrequeueOperationsAlert');
    
    loading.classList.add('d-none');
    
    if (allOperations.length === 0) {
        noOperationsAlert.classList.remove('d-none');
        return;
    }
    
    container.classList.remove('d-none');
    populateOperationsTable();
    setupSortingListeners();
}

// Populate operations table
function populateOperationsTable() {
    const table = document.getElementById('prequeueTable');
    if (!table) return;
    
    const tbody = table.querySelector('tbody');
    tbody.innerHTML = '';
    
    allOperations.forEach((operation, index) => {
        const row = document.createElement('tr');
        row.className = 'operation-row';
        
        const priorityBadgeClass = getPriorityBadgeClass(operation.priority);
        const stageBadgeClass = getStageStatusClass(operation.stage);
        const stageIcon = getStageIcon(operation.stage);
        
        row.innerHTML = `
            <td><span class="text-muted">${index + 1}</span></td>
            <td>
                <span class="text-info">${operation.operationName || operation.name}</span>
            </td>
            <td>
                <span class="priority-badge ${priorityBadgeClass}">
                    <i class="bi bi-arrow-up"></i>
                    <span>${operation.priority || 'NORMAL'}</span>
                </span>
            </td>
            <td>
                <span class="status-badge ${stageBadgeClass}">
                    <i class="bi bi-${stageIcon}"></i>
                    <span>${operation.stage || 'QUEUED'}</span>
                </span>
            </td>
            <td>
                <small class="text-muted">${operation.queuedTimestamp || operation.queuedAt || '-'}</small>
            </td>
            <td>
                <button class="btn btn-sm btn-outline-danger" 
                        onclick="deleteOperation(this)"
                        data-name="${operation.operationName || operation.name}"
                        title="Delete operation">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Get priority badge CSS class
function getPriorityBadgeClass(priority) {
    switch(priority) {
        case 'HIGH': return 'priority-high';
        case 'URGENT': return 'priority-urgent';
        case 'LOW': return 'priority-low';
        default: return 'priority-normal';
    }
}

// Get stage status CSS class
function getStageStatusClass(stage) {
    switch(stage) {
        case 'QUEUED': return 'status-warning';
        case 'EXECUTING': return 'status-info';
        case 'COMPLETED': return 'status-success';
        case 'FAILED': return 'status-danger';
        default: return 'status-secondary';
    }
}

// Get stage icon
function getStageIcon(stage) {
    switch(stage) {
        case 'QUEUED': return 'clock';
        case 'EXECUTING': return 'gear';
        case 'COMPLETED': return 'check-circle';
        case 'FAILED': return 'x-circle';
        default: return 'question-circle';
    }
}

// Set up sorting listeners
function setupSortingListeners() {
    const sortableHeaders = document.querySelectorAll('.sortable-header');
    sortableHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const column = this.getAttribute('data-column');
            sortOperationTable(column);
        });
    });
}

// Sort operations table
function sortOperationTable(column) {
    const tableBody = document.querySelector('#prequeueTable tbody');
    if (!tableBody) return;
    
    const rows = Array.from(tableBody.querySelectorAll('tr'));
    
    // Toggle sort direction
    sortDirection[column] = sortDirection[column] === 'asc' ? 'desc' : 'asc';
    const isAsc = sortDirection[column] === 'asc';
    
    // Update sort indicators
    const headers = document.querySelectorAll('.sortable-header');
    headers.forEach(header => {
        header.classList.remove('sorted-asc', 'sorted-desc');
    });
    
    const currentHeader = document.querySelector(`[data-column="${column}"]`);
    if (currentHeader) {
        currentHeader.classList.add(isAsc ? 'sorted-asc' : 'sorted-desc');
    }
    
    // Sort rows
    rows.sort((a, b) => {
        let aVal = '', bVal = '';
        
        // Get values based on column
        switch(column) {
            case 'index':
                aVal = parseInt(a.cells[0].textContent) || 0;
                bVal = parseInt(b.cells[0].textContent) || 0;
                return isAsc ? aVal - bVal : bVal - aVal;
            case 'operationName':
                aVal = a.cells[1].textContent.trim();
                bVal = b.cells[1].textContent.trim();
                break;
            case 'priority':
                aVal = getPriorityValue(a.cells[2].textContent.trim());
                bVal = getPriorityValue(b.cells[2].textContent.trim());
                return isAsc ? aVal - bVal : bVal - aVal;
            case 'stage':
                aVal = a.cells[3].textContent.trim();
                bVal = b.cells[3].textContent.trim();
                break;
            case 'queuedAt':
                aVal = a.cells[4].textContent.trim();
                bVal = b.cells[4].textContent.trim();
                break;
            default:
                return 0;
        }
        
        if (typeof aVal === 'string') {
            aVal = aVal.toLowerCase();
            bVal = bVal.toLowerCase();
            return isAsc ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
        }
        
        return isAsc ? aVal - bVal : bVal - aVal;
    });
    
    // Re-append sorted rows
    rows.forEach((row, index) => {
        tableBody.appendChild(row);
        // Update index numbers
        row.cells[0].textContent = index + 1;
    });
}

// Get priority value for sorting
function getPriorityValue(priority) {
    switch(priority) {
        case 'URGENT': return 4;
        case 'HIGH': return 3;
        case 'NORMAL': return 2;
        case 'LOW': return 1;
        default: return 2;
    }
}

// Delete operation from Redis prequeue
function deleteOperation(button) {
    const operationName = button.getAttribute('data-name');
    
    if (!operationName) {
        showTemporaryMessage('Operation name not found', 'error');
        return;
    }
    
    // Confirm deletion
    if (!confirm(`Are you sure you want to delete operation "${operationName}" from prequeue?`)) {
        return;
    }
    
    // Get the table row
    const row = button.closest('tr');
    
    // Show loading state on button
    const originalContent = button.innerHTML;
    button.innerHTML = '<i class="bi bi-hourglass-split"></i>';
    button.disabled = true;
    
    // Make delete request
    fetch(`/api/prequeue/operations/${encodeURIComponent(operationName)}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        } else {
            return response.text().then(text => {
                throw new Error(`HTTP ${response.status}: ${text || response.statusText}`);
            });
        }
    })
    .then(data => {
        if (data.success) {
            // Remove the row from the table
            row.remove();
            
            // Update operation count
            const remainingRows = document.querySelectorAll('#prequeueTable tbody tr').length;
            const countElements = document.querySelectorAll('#operationCount, .operation-count');
            countElements.forEach(element => {
                element.textContent = remainingRows + ' Operations';
            });
            
            // Re-index remaining rows
            const rows = document.querySelectorAll('#prequeueTable tbody tr');
            rows.forEach((row, index) => {
                row.cells[0].textContent = index + 1;
            });
            
            // Update global data
            allOperations = allOperations.filter(op => 
                (op.operationName || op.name) !== operationName
            );
            
            // Show success message
            showTemporaryMessage('Operation deleted successfully', 'success');
        } else {
            throw new Error(data.message || 'Delete operation failed');
        }
    })
    .catch(error => {
        console.error('Error deleting operation:', error);
        showTemporaryMessage(`Failed to delete operation: ${error.message}`, 'error');
        
        // Restore button
        button.innerHTML = originalContent;
        button.disabled = false;
    });
}

// Show temporary message to user
function showTemporaryMessage(message, type) {
    // Create alert element
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type === 'success' ? 'success' : 'danger'} alert-dismissible fade show`;
    alertDiv.innerHTML = `
        <i class="bi bi-${type === 'success' ? 'check-circle' : 'exclamation-triangle'}"></i>
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    // Insert at top of content
    const content = document.getElementById('content');
    content.insertBefore(alertDiv, content.firstChild);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.remove();
        }
    }, 5000);
}

// Show error message
function showError(message) {
    const content = document.getElementById('content');
    const errorDiv = document.createElement('div');
    errorDiv.className = 'alert alert-danger';
    errorDiv.innerHTML = `
        <i class="bi bi-exclamation-triangle"></i>
        <strong>Error:</strong> ${message}
    `;
    content.insertBefore(errorDiv, content.firstChild);
}