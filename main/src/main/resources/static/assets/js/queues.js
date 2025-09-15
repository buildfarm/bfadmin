/**
 * BuildFarm Queues JavaScript
 * Handles queue selection, operation loading, pagination, and table interactions
 */

// Queue operation data and pagination variables
let operationData = [];
let operationCurrentPage = 1;
let operationPageSize = 10;
let operationSortColumn = '';
let operationSortDirection = 'asc';

document.addEventListener('DOMContentLoaded', function() {
    // Initialize queue functionality
    initializeQueueFunctionality();
    
    // Add fade-in animations
    const animatedElements = document.querySelectorAll('.fade-in');
    animatedElements.forEach((el, index) => {
        el.style.animationDelay = `${index * 0.1}s`;
    });
});

function initializeQueueFunctionality() {
    // Extract initial operation data from the table
    extractOperationData();
    
    // Setup event listeners
    setupQueueEventListeners();
    
    // Initialize table pagination and sorting
    if (operationData.length > 0) {
        renderOperationTable();
    }
}

// Extract operation data from existing HTML table
function extractOperationData() {
    const table = document.getElementById('operationTable');
    if (!table) return;
    
    const tbody = table.querySelector('tbody');
    if (!tbody) return;
    
    operationData = [];
    const rows = tbody.querySelectorAll('tr');
    
    rows.forEach(row => {
        const cells = row.querySelectorAll('td');
        if (cells.length >= 6) {
            const operation = {
                index: cells[0].textContent.trim(),
                operationName: cells[1].querySelector('.fw-medium')?.textContent.trim() || '',
                operationId: cells[1].querySelector('small')?.textContent.replace('ID: ', '').trim() || '',
                stage: cells[2].querySelector('span')?.textContent.trim() || '',
                status: cells[3].querySelector('span')?.textContent.trim() || '',
                queuedAt: cells[4].textContent.trim(),
                hasMetadata: cells[5].querySelector('.status-success') !== null,
                rawData: cells[6].querySelector('button')?.dataset.raw || ''
            };
            operationData.push(operation);
        }
    });
    
    console.log('Extracted', operationData.length, 'operations');
}

// Set up event listeners for queue interactions
function setupQueueEventListeners() {
    // Search functionality
    const searchInput = document.getElementById('operationSearch');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            operationCurrentPage = 1;
            renderOperationTable();
        });
    }
    
    // Page size selector
    const pageSizeSelect = document.getElementById('operationPageSize');
    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', function() {
            operationPageSize = parseInt(this.value);
            operationCurrentPage = 1;
            renderOperationTable();
        });
    }
    
    // Setup sorting for operation table
    setupOperationSorting();
}

// Setup sorting listeners for operation table
function setupOperationSorting() {
    const table = document.getElementById('operationTable');
    if (!table) return;
    
    const headers = table.querySelectorAll('.sortable-header');
    headers.forEach(header => {
        header.addEventListener('click', function() {
            const column = this.dataset.column;
            handleOperationSort(column);
        });
    });
}

// Handle sorting for operations
function handleOperationSort(column) {
    if (operationSortColumn === column) {
        operationSortDirection = operationSortDirection === 'asc' ? 'desc' : 'asc';
    } else {
        operationSortColumn = column;
        operationSortDirection = 'asc';
    }
    
    updateOperationSortHeaders();
    renderOperationTable();
}

// Update sort header indicators
function updateOperationSortHeaders() {
    const table = document.getElementById('operationTable');
    if (!table) return;
    
    const headers = table.querySelectorAll('.sortable-header');
    headers.forEach(header => {
        header.classList.remove('sort-asc', 'sort-desc');
        if (header.dataset.column === operationSortColumn) {
            header.classList.add(`sort-${operationSortDirection}`);
        }
    });
}

// Render operation table with pagination and filtering
function renderOperationTable() {
    const table = document.getElementById('operationTable');
    if (!table) return;
    
    const tbody = table.querySelector('tbody');
    if (!tbody) return;
    
    // Get search term
    const searchInput = document.getElementById('operationSearch');
    const searchTerm = searchInput ? searchInput.value.toLowerCase() : '';
    
    // Filter operations based on search
    let filteredOperations = operationData.filter(operation => {
        if (!searchTerm) return true;
        return operation.name.toLowerCase().includes(searchTerm) ||
               operation.stage.toLowerCase().includes(searchTerm) ||
               operation.workerName.toLowerCase().includes(searchTerm) ||
               operation.platform.toLowerCase().includes(searchTerm);
    });
    
    // Sort operations
    if (operationSortColumn) {
        filteredOperations.sort((a, b) => {
            let valueA = a[operationSortColumn];
            let valueB = b[operationSortColumn];
            
        // Handle different data types
        if (operationSortColumn === 'name' || operationSortColumn === 'stage') {
            valueA = String(valueA).toLowerCase();
            valueB = String(valueB).toLowerCase();
        } else if (operationSortColumn === 'queuedTimestamp') {
            valueA = new Date(valueA);
            valueB = new Date(valueB);
        } else {
            valueA = String(valueA).toLowerCase();
            valueB = String(valueB).toLowerCase();
        }            if (valueA < valueB) {
                return operationSortDirection === 'asc' ? -1 : 1;
            } else if (valueA > valueB) {
                return operationSortDirection === 'asc' ? 1 : -1;
            }
            return 0;
        });
    }
    
    // Pagination
    const totalItems = filteredOperations.length;
    const totalPages = Math.ceil(totalItems / operationPageSize);
    const start = (operationCurrentPage - 1) * operationPageSize;
    const end = Math.min(start + operationPageSize, totalItems);
    const pageOperations = filteredOperations.slice(start, end);
    
    // Clear existing rows
    tbody.innerHTML = '';
    
    // Add filtered and paginated rows
    pageOperations.forEach((operation, index) => {
        const row = createOperationRow(operation, index);
        tbody.appendChild(row);
    });
    
    // Update pagination info
    updateOperationPaginationInfo(start + 1, end, totalItems, totalPages);
    updateOperationPaginationControls(totalPages);
    
    // Update operation count
    const countElement = document.getElementById('operationTableCount');
    if (countElement) {
        countElement.textContent = totalItems;
    }
}

// Create a table row for an operation
function createOperationRow(operation, index) {
    const row = document.createElement('tr');
    
    const stageIcon = operation.stage === 'QUEUED' ? 'bi-clock' : 
                     operation.stage === 'EXECUTING' ? 'bi-gear' : 
                     operation.stage === 'COMPLETED' ? 'bi-check-circle' : 
                     operation.stage === 'FAILED' ? 'bi-x-circle' : 'bi-question-circle';
    
    const stageBadgeClass = operation.stage === 'QUEUED' ? 'status-warning' :
                           operation.stage === 'EXECUTING' ? 'status-info' :
                           operation.stage === 'COMPLETED' ? 'status-success' : 
                           operation.stage === 'FAILED' ? 'status-danger' : 'status-secondary';
    
    const statusIcon = operation.stage === 'QUEUED' ? 'bi-clock' :
                      operation.stage === 'EXECUTING' ? 'bi-gear' :
                      operation.stage === 'COMPLETED' ? 'bi-check-circle' :
                      operation.stage === 'FAILED' ? 'bi-x-circle' : 'bi-question-circle';
    
    const statusBadgeClass = operation.stage === 'QUEUED' ? 'status-warning' :
                            operation.stage === 'EXECUTING' ? 'status-info' :
                            operation.stage === 'COMPLETED' ? 'status-success' :
                            operation.stage === 'FAILED' ? 'status-danger' : 'status-secondary';
    
    row.innerHTML = `
        <td><span class="text-muted">${index + 1}</span></td>
        <td>
            <span class="fw-medium">${operation.name}</span>
            ${operation.actionDigest ? `<br><small class="text-muted">Digest: ${operation.actionDigest.substring(0, 8)}...</small>` : ''}
        </td>
        <td>
            <span class="status-badge ${stageBadgeClass}">
                <i class="bi ${stageIcon}"></i>
                <span>${operation.stage}</span>
            </span>
        </td>
        <td>
            <span class="status-badge ${stageBadgeClass}">
                <i class="bi ${stageIcon}"></i>
                <span>${operation.stage}</span>
            </span>
        </td>
        <td><small class="text-muted">${operation.queuedTimestamp}</small></td>
        <td>
            <span class="status-badge ${operation.platform ? 'status-success' : 'status-secondary'}">
                <i class="bi ${operation.platform ? 'bi-check-circle' : 'bi-x-circle'}"></i>
                ${operation.platform ? 'Yes' : 'No'}
            </span>
        </td>
        <td>
            <button class="btn btn-sm btn-outline-info" onclick="showOperationDetails(this)" 
                    data-name="${operation.name}" 
                    data-stage="${operation.stage}" 
                    data-worker="${operation.workerName}" 
                    data-platform="${operation.platform}" 
                    data-queued="${operation.queuedTimestamp}" 
                    data-execute-timeout="${operation.executeTimeout}" 
                    data-action-timeout="${operation.actionTimeout}">
                <i class="bi bi-eye"></i> View
            </button>
        </td>
    `;
    
    return row;
}

// Update pagination info display
function updateOperationPaginationInfo(start, end, total, totalPages) {
    const startElement = document.getElementById('operationStart');
    const endElement = document.getElementById('operationEnd');
    const totalElement = document.getElementById('operationTotal');
    
    if (startElement) startElement.textContent = start;
    if (endElement) endElement.textContent = end;
    if (totalElement) totalElement.textContent = total;
    
    // Update pagination buttons
    const prevBtn = document.getElementById('operationPrevBtn');
    const nextBtn = document.getElementById('operationNextBtn');
    
    if (prevBtn) {
        prevBtn.disabled = operationCurrentPage <= 1;
    }
    if (nextBtn) {
        nextBtn.disabled = operationCurrentPage >= totalPages;
    }
}

// Update pagination controls
function updateOperationPaginationControls(totalPages) {
    const pageNumbersSpan = document.getElementById('operationPageNumbers');
    if (!pageNumbersSpan) return;
    
    pageNumbersSpan.innerHTML = '';
    
    if (totalPages <= 1) return;
    
    const maxVisiblePages = 5;
    let startPage = Math.max(1, operationCurrentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);
    
    if (endPage - startPage + 1 < maxVisiblePages) {
        startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.className = `page-btn ${i === operationCurrentPage ? 'active' : ''}`;
        pageBtn.textContent = i;
        pageBtn.onclick = function() {
            operationCurrentPage = i;
            renderOperationTable();
        };
        pageNumbersSpan.appendChild(pageBtn);
    }
}

// Load operations for selected queue
function loadQueueOperations() {
    const queueSelect = document.getElementById('queueSelect');
    if (!queueSelect) return;
    
    const selectedQueue = queueSelect.value;
    if (!selectedQueue) return;
    
    console.log('Loading operations for queue:', selectedQueue);
    
    // Update current queue name display
    const currentQueueName = document.getElementById('currentQueueName');
    if (currentQueueName) {
        currentQueueName.textContent = selectedQueue;
    }
    
    // Show loading state
    showLoadingState();
    
    // Make AJAX request to get operations
    fetch(`/api/queue-operations?queueName=${encodeURIComponent(selectedQueue)}`)
        .then(response => response.json())
        .then(operations => {
            console.log('Received', operations.length, 'operations');
            
            // Update operation data
            operationData = operations.map(op => ({
                index: op.index || 0,
                operationName: op.operationName || 'Unknown',
                operationId: op.operationId || '',
                stage: op.stage || 'UNKNOWN',
                status: op.status || 'Unknown',
                queuedAt: op.queuedAt || 'N/A',
                hasMetadata: op.hasMetadata || false,
                rawData: op.rawData || ''
            }));
            
            // Reset pagination
            operationCurrentPage = 1;
            
            // Update table
            renderOperationTable();
            
            // Update operation count badge
            const operationCount = document.getElementById('operationCount');
            if (operationCount) {
                operationCount.textContent = operations.length + ' Operations';
            }
            
            hideLoadingState();
        })
        .catch(error => {
            console.error('Error loading queue operations:', error);
            hideLoadingState();
            showErrorMessage('Failed to load queue operations');
        });
}

// Show loading state
function showLoadingState() {
    const table = document.getElementById('operationTable');
    if (table) {
        const tbody = table.querySelector('tbody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center p-4"><i class="bi bi-hourglass-split"></i> Loading operations...</td></tr>';
        }
    }
}

// Hide loading state
function hideLoadingState() {
    // Loading state will be replaced by renderOperationTable()
}

// Show error message
function showErrorMessage(message) {
    const table = document.getElementById('operationTable');
    if (table) {
        const tbody = table.querySelector('tbody');
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center p-4 text-danger"><i class="bi bi-exclamation-triangle"></i> ${message}</td></tr>`;
        }
    }
}

// Refresh operations for current queue
function refreshOperations() {
    loadQueueOperations();
}

// Change operation page
function changeOperationPage(direction) {
    const totalItems = operationData.length;
    const totalPages = Math.ceil(totalItems / operationPageSize);
    
    operationCurrentPage += direction;
    if (operationCurrentPage < 1) operationCurrentPage = 1;
    if (operationCurrentPage > totalPages) operationCurrentPage = totalPages;
    
    renderOperationTable();
}

// Show operation details in modal
function showOperationDetails(button) {
    // Extract operation details from data attributes
    const operationData = {
        name: button.dataset.name,
        stage: button.dataset.stage,
        worker: button.dataset.worker,
        platform: button.dataset.platform,
        queued: button.dataset.queued,
        executeTimeout: button.dataset.executeTimeout,
        actionTimeout: button.dataset.actionTimeout
    };
    
    // Update modal content
    const modalTitle = document.getElementById('operationModalLabel');
    const modalBody = document.getElementById('operationDetails');
    
    if (modalTitle) {
        modalTitle.textContent = `Operation: ${operationData.name || 'Unknown'}`;
    }
    
    if (modalBody) {
        // Format the operation data for better readability
        const formatted = JSON.stringify(operationData, null, 2);
        modalBody.textContent = formatted;
    }
    
    // Show modal using Bootstrap 5 API
    const modal = new bootstrap.Modal(document.getElementById('operationModal'));
    modal.show();
}
