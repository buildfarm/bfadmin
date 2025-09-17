/**
 * BuildFarm Dispatched Operations JavaScript
 * Handles dispatched operations table functionality, search, and sorting
 */

// Dispatched operation data and sorting variables
let dispatchedOperations = [];
let dispatchedSortColumn = 'operationName';
let dispatchedSortDirection = 'asc';

document.addEventListener('DOMContentLoaded', function() {
    // Initialize the dispatched operations table
    initializeDispatchedTable();
});

function initializeDispatchedTable() {
    // Extract data from server-rendered table
    extractDispatchedOperations();
    
    // Set up event listeners
    setupDispatchedEventListeners();
}

function extractDispatchedOperations() {
    const tableBody = document.querySelector('#dispatchedTable tbody');
    if (!tableBody) return;
    
    const rows = tableBody.querySelectorAll('tr');
    dispatchedOperations = [];
    
    rows.forEach((row, index) => {
        const cells = row.querySelectorAll('td');
        if (cells.length >= 4) {
            dispatchedOperations.push({
                operationName: cells[1].textContent.trim(),
                stage: cells[2].textContent.trim(),
                executedAt: cells[3].textContent.trim(),
                row: row.cloneNode(true)
            });
        }
    });
    
    console.log('Loaded dispatched operations:', dispatchedOperations.length);
}

function setupDispatchedEventListeners() {
    // Search functionality
    const searchInput = document.getElementById('dispatchedSearch');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            filterDispatchedOperations(this.value);
        });
    }
    
    // Sorting functionality
    const headers = document.querySelectorAll('#dispatchedTable .sortable-header');
    headers.forEach(header => {
        header.addEventListener('click', function() {
            const column = this.getAttribute('data-column');
            sortDispatchedOperations(column);
        });
    });
}

function filterDispatchedOperations(searchTerm) {
    const tableBody = document.querySelector('#dispatchedTable tbody');
    if (!tableBody) return;
    
    searchTerm = searchTerm.toLowerCase();
    let visibleCount = 0;
    
    tableBody.innerHTML = '';
    
    dispatchedOperations.forEach((operation, index) => {
        const matchesSearch = !searchTerm || 
            operation.operationName.toLowerCase().includes(searchTerm) ||
            operation.stage.toLowerCase().includes(searchTerm) ||
            operation.executedAt.toLowerCase().includes(searchTerm);
        
        if (matchesSearch) {
            const row = operation.row.cloneNode(true);
            // Update the index
            row.querySelector('td:first-child span').textContent = visibleCount + 1;
            tableBody.appendChild(row);
            visibleCount++;
        }
    });
    
    // Update count
    const countElement = document.getElementById('dispatchedTableCount');
    if (countElement) {
        countElement.textContent = visibleCount;
    }
    
    // Show "no results" message if needed
    if (visibleCount === 0) {
        const noResultsRow = document.createElement('tr');
        noResultsRow.innerHTML = '<td colspan="4" class="text-center text-muted py-4">No dispatched operations found</td>';
        tableBody.appendChild(noResultsRow);
    }
}

function sortDispatchedOperations(column) {
    if (dispatchedSortColumn === column) {
        dispatchedSortDirection = dispatchedSortDirection === 'asc' ? 'desc' : 'asc';
    } else {
        dispatchedSortColumn = column;
        dispatchedSortDirection = 'asc';
    }
    
    // Update sort headers
    const headers = document.querySelectorAll('#dispatchedTable .sortable-header');
    headers.forEach(header => {
        header.classList.remove('sorted-asc', 'sorted-desc');
        if (header.getAttribute('data-column') === column) {
            header.classList.add(dispatchedSortDirection === 'asc' ? 'sorted-asc' : 'sorted-desc');
        }
    });
    
    // Sort the data
    dispatchedOperations.sort((a, b) => {
        let aVal = String(a[column]).toLowerCase();
        let bVal = String(b[column]).toLowerCase();
        if (dispatchedSortDirection === 'asc') {
            return aVal.localeCompare(bVal);
        } else {
            return bVal.localeCompare(aVal);
        }
    });
    
    // Re-render the table
    const searchInput = document.getElementById('dispatchedSearch');
    const searchTerm = searchInput ? searchInput.value : '';
    filterDispatchedOperations(searchTerm);
}

// Show operation details
function showOperationDetails(button) {
    const operationName = button.getAttribute('data-name') || 'Unknown';
    const stage = button.getAttribute('data-stage') || 'Unknown';
    const executedAt = button.getAttribute('data-executed') || 'N/A';
    const rawData = button.getAttribute('data-raw') || 'No raw data available';

    const details = `Operation Details:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Name: ${operationName}
Stage: ${stage}
Executed At: ${executedAt}

Raw Data:
${rawData}`;

    document.getElementById('operationDetails').textContent = details;
    
    // Show the modal
    const modal = new bootstrap.Modal(document.getElementById('operationModal'));
    modal.show();
}

// Refresh dispatched operations
function refreshDispatchedOperations() {
    window.location.reload();
}