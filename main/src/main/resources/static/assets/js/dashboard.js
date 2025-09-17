/**
 * BuildFarm Dashboard JavaScript
 * Handles table sorting, search, and UI interactions
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

    // Initialize table functionality (search only, no pagination)
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

// Table Management Variables (search only)
let executeWorkerData = [];
let storageWorkerData = [];
let serversData = [];

// Initialize table functionality
function initializeTableFunctionality() {
    // Extract data from existing tables
    extractTableData();
    
    // Set up event listeners
    setupEventListeners();
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
            const cleanStatus = cells[3].textContent.trim().replace(/\([^)]*\)/g, '').trim();
            data.push({
                groupName: cells[0].textContent.trim(),
                endpoint: cells[1].textContent.trim(),
                workerType: cells[2].textContent.trim(),
                status: cleanStatus,
                expireAt: cells[4].textContent.trim(),
                firstRegisteredAt: cells[5].textContent.trim(),
                row: row
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
            const cleanStatus = cells[3].textContent.trim().replace(/\([^)]*\)/g, '').trim();
            const serverData = {
                groupName: cells[0].textContent.trim(),
                endpoint: cells[1].textContent.trim(),
                serverType: cells[2].textContent.trim(),
                status: cleanStatus,
                expireAt: cells[4].textContent.trim(),
                firstRegisteredAt: cells[5].textContent.trim(),
                row: row
            };
            data.push(serverData);
        }
    });

    return data;
}

// Set up event listeners (search only)
function setupEventListeners() {
    // Search functionality
    const executeSearch = document.getElementById('executeWorkerSearch');
    const storageSearch = document.getElementById('storageWorkerSearch');
    const serversSearch = document.getElementById('serversSearch');

    if (executeSearch) {
        executeSearch.addEventListener('input', function() {
            filterTable(executeWorkerData, this.value, 'executeWorkerCount');
        });
    }

    if (storageSearch) {
        storageSearch.addEventListener('input', function() {
            filterTable(storageWorkerData, this.value, 'storageWorkerCount');
        });
    }

    if (serversSearch) {
        serversSearch.addEventListener('input', function() {
            filterTable(serversData, this.value, 'serversCount');
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

    const headers = table.querySelectorAll('.sortable-header');
    headers.forEach(header => {
        header.addEventListener('click', function() {
            const column = this.getAttribute('data-column');
            handleSort(column, type);
        });
    });
}

function handleSort(column, type) {
    let data, sortColumn, sortDirection;
    
    if (type === 'execute') {
        data = executeWorkerData;
    } else if (type === 'storage') {
        data = storageWorkerData;
    } else if (type === 'servers') {
        data = serversData;
    }

    // Simple sorting toggle
    const currentDirection = data.sortDirection || 'asc';
    const newDirection = (data.sortColumn === column && currentDirection === 'asc') ? 'desc' : 'asc';
    
    data.sortColumn = column;
    data.sortDirection = newDirection;

    // Sort the data
    data.sort((a, b) => {
        let aVal = String(a[column]).toLowerCase();
        let bVal = String(b[column]).toLowerCase();
        if (newDirection === 'asc') {
            return aVal.localeCompare(bVal);
        } else {
            return bVal.localeCompare(aVal);
        }
    });

    // Update table display
    const tableId = type === 'execute' ? 'executeWorkerTable' : 
                   type === 'storage' ? 'storageWorkerTable' : 'serversTable';
    const countId = type === 'execute' ? 'executeWorkerCount' : 
                   type === 'storage' ? 'storageWorkerCount' : 'serversCount';
    
    updateTableDisplay(data, tableId, countId);
    updateSortHeaders(tableId, column, newDirection);
}

function updateSortHeaders(tableId, activeColumn, direction) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const headers = table.querySelectorAll('.sortable-header');
    headers.forEach(header => {
        header.classList.remove('sorted-asc', 'sorted-desc');
        if (header.getAttribute('data-column') === activeColumn) {
            header.classList.add(direction === 'asc' ? 'sorted-asc' : 'sorted-desc');
        }
    });
}

function filterTable(data, searchTerm, countElementId) {
    searchTerm = searchTerm.toLowerCase();
    let visibleCount = 0;

    data.forEach(item => {
        const matchesSearch = !searchTerm || 
            item.groupName.toLowerCase().includes(searchTerm) ||
            item.endpoint.toLowerCase().includes(searchTerm) ||
            (item.workerType && item.workerType.toLowerCase().includes(searchTerm)) ||
            (item.serverType && item.serverType.toLowerCase().includes(searchTerm)) ||
            item.status.toLowerCase().includes(searchTerm);
        
        if (item.row) {
            item.row.style.display = matchesSearch ? '' : 'none';
            if (matchesSearch) visibleCount++;
        }
    });

    // Update count
    const countElement = document.getElementById(countElementId);
    if (countElement) {
        countElement.textContent = visibleCount;
    }
}

function updateTableDisplay(data, tableId, countElementId) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const tbody = table.querySelector('tbody');
    tbody.innerHTML = '';

    let visibleCount = 0;
    data.forEach(item => {
        if (item.row && item.row.style.display !== 'none') {
            tbody.appendChild(item.row.cloneNode(true));
            visibleCount++;
        }
    });

    // Update count
    const countElement = document.getElementById(countElementId);
    if (countElement) {
        countElement.textContent = visibleCount;
    }
}