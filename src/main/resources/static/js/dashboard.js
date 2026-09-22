// Dashboard Logic

document.addEventListener('DOMContentLoaded', async () => {
    if (!window.currentUser) {
        showToast('Please login to access the dashboard', 'error');
        setTimeout(() => window.location.href = 'login.html', 1000);
        return;
    }

    // Update UI with user info
    document.getElementById('user-name').textContent = window.currentUser.fullName;
    
    // Init Settings Form
    const settingsForm = document.getElementById('settings-form');
    if (settingsForm) {
        document.getElementById('settings-fullName').value = window.currentUser.fullName;
        document.getElementById('settings-email').value = window.currentUser.email;
        document.getElementById('settings-username').value = window.currentUser.username;
    }

    // Tab Navigation
    const sidebarItems = document.querySelectorAll('.sidebar-item');
    sidebarItems.forEach(item => {
        item.addEventListener('click', () => {
            // Update active state
            sidebarItems.forEach(i => i.classList.remove('active'));
            item.classList.add('active');

            // Show tab
            const tabId = item.dataset.tab;
            document.querySelectorAll('.tab-pane').forEach(pane => pane.classList.remove('active'));
            document.getElementById(`tab-${tabId}`).classList.add('active');

            // Load data
            if (tabId === 'events') loadMyEvents();
            if (tabId === 'bookings') loadMyBookings();
        });
    });

    // Load initial data
    await loadDashboardStats();
    loadMyEvents();
    loadMyBookings();
});

async function loadDashboardStats() {
    try {
        const events = await fetchAPI('/api/events/organizer');
        document.getElementById('upcoming-events-count').textContent = events.filter(e => new Date(e.dateTime) > new Date()).length;
        document.getElementById('past-events-count').textContent = events.filter(e => new Date(e.dateTime) <= new Date()).length;
        
        // Calculate totals
        let totalAttendees = 0;
        let totalRevenue = 0;
        events.forEach(e => {
            totalAttendees += e.ticketsSold;
            totalRevenue += e.ticketsSold * e.price;
        });

        document.getElementById('total-attendees').textContent = totalAttendees;
        document.getElementById('total-revenue').textContent = formatCurrency(totalRevenue);
    } catch (err) {
        console.error('Failed to load stats:', err);
    }
}

async function loadMyEvents() {
    const grid = document.getElementById('my-events-grid');
    if (!grid) return;

    try {
        const events = await fetchAPI('/api/events/organizer');
        if (events.length === 0) {
            grid.innerHTML = '<div class="empty-state" style="grid-column: 1 / -1; text-align: center; padding: 2rem;"><p>No events created yet.</p></div>';
            return;
        }
        grid.innerHTML = events.map(e => `
            <div class="glass-panel" style="padding: 1rem;">
                <h4>${e.title}</h4>
                <p style="font-size: 0.9rem; color: var(--text-secondary);">${formatDate(e.dateTime)}</p>
                <div style="display: flex; justify-content: space-between; margin-top: 1rem; font-size: 0.9rem;">
                    <span>Sold: ${e.ticketsSold}/${e.capacity}</span>
                    <span>${formatCurrency(e.price * e.ticketsSold)}</span>
                </div>
            </div>
        `).join('');
    } catch (err) {
        grid.innerHTML = `<div class="error-state" style="grid-column: 1 / -1; padding: 2rem;">${err.message}</div>`;
    }
}

async function loadMyBookings() {
    const grid = document.getElementById('my-bookings-grid');
    if (!grid) return;

    try {
        const bookings = await fetchAPI('/api/bookings/my-bookings');
        if (bookings.length === 0) {
            grid.innerHTML = '<div class="empty-state" style="grid-column: 1 / -1; text-align: center; padding: 2rem;"><p>No bookings yet.</p></div>';
            return;
        }
        grid.innerHTML = bookings.map(b => `
            <div class="glass-panel" style="padding: 1rem;">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <h4>${b.event.title}</h4>
                    <span class="badge badge-success">${b.status}</span>
                </div>
                <p style="font-size: 0.9rem; color: var(--text-secondary); margin-top: 0.5rem;">
                    ${b.ticketCount} tickets • ${formatDate(b.bookingDate)}
                </p>
                <div style="display: flex; justify-content: space-between; margin-top: 1rem; font-size: 0.9rem; font-weight: 600;">
                    <span>Total Paid</span>
                    <span>${formatCurrency(b.totalAmount)}</span>
                </div>
            </div>
        `).join('');
    } catch (err) {
        grid.innerHTML = `<div class="error-state" style="grid-column: 1 / -1; padding: 2rem;">${err.message}</div>`;
    }
}
