// Check if user is logged in on page load
document.addEventListener('DOMContentLoaded', () => {
    checkLoginStatus();
});

async function checkLoginStatus() {
    try {
        const response = await fetch('/api/auth/check-session');
        const result = await response.json();

        if (result.success) {
            const sessionData = result.data && typeof result.data === 'object'
                ? result.data
                : { username: result.data, role: 'USER' };
            const role = sessionData.role || 'USER';

            // User is logged in - redirect to dashboard
            if (window.location.pathname === '/') {
                window.location.href = role === 'ADMIN' ? '/admin' : '/dashboard';
                return;
            }

            if (window.location.pathname === '/payment' && role !== 'USER' && role !== 'ADMIN') {
                window.location.href = '/login';
                return;
            }
            
            // Show username and logout button on other pages
            const logoutBtn = document.getElementById('logoutBtn');
            const usernameSpan = document.getElementById('usernameSpan');
            const adminLink = document.getElementById('adminLink');
            const username = sessionData.username || sessionStorage.getItem('username') || 'User';
            
            if (logoutBtn) {
                logoutBtn.style.display = 'inline-block';
            }
            
            if (usernameSpan) {
                usernameSpan.style.display = 'inline';
                usernameSpan.textContent = 'Hello, ' + username;
            }

            if (adminLink) {
                adminLink.style.display = role === 'ADMIN' ? 'inline' : 'none';
            }

            // Add logout functionality
            if (logoutBtn) {
                logoutBtn.addEventListener('click', logout);
            }
        }
    } catch (error) {
        if (window.location.pathname === '/payment') {
            window.location.href = '/login';
            return;
        }
        console.log('User not logged in');
    }
}

async function logout(e) {
    e.preventDefault();
    
    try {
        const response = await fetch('/api/auth/logout', {
            method: 'POST'
        });

        const result = await response.json();
        if (result.success) {
            sessionStorage.removeItem('username');
            window.location.href = '/';
        }
    } catch (error) {
        console.error('Logout error:', error);
        window.location.href = '/';
    }
}
