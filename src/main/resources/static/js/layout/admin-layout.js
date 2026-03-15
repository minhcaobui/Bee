function toggleSidebar() {
    document.getElementById('main-sidebar').classList.toggle('collapsed');
}

function startClock() {
    const el = document.getElementById('realtimeClock');
    if (!el) return;

    function update() {
        const now = new Date();
        el.textContent = `${now.toLocaleTimeString('vi-VN', {hour12: false})} - ${now.toLocaleDateString('vi-VN')}`;
    }

    update();
    setInterval(update, 1000);
}

window.showLoading = function () {
    const loader = document.getElementById('globalLoader');
    if (loader) loader.classList.add('show');
};

window.hideLoading = function () {
    const loader = document.getElementById('globalLoader');
    if (loader) {
        setTimeout(() => {
            loader.classList.remove('show');
        }, 300);
    }
};

function toggleDropdown(id) {
    const target = document.getElementById(id);
    const isOpen = target.classList.contains('show');
    document.querySelectorAll('.dropdown-menu').forEach(m => m.classList.remove('show'));
    if (!isOpen) target.classList.add('show');
}

let hasUnread = true;

function handleNotifClick() {
    toggleDropdown('notif-dropdown');
    if (hasUnread) {
        hasUnread = false;
        const badge = document.getElementById('notif-badge');
        if (badge) badge.style.display = 'none';
    }
}

window.addEventListener('click', e => {
    if (!e.target.closest('.header-action-container'))
        document.querySelectorAll('.dropdown-menu').forEach(m => m.classList.remove('show'));
});

function confirmLogout() {
    document.getElementById('confirmOverlay').style.display = 'flex';
}

document.addEventListener('DOMContentLoaded', startClock);

function addOrderNotification(order) {
    const container = document.querySelector('.dropdown-content');
    const badge = document.getElementById('notif-badge');
    badge.style.display = 'block';
    if (container.querySelector('.empty-notif')) container.innerHTML = '';
    const notifItem = document.createElement('div');
    notifItem.style = "padding: 12px 16px; border-bottom: 1px solid #eee; cursor: pointer; transition: 0.2s;";
    notifItem.onmouseover = () => notifItem.style.background = "#f9fafb";
    notifItem.onmouseout = () => notifItem.style.background = "#fff";
    notifItem.onclick = () => {
        sessionStorage.setItem('highlightOrderId', order.id);
        if (window.location.hash === '#orders') {
            if (window.OrderApp) {
                OrderApp.loadData();
            }
        } else {
            window.location.hash = '#orders';
        }
        document.getElementById('notif-dropdown').classList.remove('show');
    };
    notifItem.innerHTML = `
        <div style="font-weight: 800; font-size: 11px; color: #d97706;">🐝 KÈO THƠM MỚI!</div>
        <div style="font-size: 12px; margin-top: 4px;">Đơn hàng <b>${order.ma}</b> vừa cập bến</div>
        <div style="font-size: 10px; color: #888; margin-top: 2px;">Vừa xong - ${order.tenKhach}</div>
    `;
    container.prepend(notifItem);
}

document.addEventListener("DOMContentLoaded", function () {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error') && urlParams.get('error') === '403') {
        if (typeof window.toast === 'function') {
            window.toast('Bạn không có quyền truy cập vào chức năng này!', 'error');
        } else {
            alert('Bạn không có quyền truy cập vào chức năng này!');
        }
        const cleanUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
        window.history.replaceState({path: cleanUrl}, '', cleanUrl);
    }
});