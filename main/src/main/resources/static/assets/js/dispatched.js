/**
 * Dispatched Operations JavaScript
 * Handles dispatched operations table functionality, search, sorting, and pagination
 */

let currentDispatchedPage = 1;
let dispatchedPageSize = 10;
let dispatchedSortColumn = 'index';
let dispatchedSortDirection = 'asc';
let dispatchedSearchQuery = '';
let allDispatchedOperations = [];
let filteredDispatchedOperations = [];

// Initialize dispatched operations functionality when page loads
document.addEventListener('DOMContentLoaded', function() {
    console.log('=== DISPATCHED OPERATIONS JS INITIALIZED ===');
    
    const dispatchedTable = document.getElementById('dispatchedTable');
    if (dispatchedTable) {
        console.log('Dispatched operations table found, initializing...');
        initializeDispatchedOperations();
    } else {
        console.log('No dispatched operations table found on this page');
    }
});

function initializeDispatchedOperations() {
    console.log('Initializing dispatched operations table functionality');
    
    // Get initial operations data from the table
    extractDispatchedOperationsFromTable();
    
    // Initialize search functionality
    const searchInput = document.getElementById('dispatchedSearch');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            dispatchedSearchQuery = this.value.toLowerCase();
            filterDispatchedOperations();
        });
    }
    
    // Initialize page size selector
    const pageSizeSelector = document.getElementById('dispatchedPageSize');
    if (pageSizeSelector) {
        pageSizeSelector.addEventListener('change', function() {
            dispatchedPageSize = parseInt(this.value);
            currentDispatchedPage = 1;
            updateDispatchedTable();
        });
    }
    
    // Initialize sortable headers
    const sortableHeaders = document.querySelectorAll('#dispatchedTable .sortable-header');
    sortableHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const column = this.getAttribute('data-column');
            if (dispatchedSortColumn === column) {
                dispatchedSortDirection = dispatchedSortDirection === 'asc' ? 'desc' : 'asc';
            } else {
                dispatchedSortColumn = column;
                dispatchedSortDirection = 'asc';
            }
            sortDispatchedOperations();
        });
    });
    
    // Initial filtering and display
    filterDispatchedOperations();
    
    console.log(`Dispatched operations initialized with ${allDispatchedOperations.length} total operations`);
}

function extractDispatchedOperationsFromTable() {
    console.log('Extracting dispatched operations from table...');
    
    const tableBody = document.querySelector('#dispatchedTable tbody');
    if (!tableBody) {
        console.warn('No dispatched table body found');
        return;
    }
    
    const rows = tableBody.querySelectorAll('tr');
    allDispatchedOperations = [];
    
    rows.forEach((row, index) => {
        const cells = row.querySelectorAll('td');
        if (cells.length >= 7) {
            const operation = {
                index: index + 1,
                operationId: cells[1].textContent.trim(),
                operationName: cells[2].querySelector('.fw-medium')?.textContent.trim() || '',
                actionDigest: cells[2].querySelector('small')?.textContent.replace('Digest: ', '').trim() || '',
                stage: cells[3].textContent.trim(),
                status: cells[4].textContent.trim(),
                dispatchedAt: cells[5].textContent.trim(),
                hasMetadata: cells[6].textContent.trim(),
                element: row
            };
            
            allDispatchedOperations.push(operation);
        }
    });
    
    console.log(`Extracted ${allDispatchedOperations.length} dispatched operations from table`);
    console.log('Sample dispatched operation:', allDispatchedOperations[0]);
}

function filterDispatchedOperations() {
    console.log(`Filtering dispatched operations with query: "${dispatchedSearchQuery}"`);
    
    if (!dispatchedSearchQuery) {
        filteredDispatchedOperations = [...allDispatchedOperations];
    } else {
        filteredDispatchedOperations = allDispatchedOperations.filter(operation => {
            return operation.operationId.toLowerCase().includes(dispatchedSearchQuery) ||
                   operation.operationName.toLowerCase().includes(dispatchedSearchQuery) ||
                   operation.stage.toLowerCase().includes(dispatchedSearchQuery) ||
                   operation.status.toLowerCase().includes(dispatchedSearchQuery) ||
                   operation.actionDigest.toLowerCase().includes(dispatchedSearchQuery);
        });
    }
    
    console.log(`Filtered to ${filteredDispatchedOperations.length} dispatched operations`);
    currentDispatchedPage = 1;
    updateDispatchedTable();
}

function sortDispatchedOperations() {
    console.log(`Sorting dispatched operations by ${dispatchedSortColumn} ${dispatchedSortDirection}`);
    
    filteredDispatchedOperations.sort((a, b) => {
        let valueA = a[dispatchedSortColumn];
        let valueB = b[dispatchedSortColumn];
        
        // Convert to appropriate types for comparison
        if (dispatchedSortColumn === 'index') {
            valueA = parseInt(valueA);
            valueB = parseInt(valueB);
        } else if (dispatchedSortColumn === 'dispatchedAt') {
            valueA = new Date(valueA).getTime() || 0;
            valueB = new Date(valueB).getTime() || 0;
        } else {
            valueA = String(valueA).toLowerCase();
            valueB = String(valueB).toLowerCase();
        }
        
        let comparison = 0;
        if (valueA < valueB) comparison = -1;
        if (valueA > valueB) comparison = 1;
        
        return dispatchedSortDirection === 'desc' ? -comparison : comparison;
    });
    
    updateDispatchedTable();
}

function updateDispatchedTable() {
    console.log(`Updating dispatched table - Page ${currentDispatchedPage}, Size ${dispatchedPageSize}`);
    
    const tableBody = document.querySelector('#dispatchedTable tbody');
    if (!tableBody) return;
    
    // Hide all rows first
    allDispatchedOperations.forEach(operation => {
        if (operation.element) {
            operation.element.style.display = 'none';
        }
    });
    
    // Calculate pagination
    const totalOperations = filteredDispatchedOperations.length;
    const totalPages = Math.ceil(totalOperations / dispatchedPageSize);
    const startIndex = (currentDispatchedPage - 1) * dispatchedPageSize;
    const endIndex = Math.min(startIndex + dispatchedPageSize, totalOperations);
    
    // Show operations for current page
    const operationsToShow = filteredDispatchedOperations.slice(startIndex, endIndex);
    operationsToShow.forEach((operation, displayIndex) => {
        if (operation.element) {
            operation.element.style.display = '';
            // Update the index cell to show current page index
            const indexCell = operation.element.querySelector('td:first-child span');
            if (indexCell) {
                indexCell.textContent = startIndex + displayIndex + 1;
            }
        }
    });
    
    // Update table info
    updateDispatchedTableInfo(totalOperations, startIndex + 1, endIndex);
    updateDispatchedPaginationControls(totalPages);
    
    console.log(`Displaying ${operationsToShow.length} operations (${startIndex + 1}-${endIndex} of ${totalOperations})`);
}

function updateDispatchedTableInfo(total, start, end) {
    const countElement = document.getElementById('dispatchedTableCount');
    const startElement = document.getElementById('dispatchedStart');
    const endElement = document.getElementById('dispatchedEnd');
    const totalElement = document.getElementById('dispatchedTotal');
    
    if (countElement) countElement.textContent = total;
    if (startElement) startElement.textContent = total > 0 ? start : 0;
    if (endElement) endElement.textContent = end;
    if (totalElement) totalElement.textContent = total;
}

function updateDispatchedPaginationControls(totalPages) {
    const prevBtn = document.getElementById('dispatchedPrevBtn');
    const nextBtn = document.getElementById('dispatchedNextBtn');
    const pageNumbers = document.getElementById('dispatchedPageNumbers');
    
    // Update button states
    if (prevBtn) {
        prevBtn.disabled = currentDispatchedPage <= 1;
    }
    if (nextBtn) {
        nextBtn.disabled = currentDispatchedPage >= totalPages;
    }
    
    // Update page numbers
    if (pageNumbers) {
        let html = '';
        for (let i = 1; i <= totalPages; i++) {
            if (totalPages <= 10 || i <= 3 || i >= totalPages - 2 || Math.abs(i - currentDispatchedPage) <= 1) {
                html += `<button class="pagination-btn ${i === currentDispatchedPage ? 'active' : ''}" onclick="goToDispatchedPage(${i})">${i}</button>`;
            } else if (i === 4 && currentDispatchedPage > 6) {
                html += '<span class="pagination-ellipsis">...</span>';
            } else if (i === totalPages - 3 && currentDispatchedPage < totalPages - 5) {
                html += '<span class="pagination-ellipsis">...</span>';
            }
        }
        pageNumbers.innerHTML = html;
    }
}

function changeDispatchedPage(direction) {
    const totalPages = Math.ceil(filteredDispatchedOperations.length / dispatchedPageSize);
    
    if (direction > 0 && currentDispatchedPage < totalPages) {
        currentDispatchedPage++;
        updateDispatchedTable();
    } else if (direction < 0 && currentDispatchedPage > 1) {
        currentDispatchedPage--;
        updateDispatchedTable();
    }
    
    console.log(`Changed to dispatched page ${currentDispatchedPage}`);
}

function goToDispatchedPage(page) {
    const totalPages = Math.ceil(filteredDispatchedOperations.length / dispatchedPageSize);
    
    if (page >= 1 && page <= totalPages) {
        currentDispatchedPage = page;
        updateDispatchedTable();
        console.log(`Went to dispatched page ${page}`);
    }
}

// Utility function to refresh the dispatched operations page
function refreshDispatchedOperations() {
    console.log('Refreshing dispatched operations...');
    fetch('/api/dispatched-operations')
        .then(response => response.json())
        .then(data => {
            console.log('AJAX DISPATCHED OPERATIONS RESPONSE:', JSON.stringify(data, null, 2));
            console.log('Received', data.length, 'dispatched operations via AJAX');
            
            // You could update the table dynamically here
            // For now, just reload the page
            window.location.reload();
        })
        .catch(error => {
            console.error('Error fetching dispatched operations:', error);
            window.location.reload();
        });
}

console.log('Dispatched operations JavaScript loaded');
