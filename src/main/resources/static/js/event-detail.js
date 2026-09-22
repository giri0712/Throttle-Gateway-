// Event Detail Page Logic

document.addEventListener('DOMContentLoaded', async () => {
    const container = document.getElementById('event-detail-container');
    const urlParams = new URLSearchParams(window.location.search);
    const eventId = urlParams.get('id');

    if (!eventId) {
        container.innerHTML = '<div class="glass-panel" style="text-align: center; padding: 3rem;"><h2>No Event ID provided</h2><p>Please go back to the <a href="index.html">Explore page</a>.</p></div>';
        return;
    }

    try {
        const event = await fetchAPI(`/api/events/${eventId}`);
        renderEventDetail(event, container);
    } catch (err) {
        container.innerHTML = `<div class="glass-panel" style="text-align: center; padding: 3rem;"><h2>Error loading event</h2><p>${err.message}</p></div>`;
    }
});

function renderEventDetail(event, container) {
    container.innerHTML = `
        <div class="glass-panel event-detail-card">
            ${event.bannerUrl ? `<img src="${event.bannerUrl}" alt="${event.title}" class="event-banner">` : '<div class="event-banner-placeholder">📅</div>'}
            
            <div class="event-content">
                <div class="event-meta">
                    <span class="badge badge-date">${formatDate(event.dateTime)}</span>
                    <span class="badge badge-location">📍 ${event.location}</span>
                </div>
                
                <h1>${event.title}</h1>
                <p class="event-description">${event.description}</p>
                
                <div class="event-stats">
                    <div class="stat-item">
                        <span class="stat-label">Price</span>
                        <span class="stat-value">${formatCurrency(event.price)}</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">Available</span>
                        <span class="stat-value ${event.ticketsRemaining <= 0 ? 'text-error' : 'text-success'}">${event.ticketsRemaining} / ${event.capacity}</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">Sold</span>
                        <span class="stat-value">${event.ticketsSold}</span>
                    </div>
                </div>

                <div class="booking-section" id="booking-section">
                    ${event.ticketsRemaining > 0 ? renderBookingForm(event) : '<div class="sold-out-banner">❌ Sold Out</div>'}
                </div>
            </div>
        </div>
    `;

    if (event.ticketsRemaining > 0) {
        initBookingForm(event);
    }
}

function renderBookingForm(event) {
    return `
        <div class="glass-panel booking-form-container">
            <h3>Get Tickets</h3>
            <div class="form-group">
                <label for="ticket-count">Number of Tickets</label>
                <input type="number" id="ticket-count" min="1" max="${event.ticketsRemaining}" value="1">
            </div>
            <div class="price-summary">
                <span>Total:</span>
                <span id="total-price">${formatCurrency(event.price)}</span>
            </div>
            <button id="pay-now-btn" class="btn btn-primary btn-block" style="width: 100%; margin-top: 1rem;">
                Pay with Razorpay
            </button>
            <div id="booking-loading" class="loading-spinner" style="display: none; margin-top: 1rem;">
                <div class="spinner small"></div>
                <p>Processing...</p>
            </div>
        </div>
    `;
}

function initBookingForm(event) {
    const countInput = document.getElementById('ticket-count');
    const totalDisplay = document.getElementById('total-price');
    const payBtn = document.getElementById('pay-now-btn');
    const loading = document.getElementById('booking-loading');

    countInput.addEventListener('input', () => {
        const count = parseInt(countInput.value) || 0;
        totalDisplay.textContent = formatCurrency(event.price * count);
    });

    payBtn.addEventListener('click', async () => {
        if (!window.currentUser) {
            showToast('Please login to book tickets', 'error');
            setTimeout(() => window.location.href = 'login.html', 1000);
            return;
        }

        const count = parseInt(countInput.value);
        if (count <= 0) {
            showToast('Please select at least 1 ticket', 'error');
            return;
        }

        payBtn.style.display = 'none';
        loading.style.display = 'flex';

        try {
            await initiateRazorpayCheckout(
                event.id, 
                count, 
                event.title, 
                event.price * count
            );
        } finally {
            payBtn.style.display = 'block';
            loading.style.display = 'none';
        }
    });
}
