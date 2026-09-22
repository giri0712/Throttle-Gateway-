// Authentication Logic for Login and Register Pages

document.addEventListener('DOMContentLoaded', () => {
    // Login Form
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;

            try {
                await fetchAPI('/api/auth/login', {
                    method: 'POST',
                    body: JSON.stringify({ username, password })
                });
                showToast('Login successful!', 'success');
                setTimeout(() => window.location.href = 'dashboard.html', 500);
            } catch (err) {
                showToast(err.message || 'Login failed', 'error');
            }
        });
    }

    // Register Form
    const registerForm = document.getElementById('register-form');
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const data = {
                username: document.getElementById('username').value,
                email: document.getElementById('email').value,
                fullName: document.getElementById('fullName').value,
                password: document.getElementById('password').value,
                role: 'ROLE_' + document.getElementById('role').value.toUpperCase()
            };

            try {
                await fetchAPI('/api/auth/register', {
                    method: 'POST',
                    body: JSON.stringify(data)
                });
                showToast('Registration successful! Redirecting to login...', 'success');
                setTimeout(() => window.location.href = 'login.html', 1500);
            } catch (err) {
                showToast(err.message || 'Registration failed', 'error');
            }
        });
    }
});
