// Ghi đè hàm toggleSidebar để hỗ trợ xử lý cả Desktop và Mobile
window.toggleSidebar = function() {
    const sidebar = document.getElementById('main-sidebar');
    const overlay = document.getElementById('sidebar-overlay');

    if (window.innerWidth <= 768) {
        // Trên điện thoại: Trượt menu ra/vào và hiện overlay
        sidebar.classList.toggle('mobile-open');
        if (overlay) overlay.classList.toggle('show');
        // Xoá class collapsed nếu có để tránh lỗi hiển thị
        sidebar.classList.remove('collapsed');
    } else {
        // Trên máy tính: Thu nhỏ menu như bình thường
        sidebar.classList.toggle('collapsed');
        sidebar.classList.remove('mobile-open');
        if (overlay) overlay.classList.remove('show');
    }
};

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

/* =========================================
   HÀM TOAST THÔNG BÁO DÙNG CHUNG (CẬP NHẬT GIAO DIỆN PHẲNG)
   ========================================= */
window.toast = function(message, type = 'success') {
    let toastHost = document.getElementById('toastHost');

    // KIỂM TRA QUAN TRỌNG: Ép khung Toast phải nằm ở lớp ngoài cùng (con trực tiếp của body)
    if (!toastHost || toastHost.parentElement !== document.body) {
        if (toastHost) toastHost.remove();
        toastHost = document.createElement('div');
        toastHost.id = 'toastHost';
        toastHost.className = 'toast-container';
        document.body.appendChild(toastHost);
    }

    toastHost.innerHTML = ''; // Xóa thông báo cũ đi nếu có

    const toast = document.createElement('div');
    toast.className = `custom-toast toast-${type}`;

    let iconClass = 'bi-check-circle-fill';
    if (type === 'error') iconClass = 'bi-x-circle-fill';
    else if (type === 'warning') iconClass = 'bi-exclamation-triangle-fill';
    else if (type === 'info') iconClass = 'bi-info-circle-fill';

    toast.innerHTML = `
        <i class="bi ${iconClass} toast-icon"></i>
        <span class="toast-msg">${message}</span>
        <button class="toast-close" onclick="this.parentElement.remove()"><i class="bi bi-x"></i></button>
    `;

    toastHost.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideOutRight 0.3s forwards';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
};

/* =========================================
   HÀM MODAL XÁC NHẬN DÙNG CHUNG (CẬP NHẬT GIAO DIỆN CORPORATE)
   ========================================= */
window.confirmDialog = function(title, message, onConfirmCallback) {
    let overlay = document.getElementById('genericConfirmOverlay');

    // KIỂM TRA QUAN TRỌNG: Ép khung Xác nhận phải nằm ở lớp ngoài cùng
    if (!overlay || overlay.parentElement !== document.body) {
        if (overlay) overlay.remove();

        // Tự động tạo lại và gắn thẳng vào thẻ body
        overlay = document.createElement('div');
        overlay.id = 'genericConfirmOverlay';
        overlay.className = 'overlay';
        overlay.style.display = 'none';
        overlay.innerHTML = `
            <div class="card" style="padding:24px; width:360px; background:#fff; border:1px solid var(--border); border-radius: var(--border-radius); box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
                <h4 id="genericConfirmTitle" style="margin-bottom:12px; font-size:15px; font-weight: 700; display: flex; align-items: center; gap: 8px;"></h4>
                <div id="genericConfirmMsg" style="margin-bottom:24px; color:var(--sub); font-size: 13px; font-weight: 500; line-height: 1.5;"></div>
                <div style="display:flex; justify-content:flex-end; gap:10px;">
                    <button id="btnGenericCancel" class="btn btn-secondary">HỦY BỎ</button>
                    <button id="btnGenericOk" class="btn btn-primary">ĐỒNG Ý</button>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);
    }

    const titleEl = document.getElementById('genericConfirmTitle');
    const msgEl = document.getElementById('genericConfirmMsg');
    const btnOk = document.getElementById('btnGenericOk');
    const btnCancel = document.getElementById('btnGenericCancel');

    titleEl.innerHTML = `<i class="bi bi-question-circle" style="color: var(--primary); font-size: 18px;"></i> ${title}`;
    msgEl.innerHTML = message;

    overlay.style.display = 'flex';

    btnOk.onclick = null;
    btnCancel.onclick = null;

    btnOk.onclick = function() {
        overlay.style.display = 'none';
        if (typeof onConfirmCallback === 'function') {
            onConfirmCallback();
        }
    };

    btnCancel.onclick = function() {
        overlay.style.display = 'none';
    };
};

document.addEventListener('DOMContentLoaded', () => {
    startClock();

    // Tự động đóng menu trên điện thoại khi người dùng bấm chọn một mục
    document.querySelectorAll('.sidebar-item').forEach(item => {
        item.addEventListener('click', () => {
            if (window.innerWidth <= 768) {
                document.getElementById('main-sidebar').classList.remove('mobile-open');
                const overlay = document.getElementById('sidebar-overlay');
                if (overlay) overlay.classList.remove('show');
            }
        });
    });
});

function addOrderNotification(order) {
    const container = document.querySelector('.dropdown-content');
    const badge = document.getElementById('notif-badge');
    badge.style.display = 'block';
    if (container.querySelector('.empty-notif')) container.innerHTML = '';
    const notifItem = document.createElement('div');
    notifItem.style = "padding: 15px 20px; border-bottom: 1px solid var(--border); cursor: pointer; transition: 0.2s;";
    notifItem.onmouseover = () => notifItem.style.background = "var(--panel-hover)";
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
        <div style="font-weight: 600; font-size: 12px; color: var(--primary); margin-bottom: 2px;">Đơn hàng mới!</div>
        <div style="font-size: 12px; color: var(--text-main);">Mã đơn: <b>${order.ma}</b></div>
        <div style="font-size: 11px; color: var(--sub); margin-top: 4px;">Vừa xong - ${order.tenKhach}</div>
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
        const btn = document.querySelector('button[onclick="handleNotifClick()"]');
        const dropdown = document.getElementById('notif-dropdown');

        if(btn) {
            btn.removeAttribute('onclick');
            btn.onclick = (e) => {
                e.stopPropagation();
                dropdown.classList.toggle('show');
            };
        }

        document.addEventListener('click', (e) => {
            if (dropdown && btn && !dropdown.contains(e.target) && !btn.contains(e.target)) {
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
        if(!badge || !content) return;

        if (!orders || orders.length === 0) {
            badge.style.display = 'none';
            content.innerHTML = `
                    <div class="empty-notif" style="padding:30px; text-align:center; color:var(--sub);">
                        <i class="bi bi-bell-slash" style="font-size:24px; display:block; margin-bottom:10px;"></i>
                        <span>Không có đơn hàng mới chờ xác nhận</span>
                    </div>`;
            return;
        }

        badge.style.display = 'block';

        const newestOrderId = orders[0].id;
        if (this.lastOrderId !== null && newestOrderId > this.lastOrderId) {
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
                <div style="text-align:center; padding:12px; border-top:1px solid var(--border); font-size:12px; font-weight:600; cursor:pointer; color:var(--primary); background:var(--panel-hover);" onclick="NotifApp.goToOrder()">
                    XEM TẤT CẢ HÓA ĐƠN <i class="bi bi-arrow-right"></i>
                </div>
            `;

        content.innerHTML = html;
    },

    goToOrder: function() {
        document.getElementById('notif-dropdown').classList.remove('show');
        window.location.hash = 'orders';

        if(window.OrderApp && typeof window.OrderApp.switchTab === 'function') {
            setTimeout(() => window.OrderApp.switchTab('don-hang'), 100);
        }
    },

    playDingSound: function() {
        try {
            const audio = new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3');
            audio.volume = 0.3; // Giảm âm lượng một chút cho đỡ chói (Nghiêm túc hơn)
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