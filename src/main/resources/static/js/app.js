// Global User Context
window.currentUser = null;

// ==========================================
// API Helper with CSRF Token Handling
// ==========================================
async function fetchAPI(url, options = {}) {
    const defaultHeaders = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
    };

    // Add CSRF token from cookie if it exists
    const csrfToken = getCookie('XSRF-TOKEN');
    if (csrfToken) {
        defaultHeaders['X-XSRF-TOKEN'] = csrfToken;
    }

    const config = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...options.headers,
        },
        credentials: 'include', // Important for session cookies
    };

    const response = await fetch(url, config);

    // Handle 401 Unauthorized
    if (response.status === 401) {
        window.currentUser = null;
        if (!url.includes('/api/auth/me')) {
            window.location.href = 'login.html';
        }
        throw new Error('Session expired. Please login again.');
    }

    // Handle 403 Forbidden
    if (response.status === 403) {
        throw new Error('You do not have permission to perform this action.');
    }

    // Handle non-JSON responses
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || data.error || 'An error occurred');
        }
        return data;
    }

    if (!response.ok) {
        throw new Error('An error occurred');
    }

    return response.text();
}

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// ==========================================
// Toast Notification System
// ==========================================
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container') || createToastContainer();
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ';
    toast.innerHTML = `
        <span class="toast-icon">${icon}</span>
        <span class="toast-message">${message}</span>
        <button class="toast-close">&times;</button>
    `;
    
    container.appendChild(toast)

    // Auto-remove after 4 seconds
    const timer = setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 4000)

    toast.querySelector('.toast-close').addEventListener('click', () => {
        clearTimeout(timer)
        toast.remove()
    })
}

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
    return container;
}

// ==========================================
// Date & Currency Formatters
// ==========================================
function formatDate(dateStr) {
    const d = new Date(dateStr)
    return d.toLocaleDateString('en-US', {
        weekday: 'short', year: 'numeric', month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit'
    })
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount)
}

// ==========================================
// Navigation Updates
// ==========================================
function updateNavigation() {
    const authNav = document.getElementById('auth-nav-links')
    if (!authNav) return

    if (window.currentUser) {
        authNav.innerHTML = `
            <li><a href="index.html" class="${isActivePage('index.html')}">Explore</a></li>
            <li><a href="dashboard.html" class="${isActivePage('dashboard.html')}">Dashboard</a></li>
            <li>
                <button onclick="handleLogout()" class="btn btn-secondary" style="padding: 0.4rem 1rem; font-size: 0.85rem;">
                    Sign Out
                </button>
            </li>
            <li style="margin-left: 0.5rem; display: flex; align-items: center; gap: 0.5rem;">
                <div style="width: 32px; height: 32px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), var(--secondary)); display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 0.85rem;">
                    ${window.currentUser.fullName.charAt(0).toUpperCase()}
                </div>
            </li>
        `
    } else {
        authNav.innerHTML = `
            <li><a href="index.html" class="${isActivePage('index.html')}">Explore</a></li>
            <li><a href="login.html" class="btn btn-primary" style="padding: 0.5rem 1.2rem; font-size: 0.85rem;">Sign In</a></li>
        `
    }
}

function isActivePage(filename) {
    const currentPage = window.location.pathname.split('/').pop()
    if (currentPage === '' && filename === 'index.html') return 'active'
    return currentPage === filename ? 'active' : ''
}

// ==========================================
// Authentication
// ==========================================
async function handleLogout() {
    try {
        await fetchAPI('/api/auth/logout', { method: 'POST' })
        window.currentUser = null
        showToast('Logged out successfully', 'success')
        setTimeout(() => {
            window.location.href = 'index.html'
        }, 1000)
    } catch (err) {
        showToast(err.message || 'Failed to logout', 'error')
    }
}

async function checkAuthSession() {
    try {
        const user = await fetchAPI('/api/auth/me')
        if (user && !user.error) {
            window.currentUser = user
        }
    } catch (err) {
        // Ignored: User not logged in
    } finally {
        updateNavigation()
    }
}

// ==========================================
// Razorpay Checkout Helper
// ==========================================
async function initiateRazorpayCheckout(eventId, ticketCount, eventName, totalAmount) {
    try {
        // Step 1: Create Order on Backend
        const orderResponse = await fetchAPI('/api/bookings/create-order', {
            method: 'POST',
            body: JSON.stringify({ eventId, ticketCount })
        });

        const orderId = orderResponse.orderId;

        // Step 2: Open Razorpay Checkout
        const options = {
            key: 'rzp_test_YOUR_KEY_ID', // Replace with your actual key or fetch from backend
            amount: totalAmount * 100, // Amount in paise
            currency: 'INR',
            name: 'EventHub',
            description: `Booking for ${eventName}`,
            order_id: orderId,
            handler: async function (response) {
                try {
                    // Step 3: Verify Payment on Backend
                    const verifyResponse = await fetchAPI(
                        `/api/bookings/verify-payment?eventId=${eventId}&ticketCount=${ticketCount}`, 
                        {
                            method: 'POST',
                            body: JSON.stringify({
                                razorpayOrderId: response.razorpay_order_id,
                                razorpayPaymentId: response.razorpay_payment_id,
                                razorpaySignature: response.razorpay_signature
                            })
                        }
                    );
                    showToast('Payment successful! Booking confirmed.', 'success');
                    setTimeout(() => window.location.href = 'dashboard.html', 1500);
                } catch (err) {
                    showToast('Payment verification failed: ' + err.message, 'error');
                }
            },
            prefill: {
                name: window.currentUser ? window.currentUser.fullName : '',
                email: window.currentUser ? window.currentUser.email : ''
            },
            theme: {
                color: '#6366f1'
            }
        };

        const rzp = new Razorpay(options);
        rzp.on('payment.failed', function (response) {
            showToast('Payment failed: ' + response.error.description, 'error');
        });
        rzp.open();

    } catch (err) {
        showToast('Error initiating payment: ' + err.message, 'error');
    }
}

// ==========================================
// QR Code Generator (Mock)
// ==========================================
function generateMockQRCode(parentEl, payload) {
    if (!parentEl) return
    parentEl.innerHTML = ''

    let hash = 0
    for (let i = 0; i < payload.length; i++) {
        hash = payload.charCodeAt(i) + ((hash << 5) - hash)
    }

    const size = 16
    parentEl.style.width = '100px'
    parentEl.style.height = '100px'
    parentEl.style.display = 'flex'
    parentEl.style.flexWrap = 'wrap'
    parentEl.style.background = 'white'
    parentEl.style.padding = '4px'

    for (let row = 0; row < size; row++) {
        for (let col = 0; col < size; col++) {
            const pixel = document.createElement('div')
            pixel.style.width = '6px'
            pixel.style.height = '6px'

            const isCorner =
                (row < 4 && col < 4) ||
                (row < 4 && col >= size - 4) ||
                (row >= size - 4 && col < 4)

            if (isCorner) {
                const inner = (row === 0 || row === 3 || col === 0 || col === 3 ||
                               row === size - 1 || row === size - 4 || col === size - 1 || col === size - 4)
                pixel.style.backgroundColor = inner ? '#000' : '#fff'
                if ((row === 1 && col === 1) || (row === 1 && col === size - 2) || (row === size - 2 && col === 1) ||
                    (row === 2 && col === 2) || (row === 2 && col === size - 3) || (row === size - 3 && col === 2)) {
                    pixel.style.backgroundColor = '#000'
                }
            } else {
                const val = Math.abs(Math.sin(hash + (row * size) + col) * 1000)
                pixel.style.backgroundColor = (val % 2 < 1) ? '#000' : '#fff'
            }
            parentEl.appendChild(pixel)
        }
    }
}

// Initialise auth session extraction
document.addEventListener('DOMContentLoaded', checkAuthSession)
