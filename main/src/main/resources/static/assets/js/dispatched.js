/**
 * BuildFarm Dispatched JavaScript
 * Handles async data loading, table sorting and delete operations for the dispatched table
 */

document.addEventListener('DOMContentLoaded', function() {
    // Load dispatched data asynchronously
    loadDispatchedData();
});

// Global variables
let allOperations = [];
let sortDirection = {};

// Load dispatched data from async API
async function loadDispatchedData() {
    try {
        const response = await fetch('/api/dispatched/data');
        const data = await response.json();
        
        if (data.error) {
            showError(data.error);
            return;
        }
        
        // Store the data
        allOperations = data.operations || [];
        
        // Update UI
        updateDispatchedSummary();
        loadOperationsTable();
        
    } catch (error) {
        console.error('Failed to load dispatched data:', error);
        showError('Failed to load dispatched data: ' + error.message);
    }
}

// Update dispatched summary
function updateDispatchedSummary() {
    const operationCount = document.getElementById('dispatchedCount');
    const noOperationsAlert = document.getElementById('noDispatchedOperationsAlert');
    
    if (allOperations.length === 0) {
        noOperationsAlert.classList.remove('d-none');
        operationCount.textContent = '0 Operations';
        return;
    }
    
    operationCount.textContent = `${allOperations.length} Operations`;
}

// Load operations table
function loadOperationsTable() {
    const loading = document.getElementById('dispatchedLoading');
    const container = document.getElementById('dispatchedTableContainer');
    const noOperationsAlert = document.getElementById('noDispatchedOperationsAlert');
    
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
    const table = document.getElementById('dispatchedTable');
    if (!table) return;
    
    const tbody = table.querySelector('tbody');
    tbody.innerHTML = '';
    
    allOperations.forEach((operation, index) => {
        const row = document.createElement('tr');
        row.className = 'operation-row';
        
        const statusBadgeClass = getStatusBadgeClass(operation.status);
        const statusIcon = getStatusIcon(operation.status);
        const workerBadgeClass = getWorkerBadgeClass(operation.workerName);
        
        row.innerHTML = `
            <td><span class="text-muted">${index + 1}</span></td>
            <td>
                <span class="text-info">${operation.operationName || operation.name}</span>
            </td>
            <td>
                <span class="worker-badge ${workerBadgeClass}">
                    <i class="bi bi-cpu"></i>
                    <span>${operation.workerName || 'Unknown'}</span>
                </span>
            </td>
            <td>
                <span class="status-badge ${statusBadgeClass}">
                    <i class="bi bi-${statusIcon}"></i>
                    <span>${operation.status || 'UNKNOWN'}</span>
                </span>
            </td>
            <td>
                <small class="text-muted">${operation.dispatchedTimestamp || operation.dispatchedAt || '-'}</small>
            </td>
            <td>
                <small class="text-muted">${operation.completedTimestamp || operation.completedAt || '-'}</small>
            </td>
            <td>
                <button class="btn btn-sm btn-outline-danger" 
                        onclick="deleteOperation(this)"
                        data-name="${operation.operationName || operation.name}"
                        data-worker="${operation.workerName}"
                        title="Delete operation">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Get status badge CSS class
function getStatusBadgeClass(status) {
    switch(status) {
        case 'COMPLETED': return 'status-success';
        case 'FAILED': return 'status-danger';
        case 'EXECUTING': return 'status-info';
        case 'CANCELLED': return 'status-warning';
        default: return 'status-secondary';
    }
}

// Get status icon
function getStatusIcon(status) {
    switch(status) {
        case 'COMPLETED': return 'check-circle';
        case 'FAILED': return 'x-circle';
        case 'EXECUTING': return 'gear';
        case 'CANCELLED': return 'dash-circle';
        default: return 'question-circle';
    }
}

// Get worker badge CSS class
function getWorkerBadgeClass(workerName) {
    if (!workerName || workerName === 'Unknown') {
        return 'worker-unknown';
    }
    
    // Color based on worker type
    if (workerName.includes('cpu')) {
        return 'worker-cpu';
    } else if (workerName.includes('storage')) {
        return 'worker-storage';
    } else {
        return 'worker-generic';
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
    const tableBody = document.querySelector('#dispatchedTable tbody');
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
            case 'workerName':
                aVal = a.cells[2].textContent.trim();
                bVal = b.cells[2].textContent.trim();
                break;
            case 'status':
                aVal = getStatusValue(a.cells[3].textContent.trim());
                bVal = getStatusValue(b.cells[3].textContent.trim());
                return isAsc ? aVal - bVal : bVal - aVal;
            case 'dispatchedAt':
                aVal = a.cells[4].textContent.trim();
                bVal = b.cells[4].textContent.trim();
                break;
            case 'completedAt':
                aVal = a.cells[5].textContent.trim();
                bVal = b.cells[5].textContent.trim();
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

// Get status value for sorting
function getStatusValue(status) {
    switch(status) {
        case 'EXECUTING': return 4;
        case 'COMPLETED': return 3;
        case 'FAILED': return 2;
        case 'CANCELLED': return 1;
        default: return 0;
    }
}

// Delete operation from Redis dispatched
function deleteOperation(button) {
    const operationName = button.getAttribute('data-name');
    const workerName = button.getAttribute('data-worker');
    
    if (!operationName) {
        showTemporaryMessage('Operation name not found', 'error');
        return;
    }
    
    // Confirm deletion
    const confirmMessage = workerName 
        ? `Are you sure you want to delete operation "${operationName}" from worker "${workerName}"?`
        : `Are you sure you want to delete operation "${operationName}"?`;
        
    if (!confirm(confirmMessage)) {
        return;
    }
    
    // Get the table row
    const row = button.closest('tr');
    
    // Show loading state on button
    const originalContent = button.innerHTML;
    button.innerHTML = '<i class="bi bi-hourglass-split"></i>';
    button.disabled = true;
    
    // Prepare the URL
    let deleteUrl = `/api/dispatched/operations/${encodeURIComponent(operationName)}`;
    if (workerName) {
        deleteUrl += `?worker=${encodeURIComponent(workerName)}`;
    }
    
    // Make delete request
    fetch(deleteUrl, {
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
            const remainingRows = document.querySelectorAll('#dispatchedTable tbody tr').length;
            const countElements = document.querySelectorAll('#dispatchedCount, .operation-count');
            countElements.forEach(element => {
                element.textContent = remainingRows + ' Operations';
            });
            
            // Re-index remaining rows
            const rows = document.querySelectorAll('#dispatchedTable tbody tr');
            rows.forEach((row, index) => {
                row.cells[0].textContent = index + 1;
            });
            
            // Update global data
            allOperations = allOperations.filter(op => {
                const matchesName = (op.operationName || op.name) === operationName;
                const matchesWorker = !workerName || op.workerName === workerName;
                return !(matchesName && matchesWorker);
            });
            
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