// Index Page Logic

document.addEventListener('DOMContentLoaded', async () => {
    const eventsGrid = document.getElementById('events-grid');
    const searchInput = document.getElementById('search-input');
    const searchBtn = document.getElementById('search-btn');

    // Load events
    await loadEvents();

    // Search functionality
    searchBtn.addEventListener('click', () => {
        loadEvents(searchInput.value);
    });

    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            loadEvents(searchInput.value);
        }
    });
});

async function loadEvents(search = '') {
    const eventsGrid = document.getElementById('events-grid');
    
    try {
        const url = search ? `/api/events?search=${encodeURIComponent(search)}` : '/api/events';
        const events = await fetchAPI(url);
        
        if (events.length === 0) {
            eventsGrid.innerHTML = `
                <div class="empty-state glass-panel" style="grid-column: 1 / -1; text-align: center; padding: 4rem;">
                    <h3>No events found</h3>
                    <p>Try a different search term or check back later.</p>
                </div>
            `;
            return;
        }

        eventsGrid.innerHTML = events.map(event => renderEventCard(event)).join('');
        
        // Add click handlers to cards
        document.querySelectorAll('.event-card').forEach(card => {
            card.addEventListener('click', () => {
                window.location.href = `event.html?id=${card.dataset.eventId}`;
            });
        });
    } catch (err) {
        eventsGrid.innerHTML = `
            <div class="error-state glass-panel" style="grid-column: 1 / -1; text-align: center; padding: 4rem;">
                <h3>Failed to load events</h3>
                <p>${err.message}</p>
            </div>
        `;
    }
}

function renderEventCard(event) {
    const statusBadge = event.ticketsRemaining > 0 
        ? `<span class="badge badge-success">Available</span>` 
        : `<span class="badge badge-error">Sold Out</span>`;

    return `
        <div class="event-card glass-panel" data-event-id="${event.id}">
            ${event.bannerUrl ? `<img src="${event.bannerUrl}" alt="${event.title}" class="event-banner">` : '<div class="event-banner-placeholder">📅</div>'}
            <div class="event-card-content">
                <div class="event-card-header">
                    ${statusBadge}
                    <span class="event-date">${formatDate(event.dateTime)}</span>
                </div>
                <h3 class="event-title">${event.title}</h3>
                <p class="event-location">📍 ${event.location}</p>
                <div class="event-footer">
                    <span class="event-price">${formatCurrency(event.price)}</span>
                    <span class="event-tickets">${event.ticketsRemaining} left</span>
                </div>
            </div>
        </div>
    `;
}
