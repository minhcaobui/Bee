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

function confirmLogout(event) {
    if (event) {
        event.preventDefault(); // Ngăn form tự động submit
        event.stopPropagation(); // Ngăn sự kiện click lan ra ngoài
    }
    document.getElementById('confirmOverlay').style.display = 'flex';
}

function executeLogout() {
    // Tìm form có chứa đường dẫn logout của Spring Security và submit
    const logoutForm = document.querySelector('form[action*="logout"]');
    if (logoutForm) {
        logoutForm.submit();
    } else {
        // Backup an toàn nếu không tìm thấy form
        window.location.href = '/logout';
    }
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

const NotifApp = {
    lastOrderId: null,
    pollingInterval: 10000,

    init: function() {
        this.bindDropdownEvent();
        this.fetchNotifications();
        setInterval(() => this.fetchNotifications(), this.pollingInterval);
    },

    bindDropdownEvent: function() {
        // Nút bấm chuông
        const btn = document.querySelector('button[onclick="handleNotifClick()"]');
        const dropdown = document.getElementById('notif-dropdown');

        btn.removeAttribute('onclick');
        btn.onclick = (e) => {
            e.stopPropagation();
            dropdown.classList.toggle('show');
        };

        document.addEventListener('click', (e) => {
            if (!dropdown.contains(e.target) && !btn.contains(e.target)) {
                dropdown.classList.remove('show');
            }
        });
    },

    fetchNotifications: async function() {
        try {
            const res = await fetch('/api/hoa-don/thong-bao-moi');
            if (!res.ok) return;
            const data = await res.json();

            this.renderUI(data);

        } catch (error) {
            console.error("Lỗi lấy thông báo:", error);
        }
    },

    renderUI: function(orders) {
        const badge = document.getElementById('notif-badge');
        const content = document.querySelector('.dropdown-content');

        if (!orders || orders.length === 0) {
            badge.style.display = 'none';
            content.innerHTML = `
                    <div class="empty-notif" style="padding:30px; text-align:center; color:#999;">
                        <i class="bi bi-bell-slash" style="font-size:24px; display:block; margin-bottom:10px;"></i>
                        <span>Không có đơn hàng mới chờ xác nhận</span>
                    </div>`;
            return;
        }

        badge.style.display = 'block';

        const newestOrderId = orders[0].id;
        if (this.lastOrderId !== null && newestOrderId > this.lastOrderId) {
            // Rung chuông (hiện Toast hoặc phát âm thanh)
            this.playDingSound();
            if(window.toast) {
                window.toast(`Có đơn hàng online mới: #${orders[0].ma}`, "success");
            }
        }
        this.lastOrderId = newestOrderId;

        let html = '<div class="notif-list">';
        orders.forEach(ord => {
            const total = new Intl.NumberFormat('vi-VN').format(ord.tongTien || 0) + ' đ';
            const timeAgo = this.timeSince(ord.ngayTao);

            html += `
                <div class="notif-item" onclick="NotifApp.goToOrder()">
                    <div class="notif-icon"><i class="bi bi-bag-check-fill"></i></div>
                    <div class="notif-content">
                        <div class="notif-title">Đơn hàng mới: #${ord.ma}</div>
                        <div class="notif-desc">Khách hàng <b>${ord.khachHang}</b> vừa đặt hàng với tổng giá trị <b>${total}</b>.</div>
                        <div class="notif-time"><i class="bi bi-clock"></i> ${timeAgo}</div>
                    </div>
                </div>`;
        });
        html += '</div>';

        html += `
                <div style="text-align:center; padding:10px; border-top:1px solid #eee; font-size:12px; font-weight:700; cursor:pointer; color:#2563eb;" onclick="NotifApp.goToOrder()">
                    XEM TẤT CẢ HÓA ĐƠN <i class="bi bi-arrow-right"></i>
                </div>
            `;

        content.innerHTML = html;
    },

    goToOrder: function() {
        document.getElementById('notif-dropdown').classList.remove('show');
        window.location.hash = 'orders'; // Điều hướng sang Tab hóa đơn

        // Nếu bạn có hàm chuyển tab sẵn thì gọi luôn ở đây
        if(window.OrderApp && typeof window.OrderApp.switchTab === 'function') {
            setTimeout(() => window.OrderApp.switchTab('don-hang'), 100);
        }
    },

    playDingSound: function() {
        // Hiệu ứng âm thanh nhỏ báo đơn mới (Tùy chọn)
        try {
            const audio = new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3');
            audio.volume = 0.5;
            audio.play().catch(e => console.log("Trình duyệt chặn autoplay âm thanh"));
        } catch(e){}
    },

    timeSince: function(dateStr) {
        if (!dateStr) return "Gần đây";
        const date = new Date(dateStr.replace(/-/g, '/'));
        const seconds = Math.floor((new Date() - date) / 1000);

        let interval = seconds / 31536000;
        if (interval > 1) return Math.floor(interval) + " năm trước";
        interval = seconds / 2592000;
        if (interval > 1) return Math.floor(interval) + " tháng trước";
        interval = seconds / 86400;
        if (interval > 1) return Math.floor(interval) + " ngày trước";
        interval = seconds / 3600;
        if (interval > 1) return Math.floor(interval) + " giờ trước";
        interval = seconds / 60;
        if (interval > 1) return Math.floor(interval) + " phút trước";
        return "Vừa xong";
    }
};

document.addEventListener("DOMContentLoaded", () => {
    NotifApp.init();
});

setInterval(() => {
    if(typeof fetchNotifications === 'function') {
        fetchNotifications();
    }
}, 30000);