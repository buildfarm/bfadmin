/**
 * BuildFarm Queues JavaScript
 * Handles queue selection, operation loading, and table interactions
 */

// Queue operation data and sorting variables
let operationData = [];
let operationSortColumn = '';
let operationSortDirection = 'asc';
let selectedQueue = '';

// Initialize the page when DOM loads
document.addEventListener('DOMContentLoaded', function() {
    // Initialize queue functionality
    initializeQueues();
    
    // Load initial data if a queue is selected
    const initialQueue = document.querySelector('.queue-item.selected');
    if (initialQueue) {
        const queueName = initialQueue.getAttribute('data-queue');
        if (queueName) {
            selectQueue(queueName);
        }
    }
});

function initializeQueues() {
    // Set up queue selection handlers
    setupQueueHandlers();
    
    // Set up table sorting
    setupTableSorting();
    
    // Set up search functionality
    setupSearchHandlers();
    
    // Set up refresh functionality
    setupRefreshHandlers();
}

function setupQueueHandlers() {
    // Handle queue selection clicks
    const queueItems = document.querySelectorAll('.queue-item');
    queueItems.forEach(item => {
        item.addEventListener('click', function() {
            const queueName = this.getAttribute('data-queue');
            selectQueue(queueName);
        });
    });
}

function setupTableSorting() {
    // Add sorting functionality to table headers
    const sortableHeaders = document.querySelectorAll('.sortable-header');
    sortableHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const column = this.getAttribute('data-column');
            sortOperations(column);
        });
    });
}

function setupSearchHandlers() {
    // Handle operation search
    const searchInput = document.getElementById('operationSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            filterOperations(this.value);
        });
    }
}

function setupRefreshHandlers() {
    // Handle refresh button
    const refreshBtn = document.getElementById('refreshOperationsBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', function() {
            refreshOperations();
        });
    }
}

function selectQueue(queueName) {
    selectedQueue = queueName;
    
    // Update UI to show selected queue
    updateSelectedQueue(queueName);
    
    // Load operations for this queue
    loadQueueOperations(queueName);
}

function updateSelectedQueue(queueName) {
    // Remove previous selection
    const previousSelected = document.querySelector('.queue-item.selected');
    if (previousSelected) {
        previousSelected.classList.remove('selected');
    }
    
    // Add selection to current queue
    const currentQueue = document.querySelector(`[data-queue="${queueName}"]`);
    if (currentQueue) {
        currentQueue.classList.add('selected');
    }
    
    // Update queue name display
    const queueNameDisplay = document.getElementById('selectedQueueName');
    if (queueNameDisplay) {
        queueNameDisplay.textContent = queueName;
    }
}

function loadQueueOperations(queueName) {
    // Show loading state
    showLoadingState();
    
    // Make request to load operations
    const url = `/api/queues/${encodeURIComponent(queueName)}/operations`;
    
    fetch(url)
        .then(response => response.json())
        .then(data => {
            operationData = data || [];
            renderOperationTable();
            updateOperationCount();
        })
        .catch(error => {
            console.error('Error loading queue operations:', error);
            showErrorState('Failed to load queue operations');
        });
}

function showLoadingState() {
    const tableBody = document.querySelector('#operationTable tbody');
    if (tableBody) {
        tableBody.innerHTML = '<tr><td colspan="6" class="text-center py-4"><div class="spinner-border spinner-border-sm" role="status"></div> Loading operations...</td></tr>';
    }
}

function showErrorState(message) {
    const tableBody = document.querySelector('#operationTable tbody');
    if (tableBody) {
        tableBody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-4">${message}</td></tr>`;
    }
}

function renderOperationTable() {
    const tableBody = document.querySelector('#operationTable tbody');
    if (!tableBody) return;
    
    // Get current search term
    const searchInput = document.getElementById('operationSearchInput');
    const searchTerm = searchInput ? searchInput.value.toLowerCase() : '';
    
    // Filter operations based on search
    const filteredOperations = operationData.filter(operation => {
        if (!searchTerm) return true;
        
        return (operation.name && operation.name.toLowerCase().includes(searchTerm)) ||
               (operation.stage && operation.stage.toLowerCase().includes(searchTerm)) ||
               (operation.status && operation.status.toLowerCase().includes(searchTerm)) ||
               (operation.worker && operation.worker.toLowerCase().includes(searchTerm)) ||
               (operation.queuedAt && operation.queuedAt.toLowerCase().includes(searchTerm));
    });
    
    // Clear table
    tableBody.innerHTML = '';
    
    // Add filtered rows
    filteredOperations.forEach((operation, index) => {
        const row = createOperationRow(operation, index + 1);
        tableBody.appendChild(row);
    });
    
    // Show "no results" message if needed
    if (filteredOperations.length === 0) {
        const noResultsRow = document.createElement('tr');
        noResultsRow.innerHTML = '<td colspan="6" class="text-center text-muted py-4">No operations found</td>';
        tableBody.appendChild(noResultsRow);
    }
    
    // Update count display
    updateOperationCount(filteredOperations.length);
}

function createOperationRow(operation, index) {
    const row = document.createElement('tr');
    
    row.innerHTML = `
        <td>${index}</td>
        <td>
            <div class="operation-name">${escapeHtml(operation.name || 'N/A')}</div>
        </td>
        <td>
            <span class="badge ${getStageClass(operation.stage)}">${escapeHtml(operation.stage || 'Unknown')}</span>
        </td>
        <td>
            <span class="badge ${getStatusClass(operation.status)}">${escapeHtml(operation.status || 'Unknown')}</span>
        </td>
        <td>${escapeHtml(operation.worker || 'N/A')}</td>
        <td>${escapeHtml(operation.queuedAt || 'N/A')}</td>
        <td>
            <button class="btn btn-sm btn-outline-primary" onclick="showOperationDetails('${escapeHtml(operation.name || '')}')">
                <i class="fas fa-eye"></i>
            </button>
        </td>
    `;
    
    return row;
}

function getStageClass(stage) {
    const stageClasses = {
        'QUEUED': 'bg-warning',
        'EXECUTING': 'bg-info',
        'COMPLETED': 'bg-success',
        'ERROR': 'bg-danger'
    };
    return stageClasses[stage] || 'bg-secondary';
}

function getStatusClass(status) {
    const statusClasses = {
        'PENDING': 'bg-warning',
        'RUNNING': 'bg-info', 
        'SUCCESS': 'bg-success',
        'FAILURE': 'bg-danger',
        'CANCELLED': 'bg-secondary'
    };
    return statusClasses[status] || 'bg-secondary';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function sortOperations(column) {
    if (operationSortColumn === column) {
        operationSortDirection = operationSortDirection === 'asc' ? 'desc' : 'asc';
    } else {
        operationSortColumn = column;
        operationSortDirection = 'asc';
    }
    
    // Update sort indicators
    updateSortIndicators(column);
    
    // Sort the data
    operationData.sort((a, b) => {
        let aVal = String(a[column] || '').toLowerCase();
        let bVal = String(b[column] || '').toLowerCase();
        
        if (operationSortDirection === 'asc') {
            return aVal.localeCompare(bVal);
        } else {
            return bVal.localeCompare(aVal);
        }
    });
    
    // Re-render table
    renderOperationTable();
}

function updateSortIndicators(column) {
    // Remove all existing sort classes
    const headers = document.querySelectorAll('.sortable-header');
    headers.forEach(header => {
        header.classList.remove('sorted-asc', 'sorted-desc');
    });
    
    // Add sort class to current column
    const currentHeader = document.querySelector(`[data-column="${column}"]`);
    if (currentHeader) {
        currentHeader.classList.add(operationSortDirection === 'asc' ? 'sorted-asc' : 'sorted-desc');
    }
}

function filterOperations(searchTerm) {
    // Re-render table with current search term
    renderOperationTable();
}

function updateOperationCount(count = null) {
    const actualCount = count !== null ? count : operationData.length;
    const countElements = document.querySelectorAll('#operationCount, .operation-count');
    countElements.forEach(element => {
        element.textContent = actualCount;
    });
}

function refreshOperations() {
    if (selectedQueue) {
        loadQueueOperations(selectedQueue);
    }
}

function showOperationDetails(operationName) {
    // Find the operation in our data
    const operation = operationData.find(op => op.name === operationName);
    if (!operation) {
        alert('Operation not found');
        return;
    }
    
    // Show operation details in modal or alert
    const details = `Operation Details:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Name: ${operation.name || 'N/A'}
Stage: ${operation.stage || 'Unknown'}
Status: ${operation.status || 'Unknown'}
Worker: ${operation.worker || 'N/A'}
Queued At: ${operation.queuedAt || 'N/A'}

Additional Details:
${JSON.stringify(operation, null, 2)}`;

    alert(details);
}

// Handle search input with debouncing
function handleSearchInput(event) {
    clearTimeout(window.searchTimeout);
    window.searchTimeout = setTimeout(() => {
        filterOperations(event.target.value);
    }, 300);
}
