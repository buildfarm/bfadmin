/**
 * BuildFarm Prequeue Operations JavaScript
 * Handles prequeued operation loading and table interactions
 */

// Prequeue operation data and sorting variables
let prequeueOperations = [];
let prequeueSortColumn = 'operationName';
let prequeueSortDirection = 'asc';

document.addEventListener('DOMContentLoaded', function() {
    // Initialize the prequeue operations table
    initializePrequeueTable();
});

function initializePrequeueTable() {
    // Extract data from server-rendered table
    extractPrequeueOperations();
    
    // Set up event listeners
    setupPrequeueEventListeners();
}

function extractPrequeueOperations() {
    const tableBody = document.querySelector('#prequeueTable tbody');
    if (!tableBody) return;
    
    const rows = tableBody.querySelectorAll('tr');
    prequeueOperations = [];
    
    rows.forEach((row, index) => {
        const cells = row.querySelectorAll('td');
        if (cells.length >= 5) {
            prequeueOperations.push({
                operationName: cells[1].textContent.trim(),
                stage: cells[2].textContent.trim(),
                status: cells[3].textContent.trim(),
                queuedAt: cells[4].textContent.trim(),
                row: row.cloneNode(true)
            });
        }
    });
    
    console.log('Loaded prequeue operations:', prequeueOperations.length);
}

function setupPrequeueEventListeners() {
    // Search functionality
    const searchInput = document.getElementById('operationSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            filterPrequeueOperations(this.value);
        });
    }
    
    // Sorting functionality
    const headers = document.querySelectorAll('#prequeueTable .sortable-header');
    headers.forEach(header => {
        header.addEventListener('click', function() {
            const column = this.getAttribute('data-column');
            sortPrequeueOperations(column);
        });
    });
}

function filterPrequeueOperations(searchTerm) {
    const tableBody = document.querySelector('#prequeueTable tbody');
    if (!tableBody) return;
    
    searchTerm = searchTerm.toLowerCase();
    let visibleCount = 0;
    
    tableBody.innerHTML = '';
    
    prequeueOperations.forEach((operation, index) => {
        const matchesSearch = !searchTerm || 
            operation.operationName.toLowerCase().includes(searchTerm) ||
            operation.stage.toLowerCase().includes(searchTerm) ||
            operation.status.toLowerCase().includes(searchTerm) ||
            operation.queuedAt.toLowerCase().includes(searchTerm);
        
        if (matchesSearch) {
            const row = operation.row.cloneNode(true);
            // Update the index
            row.querySelector('td:first-child span').textContent = visibleCount + 1;
            tableBody.appendChild(row);
            visibleCount++;
        }
    });
    
    // Update count displays
    updatePrequeueCount(visibleCount);
    
    // Show "no results" message if needed
    if (visibleCount === 0) {
        const noResultsRow = document.createElement('tr');
        noResultsRow.innerHTML = '<td colspan="5" class="text-center text-muted py-4">No prequeue operations found</td>';
        tableBody.appendChild(noResultsRow);
    }
}

function sortPrequeueOperations(column) {
    if (prequeueSortColumn === column) {
        prequeueSortDirection = prequeueSortDirection === 'asc' ? 'desc' : 'asc';
    } else {
        prequeueSortColumn = column;
        prequeueSortDirection = 'asc';
    }
    
    // Update sort headers
    const headers = document.querySelectorAll('#prequeueTable .sortable-header');
    headers.forEach(header => {
        header.classList.remove('sorted-asc', 'sorted-desc');
        if (header.getAttribute('data-column') === column) {
            header.classList.add(prequeueSortDirection === 'asc' ? 'sorted-asc' : 'sorted-desc');
        }
    });
    
    // Sort the data
    prequeueOperations.sort((a, b) => {
        let aVal = String(a[column]).toLowerCase();
        let bVal = String(b[column]).toLowerCase();
        if (prequeueSortDirection === 'asc') {
            return aVal.localeCompare(bVal);
        } else {
            return bVal.localeCompare(aVal);
        }
    });
    
    // Re-render the table
    const searchInput = document.getElementById('operationSearchInput');
    const searchTerm = searchInput ? searchInput.value : '';
    filterPrequeueOperations(searchTerm);
}

function updatePrequeueCount(count) {
    // Update operation count displays
    const countElements = document.querySelectorAll('#operationCount, .operation-count');
    countElements.forEach(element => {
        element.textContent = count;
    });
}

// Handle search input with debouncing
function handleSearchInput(event) {
    clearTimeout(window.searchTimeout);
    window.searchTimeout = setTimeout(() => {
        filterPrequeueOperations(event.target.value);
    }, 300);
}

// Show operation details
function showOperationDetails(button) {
    const operationName = button.getAttribute('data-name') || 'Unknown';
    const stage = button.getAttribute('data-stage') || 'Unknown';
    const status = button.getAttribute('data-status') || 'Unknown';
    const queuedAt = button.getAttribute('data-queued') || 'N/A';
    const rawData = button.getAttribute('data-raw') || 'No raw data available';

    const details = `Operation Details:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Name: ${operationName}
Stage: ${stage}
Status: ${status}
Queued At: ${queuedAt}

Raw Data:
${rawData}`;

    document.getElementById('operationDetails').textContent = details;
    
    // Show the modal
    const modal = new bootstrap.Modal(document.getElementById('operationModal'));
    modal.show();
}

// Refresh prequeue operations
function refreshPrequeueOperations() {
    window.location.reload();
}
