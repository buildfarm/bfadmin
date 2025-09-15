/**
 * BuildFarm Dashboard JavaScript
 * Handles table pagination, sorting, search, and UI interactions
 */

document.addEventListener('DOMContentLoaded', function() {
    // Add fade-in animation to elements
    const animatedElements = document.querySelectorAll('.fade-in');
    animatedElements.forEach((el, index) => {
        el.style.animationDelay = `${index * 0.1}s`;
    });

    // Add pulse animation to status badges
    const statusBadges = document.querySelectorAll('.status-success');
    statusBadges.forEach(badge => {
        if (badge.textContent.includes('Connected') || badge.textContent.includes('Active')) {
            badge.classList.add('pulse');
        }
    });

    // Initialize tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Add hover effects to nav links
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('mouseenter', function() {
            this.style.transform = 'translateX(5px)';
        });
        link.addEventListener('mouseleave', function() {
            this.style.transform = 'translateX(0)';
        });
    });

    // Update timestamps
    updateRelativeTime();
    setInterval(updateRelativeTime, 60000); // Update every minute

    // Initialize table functionality
    initializeTableFunctionality();
});

// Format relative time
function updateRelativeTime() {
    const timeElements = document.querySelectorAll('[data-time]');
    timeElements.forEach(el => {
        const timestamp = el.getAttribute('data-time');
        if (timestamp) {
            el.textContent = formatRelativeTime(new Date(timestamp));
        }
    });
}

function formatRelativeTime(date) {
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) return `${days}d ago`;
    if (hours > 0) return `${hours}h ago`;
    if (minutes > 0) return `${minutes}m ago`;
    return 'Just now';
}

// Add smooth scrolling
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth'
            });
        }
    });
});

// Table Management Variables
let executeWorkerData = [];
let storageWorkerData = [];
let serversData = [];
let executeWorkerCurrentPage = 1;
let storageWorkerCurrentPage = 1;
let serversCurrentPage = 1;
let executeWorkerPageSize = 10;
let storageWorkerPageSize = 10;
let serversPageSize = 25;

// Initialize window variables for pagination
window.serversPageSize = 25;

let executeWorkerSortColumn = '';
let executeWorkerSortDirection = 'asc';
let storageWorkerSortColumn = '';
let storageWorkerSortDirection = 'asc';
let serversSortColumn = '';
let serversSortDirection = 'asc';

// Initialize table functionality
function initializeTableFunctionality() {
    // Extract data from existing tables
    extractTableData();
    
    // Set up event listeners
    setupEventListeners();
    
    // Initial render
    renderExecuteWorkerTable();
    renderStorageWorkerTable();
    renderServersTable();
}

// Extract worker data from existing HTML tables
function extractTableData() {
    const executeTable = document.getElementById('executeWorkerTable');
    const storageTable = document.getElementById('storageWorkerTable');
    const serversTable = document.getElementById('serversTable');

    if (executeTable) {
        executeWorkerData = extractWorkerDataFromTable(executeTable);
    }

    if (storageTable) {
        storageWorkerData = extractWorkerDataFromTable(storageTable);
    }

    if (serversTable) {
        serversData = extractServerDataFromTable(serversTable);
    }
}

function extractWorkerDataFromTable(table) {
    const rows = table.querySelectorAll('tbody tr');
    const data = [];

    rows.forEach(row => {
        const cells = row.querySelectorAll('td');
        if (cells.length >= 6) {
            // Remove content in parentheses from status
            const cleanStatus = cells[3].textContent.trim().replace(/\([^)]*\)/g, '').trim();
            data.push({
                groupName: cells[0].textContent.trim(),
                endpoint: cells[1].textContent.trim(),
                workerType: cells[2].textContent.trim(),
                status: cleanStatus,
                expireAt: cells[4].textContent.trim(),
                firstRegisteredAt: cells[5].textContent.trim(),
                originalHtml: row.innerHTML
            });
        }
    });

    return data;
}

function extractServerDataFromTable(table) {
    const rows = table.querySelectorAll('tbody tr');
    const data = [];

    rows.forEach((row, index) => {
        const cells = row.querySelectorAll('td');
        
        if (cells.length >= 6) {
            // Remove content in parentheses from status
            const cleanStatus = cells[3].textContent.trim().replace(/\([^)]*\)/g, '').trim();
            const serverData = {
                groupName: cells[0].textContent.trim(),
                endpoint: cells[1].textContent.trim(),
                serverType: cells[2].textContent.trim(),
                status: cleanStatus,
                expireAt: cells[4].textContent.trim(),
                firstRegisteredAt: cells[5].textContent.trim(),
                originalHtml: row.innerHTML
            };
            data.push(serverData);
        }
    });

    return data;
}

// Set up event listeners
function setupEventListeners() {
    // Search functionality
    const executeSearch = document.getElementById('executeWorkerSearch');
    const storageSearch = document.getElementById('storageWorkerSearch');

    if (executeSearch) {
        executeSearch.addEventListener('input', function() {
            executeWorkerCurrentPage = 1;
            renderExecuteWorkerTable();
        });
    }

    if (storageSearch) {
        storageSearch.addEventListener('input', function() {
            storageWorkerCurrentPage = 1;
            renderStorageWorkerTable();
        });
    }

    // Page size selectors
    const executePageSize = document.getElementById('executeWorkerPageSize');
    const storagePageSize = document.getElementById('storageWorkerPageSize');

    if (executePageSize) {
        executePageSize.addEventListener('change', function() {
            executeWorkerPageSize = parseInt(this.value);
            executeWorkerCurrentPage = 1;
            renderExecuteWorkerTable();
        });
    }

    if (storagePageSize) {
        storagePageSize.addEventListener('change', function() {
            storageWorkerPageSize = parseInt(this.value);
            storageWorkerCurrentPage = 1;
            renderStorageWorkerTable();
        });
    }

    // Servers event listeners
    const serversSearch = document.getElementById('serversSearch');
    const serversPageSize = document.getElementById('serversPageSize');

    if (serversSearch) {
        serversSearch.addEventListener('input', function() {
            serversCurrentPage = 1;
            renderServersTable();
        });
    }

    if (serversPageSize) {
        serversPageSize.addEventListener('change', function() {
            window.serversPageSize = parseInt(this.value);
            serversCurrentPage = 1;
            renderServersTable();
        });
    }

    // Sorting functionality
    setupSortingListeners('executeWorkerTable', 'execute');
    setupSortingListeners('storageWorkerTable', 'storage');
    setupSortingListeners('serversTable', 'servers');
}

function setupSortingListeners(tableId, type) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const headers = table.querySelectorAll('.sortable');
    headers.forEach(header => {
        header.addEventListener('click', function() {
            const column = this.getAttribute('data-column');
            handleSort(column, type);
        });
    });
}

function handleSort(column, type) {
    if (type === 'execute') {
        if (executeWorkerSortColumn === column) {
            executeWorkerSortDirection = executeWorkerSortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            executeWorkerSortColumn = column;
            executeWorkerSortDirection = 'asc';
        }
        updateSortHeaders('executeWorkerTable', column, executeWorkerSortDirection);
        renderExecuteWorkerTable();
    } else if (type === 'storage') {
        if (storageWorkerSortColumn === column) {
            storageWorkerSortDirection = storageWorkerSortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            storageWorkerSortColumn = column;
            storageWorkerSortDirection = 'asc';
        }
        updateSortHeaders('storageWorkerTable', column, storageWorkerSortDirection);
        renderStorageWorkerTable();
    } else if (type === 'servers') {
        if (serversSortColumn === column) {
            serversSortDirection = serversSortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            serversSortColumn = column;
            serversSortDirection = 'asc';
        }
        updateSortHeaders('serversTable', column, serversSortDirection);
        renderServersTable();
    }
}

function updateSortHeaders(tableId, activeColumn, direction) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const headers = table.querySelectorAll('.sortable');
    headers.forEach(header => {
        header.classList.remove('sorted-asc', 'sorted-desc');
        if (header.getAttribute('data-column') === activeColumn) {
            header.classList.add(direction === 'asc' ? 'sorted-asc' : 'sorted-desc');
        }
    });
}

// Render Execute Worker Table
function renderExecuteWorkerTable() {
    const searchTerm = document.getElementById('executeWorkerSearch')?.value.toLowerCase() || '';
    let filteredData = executeWorkerData.filter(worker => 
        worker.groupName.toLowerCase().includes(searchTerm) ||
        worker.endpoint.toLowerCase().includes(searchTerm) ||
        worker.status.toLowerCase().includes(searchTerm)
    );

    // Apply sorting
    if (executeWorkerSortColumn) {
        filteredData.sort((a, b) => {
            let aVal = a[executeWorkerSortColumn].toLowerCase();
            let bVal = b[executeWorkerSortColumn].toLowerCase();
            if (executeWorkerSortDirection === 'asc') {
                return aVal.localeCompare(bVal);
            } else {
                return bVal.localeCompare(aVal);
            }
        });
    }

    // Apply pagination
    const totalItems = filteredData.length;
    const totalPages = Math.ceil(totalItems / executeWorkerPageSize);
    const startIndex = (executeWorkerCurrentPage - 1) * executeWorkerPageSize;
    const endIndex = Math.min(startIndex + executeWorkerPageSize, totalItems);
    const pageData = filteredData.slice(startIndex, endIndex);

    // Update table
    const table = document.getElementById('executeWorkerTable');
    if (table) {
        const tbody = table.querySelector('tbody');
        tbody.innerHTML = '';
        pageData.forEach(worker => {
            const row = document.createElement('tr');
            row.innerHTML = worker.originalHtml;
            tbody.appendChild(row);
        });
    }

    // Update pagination info
    updatePaginationInfo('execute', startIndex + 1, endIndex, totalItems, totalPages);
}

// Render Storage Worker Table
function renderStorageWorkerTable() {
    const searchTerm = document.getElementById('storageWorkerSearch')?.value.toLowerCase() || '';
    let filteredData = storageWorkerData.filter(worker => 
        worker.groupName.toLowerCase().includes(searchTerm) ||
        worker.endpoint.toLowerCase().includes(searchTerm) ||
        worker.status.toLowerCase().includes(searchTerm)
    );

    // Apply sorting
    if (storageWorkerSortColumn) {
        filteredData.sort((a, b) => {
            let aVal = a[storageWorkerSortColumn].toLowerCase();
            let bVal = b[storageWorkerSortColumn].toLowerCase();
            if (storageWorkerSortDirection === 'asc') {
                return aVal.localeCompare(bVal);
            } else {
                return bVal.localeCompare(aVal);
            }
        });
    }

    // Apply pagination
    const totalItems = filteredData.length;
    const totalPages = Math.ceil(totalItems / storageWorkerPageSize);
    const startIndex = (storageWorkerCurrentPage - 1) * storageWorkerPageSize;
    const endIndex = Math.min(startIndex + storageWorkerPageSize, totalItems);
    const pageData = filteredData.slice(startIndex, endIndex);

    // Update table
    const table = document.getElementById('storageWorkerTable');
    if (table) {
        const tbody = table.querySelector('tbody');
        tbody.innerHTML = '';
        pageData.forEach(worker => {
            const row = document.createElement('tr');
            row.innerHTML = worker.originalHtml;
            tbody.appendChild(row);
        });
    }

    // Update pagination info
    updatePaginationInfo('storage', startIndex + 1, endIndex, totalItems, totalPages);
}

// Render Servers Table
function renderServersTable() {
    const searchTerm = document.getElementById('serversSearch')?.value.toLowerCase() || '';
    
    let filteredData = serversData.filter(server => 
        server.groupName.toLowerCase().includes(searchTerm) ||
        server.endpoint.toLowerCase().includes(searchTerm) ||
        server.serverType.toLowerCase().includes(searchTerm) ||
        server.status.toLowerCase().includes(searchTerm)
    );

    // Apply sorting
    if (serversSortColumn) {
        filteredData.sort((a, b) => {
            let aVal = a[serversSortColumn].toLowerCase();
            let bVal = b[serversSortColumn].toLowerCase();
            if (serversSortDirection === 'asc') {
                return aVal.localeCompare(bVal);
            } else {
                return bVal.localeCompare(aVal);
            }
        });
    }

    // Apply pagination
    const totalItems = filteredData.length;
    const totalPages = Math.ceil(totalItems / window.serversPageSize);
    const startIndex = (serversCurrentPage - 1) * window.serversPageSize;
    const endIndex = Math.min(startIndex + window.serversPageSize, totalItems);
    const pageData = filteredData.slice(startIndex, endIndex);

    // Update table
    const table = document.getElementById('serversTable');
    if (table) {
        const tbody = table.querySelector('tbody');
        if (tbody) {
            tbody.innerHTML = '';
            pageData.forEach((server, index) => {
                const row = document.createElement('tr');
                row.innerHTML = server.originalHtml;
                tbody.appendChild(row);
            });
        }
    }

    // Update pagination info - only if pagination elements exist
    const infoElement = document.getElementById('serversInfo');
    if (infoElement) {
        updatePaginationInfo('servers', startIndex + 1, endIndex, totalItems, totalPages);
    }
}

// Update pagination info and controls
function updatePaginationInfo(type, start, end, total, totalPages) {            
    // Defensive check - ensure we have valid parameters
    if (!type || total === undefined || total === null) {
        return;
    }
    
    let prefix, currentPage;
    if (type === 'execute') {
        prefix = 'executeWorker';
        currentPage = executeWorkerCurrentPage;
    } else if (type === 'storage') {
        prefix = 'storageWorker';
        currentPage = storageWorkerCurrentPage;
    } else if (type === 'servers') {
        prefix = 'servers';
        currentPage = serversCurrentPage;
    } else {
        return;
    }

    // Update info text - handle different element structures
    if (type === 'servers') {
        const infoElement = document.getElementById('serversInfo');
        if (infoElement) {
            infoElement.textContent = `Showing ${start} to ${end} of ${total} servers`;
        }
        const countElement = document.getElementById('serversCount');
        if (countElement) {
            countElement.textContent = total;
        }
    } else {
        // For worker tables
        const startEl = document.getElementById(prefix + 'Start');
        const endEl = document.getElementById(prefix + 'End');
        const totalEl = document.getElementById(prefix + 'Total');
        const countEl = document.getElementById(prefix + 'Count');
        
        if (startEl) startEl.textContent = total > 0 ? start : 0;
        if (endEl) endEl.textContent = end;
        if (totalEl) totalEl.textContent = total;
        if (countEl) countEl.textContent = total;
    }

    // Update pagination buttons - handle different button naming for servers
    let prevBtn, nextBtn, firstBtn, lastBtn, pageNumbersSpan;
    
    if (type === 'servers') {
        prevBtn = document.getElementById('serversPrevBtn');
        nextBtn = document.getElementById('serversNextBtn');
        firstBtn = document.getElementById('serversFirstBtn');
        lastBtn = document.getElementById('serversLastBtn');
        pageNumbersSpan = document.getElementById('serversPageNumbers');
    } else {
        prevBtn = document.getElementById(prefix + 'PrevBtn');
        nextBtn = document.getElementById(prefix + 'NextBtn');
        firstBtn = document.getElementById(prefix + 'FirstBtn');
        lastBtn = document.getElementById(prefix + 'LastBtn');
        pageNumbersSpan = document.getElementById(prefix + 'PageNumbers');
    }

    if (prevBtn) prevBtn.disabled = currentPage <= 1;
    if (nextBtn) nextBtn.disabled = currentPage >= totalPages;
    if (firstBtn) firstBtn.disabled = currentPage <= 1;
    if (lastBtn) lastBtn.disabled = currentPage >= totalPages;

    // Generate page numbers
    if (pageNumbersSpan) {
        pageNumbersSpan.innerHTML = '';
        for (let i = Math.max(1, currentPage - 2); i <= Math.min(totalPages, currentPage + 2); i++) {
            const pageBtn = document.createElement('button');
            pageBtn.className = 'pagination-btn' + (i === currentPage ? ' active' : '');
            pageBtn.textContent = i;
            pageBtn.onclick = () => {
                if (type === 'execute') {
                    executeWorkerCurrentPage = i;
                    renderExecuteWorkerTable();
                } else if (type === 'storage') {
                    storageWorkerCurrentPage = i;
                    renderStorageWorkerTable();
                } else if (type === 'servers') {
                    serversCurrentPage = i;
                    renderServersTable();
                }
            };
            pageNumbersSpan.appendChild(pageBtn);
        }
    }
}

// Page navigation functions
function changeExecuteWorkerPage(direction) {
    const totalItems = executeWorkerData.length;
    const totalPages = Math.ceil(totalItems / executeWorkerPageSize);

    executeWorkerCurrentPage += direction;
    if (executeWorkerCurrentPage < 1) executeWorkerCurrentPage = 1;
    if (executeWorkerCurrentPage > totalPages) executeWorkerCurrentPage = totalPages;

    renderExecuteWorkerTable();
}

function changeStorageWorkerPage(direction) {
    const totalItems = storageWorkerData.length;
    const totalPages = Math.ceil(totalItems / storageWorkerPageSize);

    storageWorkerCurrentPage += direction;
    if (storageWorkerCurrentPage < 1) storageWorkerCurrentPage = 1;
    if (storageWorkerCurrentPage > totalPages) storageWorkerCurrentPage = totalPages;

    renderStorageWorkerTable();
}

function changeServersPage(direction) {
    const totalItems = serversData.length;
    const totalPages = Math.ceil(totalItems / window.serversPageSize);

    serversCurrentPage += direction;
    if (serversCurrentPage < 1) serversCurrentPage = 1;
    if (serversCurrentPage > totalPages) serversCurrentPage = totalPages;

    renderServersTable();
}
