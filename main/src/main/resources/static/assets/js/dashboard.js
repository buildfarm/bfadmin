/**
 * BuildFarm Dashboard JavaScript
 * Handles async data loading, table sorting and UI interactions
 */

document.addEventListener('DOMContentLoaded', function() {
    // Add fade-in animation to elements
    const animatedElements = document.querySelectorAll('.fade-in');
    animatedElements.forEach((el, index) => {
        el.style.animationDelay = `${index * 0.1}s`;
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

    // Load dashboard data asynchronously
    loadDashboardData();
    
    // Update timestamps
    updateRelativeTime();
    setInterval(updateRelativeTime, 60000); // Update every minute
});

// Global variables for table data
let executeWorkerData = [];
let storageWorkerData = [];
let serversData = [];

// Load dashboard data from async API
async function loadDashboardData() {
    try {
        const response = await fetch('/api/dashboard/data');
        const data = await response.json();
        
        if (data.error) {
            showError(data.error);
            return;
        }
        
        // Update system status
        updateSystemStatus(data.systemStatus);
        
        // Load workers and servers tables
        loadExecuteWorkers(data.executeWorkersTable);
        loadStorageWorkers(data.storageWorkersTable);
        loadServers(data.serversTable);
        
    } catch (error) {
        console.error('Failed to load dashboard data:', error);
        showError('Failed to load dashboard data: ' + error.message);
    }
}

// Update system status cards
function updateSystemStatus(systemStatus) {
    const statusCards = document.querySelectorAll('#systemStatusGrid .card');
    const systemHealthBadge = document.getElementById('systemHealthBadge');
    
    if (systemHealthBadge) {
        systemHealthBadge.innerHTML = `
            <i class="bi bi-${systemStatus.systemHealthy ? 'check-circle' : 'exclamation-triangle'}"></i>
            ${systemStatus.systemHealthText}`;
        systemHealthBadge.className = `status-badge status-${systemStatus.systemHealthClass}`;
    }
    
    // Update individual status cards with real data
    if (statusCards.length >= 4) {
        // Valkey status
        statusCards[0].querySelector('.ms-3.stats-content').innerHTML = `
            <h3>${systemStatus.valkeyStatus}</h3>
            <p>Valkey Cluster</p>`;
        statusCards[0].className = `card stats-card ${systemStatus.valkeyConnected ? 'green' : 'red'} fade-in`;
        
        // Execute workers
        statusCards[1].querySelector('.ms-3.stats-content').innerHTML = `
            <h3>${systemStatus.activeExecuteCount}/${systemStatus.executeWorkerCount}</h3>
            <p>Execute Workers</p>`;
        statusCards[1].className = `card stats-card ${systemStatus.executeWorkerClass === 'success' ? 'green' : 'orange'} fade-in`;
        
        // Storage workers
        statusCards[2].querySelector('.ms-3.stats-content').innerHTML = `
            <h3>${systemStatus.activeStorageCount}/${systemStatus.storageWorkerCount}</h3>
            <p>Storage Workers</p>`;
        statusCards[2].className = `card stats-card ${systemStatus.storageWorkerClass === 'success' ? 'green' : 'orange'} fade-in`;
        
        // Servers
        statusCards[3].querySelector('.ms-3.stats-content').innerHTML = `
            <h3>${systemStatus.activeServerCount}/${systemStatus.serverCount}</h3>
            <p>Servers</p>`;
        statusCards[3].className = `card stats-card ${systemStatus.serverClass === 'success' ? 'green' : 'purple'} fade-in`;
    }
    
    // Show error if present
    if (systemStatus.error) {
        showError(systemStatus.error);
    }
}

// Load execute workers table
function loadExecuteWorkers(workers) {
    const loading = document.getElementById('executeWorkersLoading');
    const container = document.getElementById('executeWorkersTableContainer');
    const emptyMessage = document.getElementById('executeWorkersEmptyMessage');
    const countBadge = document.getElementById('executeWorkerCount');
    
    loading.classList.add('d-none');
    
    if (workers && workers.length > 0) {
        container.classList.remove('d-none');
        populateWorkersTable('executeWorkerTable', workers);
        countBadge.textContent = `${workers.length} Workers`;
        executeWorkerData = workers;
        setupSortingListeners('executeWorkerTable', 'execute');
    } else {
        emptyMessage.classList.remove('d-none');
        countBadge.textContent = '0 Workers';
    }
}

// Load storage workers table
function loadStorageWorkers(workers) {
    const loading = document.getElementById('storageWorkersLoading');
    const container = document.getElementById('storageWorkersTableContainer');
    const emptyMessage = document.getElementById('storageWorkersEmptyMessage');
    const countBadge = document.getElementById('storageWorkerCount');
    
    loading.classList.add('d-none');
    
    if (workers && workers.length > 0) {
        container.classList.remove('d-none');
        populateWorkersTable('storageWorkerTable', workers);
        countBadge.textContent = `${workers.length} Workers`;
        storageWorkerData = workers;
        setupSortingListeners('storageWorkerTable', 'storage');
    } else {
        emptyMessage.classList.remove('d-none');
        countBadge.textContent = '0 Workers';
    }
}

// Load servers table
function loadServers(servers) {
    const loading = document.getElementById('serversLoading');
    const container = document.getElementById('serversTableContainer');
    const emptyMessage = document.getElementById('serversEmptyMessage');
    const countBadge = document.getElementById('serversCount');
    
    loading.classList.add('d-none');
    
    if (servers && servers.length > 0) {
        container.classList.remove('d-none');
        populateServersTable('serversTable', servers);
        countBadge.textContent = `${servers.length} Servers`;
        serversData = servers;
        setupSortingListeners('serversTable', 'servers');
    } else {
        emptyMessage.classList.remove('d-none');
        countBadge.textContent = '0 Servers';
    }
}

// Populate workers table
function populateWorkersTable(tableId, workers) {
    const table = document.getElementById(tableId);
    if (!table) return;
    
    const tbody = table.querySelector('tbody');
    tbody.innerHTML = '';
    
    workers.forEach((worker, index) => {
        const row = document.createElement('tr');
        const isActive = worker.status && worker.status.toString().startsWith('Active');
        const isExpired = worker.status === 'Expired';
        
        row.innerHTML = `
            <td><span class="status-badge status-secondary">${worker.groupName || 'default'}</span></td>
            <td><span class="text-info">${worker.endpoint}</span></td>
            <td>
                <span class="status-badge ${tableId === 'executeWorkerTable' ? 'status-success' : 'status-info'}">
                    <i class="bi bi-${tableId === 'executeWorkerTable' ? 'cpu' : 'hdd'}"></i> 
                    <span>${worker.workerType}</span>
                </span>
            </td>
            <td>
                <span class="status-badge ${isActive ? 'status-success' : (isExpired ? 'status-danger' : 'status-warning')}">
                    <i class="bi bi-${isActive ? 'check-circle' : (isExpired ? 'x-circle' : 'exclamation-circle')}"></i>
                    <span>${worker.status}</span>
                </span>
            </td>
            <td><small class="text-muted">${worker.expireAt}</small></td>
            <td><small class="text-muted">${worker.firstRegisteredAt}</small></td>
        `;
        tbody.appendChild(row);
    });
}

// Populate servers table
function populateServersTable(tableId, servers) {
    const table = document.getElementById(tableId);
    if (!table) return;
    
    const tbody = table.querySelector('tbody');
    tbody.innerHTML = '';
    
    servers.forEach((server, index) => {
        const row = document.createElement('tr');
        const isActive = server.status && server.status.toString().startsWith('Active');
        const isExpired = server.status === 'Expired';
        
        row.innerHTML = `
            <td><span class="status-badge status-secondary">${server.groupName || 'default'}</span></td>
            <td><span class="text-info">${server.endpoint}</span></td>
            <td>
                <span class="status-badge ${server.serverType === 'Scheduler' ? 'status-info' : (server.serverType === 'CAS' ? 'status-primary' : 'status-success')}">
                    <i class="bi bi-${server.serverType === 'Scheduler' ? 'gear' : (server.serverType === 'CAS' ? 'database' : 'server')}"></i> 
                    <span>${server.serverType}</span>
                </span>
            </td>
            <td>
                <span class="status-badge ${isActive ? 'status-success' : (isExpired ? 'status-danger' : 'status-warning')}">
                    <i class="bi bi-${isActive ? 'check-circle' : (isExpired ? 'x-circle' : 'exclamation-circle')}"></i>
                    <span>${server.status}</span>
                </span>
            </td>
            <td><small class="text-muted">${server.expireAt}</small></td>
            <td><small class="text-muted">${server.firstRegisteredAt}</small></td>
        `;
        tbody.appendChild(row);
    });
}

// Set up sorting listeners for tables
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

// Handle table sorting
function handleSort(column, type) {
    let data;
    
    if (type === 'execute') {
        data = executeWorkerData;
    } else if (type === 'storage') {
        data = storageWorkerData;
    } else if (type === 'servers') {
        data = serversData;
    }

    if (!data || data.length === 0) return;

    // Simple sorting toggle
    const currentDirection = data.sortDirection || 'asc';
    const newDirection = (data.sortColumn === column && currentDirection === 'asc') ? 'desc' : 'asc';
    
    data.sortColumn = column;
    data.sortDirection = newDirection;

    // Sort the data
    data.sort((a, b) => {
        let aVal = String(a[column] || '').toLowerCase();
        let bVal = String(b[column] || '').toLowerCase();
        if (newDirection === 'asc') {
            return aVal.localeCompare(bVal);
        } else {
            return bVal.localeCompare(aVal);
        }
    });

    // Repopulate table with sorted data
    const tableId = type === 'execute' ? 'executeWorkerTable' : 
                   type === 'storage' ? 'storageWorkerTable' : 'serversTable';
    
    if (type === 'servers') {
        populateServersTable(tableId, data);
    } else {
        populateWorkersTable(tableId, data);
    }
    
    updateSortHeaders(tableId, column, newDirection);
}

// Update sort headers visual indicators
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

// Show error message
function showError(message) {
    const errorAlert = document.getElementById('errorAlert');
    const errorMessage = document.getElementById('errorMessage');
    
    if (errorAlert && errorMessage) {
        errorMessage.textContent = message;
        errorAlert.classList.remove('d-none');
    }
}

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