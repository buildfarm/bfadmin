/**
 * BuildFarm Queues JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {
    // Set up table sorting
    const sortableHeaders = document.querySelectorAll('.sortable-header');
    sortableHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const column = this.getAttribute('data-column');
            sortOperationTable(column);
        });
    });
});

    const tableBody = document.querySelector('#operationTable tbody');
    if (!tableBody) return;
    
    const rows = tableBody.querySelectorAll('tr');
    let visibleCount = 0;
    
    
    rows.forEach((row, index) => {
        const cells = row.querySelectorAll('td');
        
        const rowText = row.textContent.toLowerCase();
        
            row.style.display = '';
            // Update the index number
            const indexCell = cells[0];
            if (indexCell) {
                indexCell.textContent = visibleCount + 1;
            }
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });
    
    // Update operation count
    const countElements = document.querySelectorAll('#operationCount, .operation-count');
    countElements.forEach(element => {
        element.textContent = visibleCount;
    });
}

let sortDirection = {};

function sortOperationTable(column) {
    const tableBody = document.querySelector('#operationTable tbody');
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
            case 'queueName':
                aVal = a.cells[1].textContent.trim();
                bVal = b.cells[1].textContent.trim();
                break;
            case 'operationName':
                aVal = a.cells[2].textContent.trim();
                bVal = b.cells[2].textContent.trim();
                break;
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

// Delete operation from Redis
function deleteOperation(button) {
    const operationName = button.getAttribute('data-name');
    const queueName = button.getAttribute('data-queue');
    
    if (!operationName) {
        showTemporaryMessage('Operation name not found', 'error');
        return;
    }
    
    if (!queueName) {
        showTemporaryMessage('Queue name not found', 'error');
        return;
    }
    
    // Confirm deletion
    if (!confirm(`Are you sure you want to delete operation "${operationName}" from queue "${queueName}"?`)) {
        return;
    }
    
    // Get the table row
    const row = button.closest('tr');
    
    // Show loading state on button
    const originalContent = button.innerHTML;
    button.innerHTML = '<i class="bi bi-hourglass-split"></i>';
    button.disabled = true;
    
    console.log(`Attempting to delete operation: ${operationName} from queue: ${queueName}`);
    
    // Make delete request
    fetch(`/api/queues/${encodeURIComponent(queueName)}/operations/${encodeURIComponent(operationName)}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        console.log(`Delete response status: ${response.status}`);
        if (response.ok) {
            return response.json();
        } else {
            return response.text().then(text => {
                throw new Error(`HTTP ${response.status}: ${text || response.statusText}`);
            });
        }
    })
    .then(data => {
        console.log('Delete response data:', data);
        if (data.success) {
            // Remove the row from the table
            row.remove();
            
            // Update operation count
            const remainingRows = document.querySelectorAll('#operationTable tbody tr').length;
            const countElements = document.querySelectorAll('#operationCount, .operation-count');
            countElements.forEach(element => {
                element.textContent = remainingRows;
            });
            
            // Re-index remaining rows
            const rows = document.querySelectorAll('#operationTable tbody tr');
            rows.forEach((row, index) => {
                row.cells[0].textContent = index + 1;
            });
            
            // Show success message
            showTemporaryMessage('Operation deleted successfully', 'success');
        } else {
            throw new Error(data.message || 'Unknown error occurred');
        }
    })
    .catch(error => {
        console.error('Error deleting operation:', error);
        
        // Restore button state
        button.innerHTML = originalContent;
        button.disabled = false;
        
        // Show error message
        showTemporaryMessage(`Failed to delete operation: ${error.message}`, 'error');
    });
}

// Show temporary message to user
function showTemporaryMessage(message, type) {
    // Remove any existing messages
    const existingMessages = document.querySelectorAll('.temp-message');
    existingMessages.forEach(msg => msg.remove());
    
    // Create message element
    const messageDiv = document.createElement('div');
    messageDiv.className = `alert alert-${type === 'success' ? 'success' : 'danger'} alert-dismissible fade show position-fixed temp-message`;
    messageDiv.style.cssText = 'top: 20px; right: 20px; z-index: 1050; min-width: 300px;';
    messageDiv.innerHTML = `
        <i class="bi ${type === 'success' ? 'bi-check-circle' : 'bi-exclamation-triangle'}"></i>
        ${message}
        <button type="button" class="btn-close" onclick="this.parentElement.remove()"></button>
    `;
    
    // Add to page
    document.body.appendChild(messageDiv);
    
    // Auto-remove after 5 seconds
    setTimeout(() => {
        if (messageDiv.parentNode) {
            messageDiv.remove();
        }
    }, 5000);
}
