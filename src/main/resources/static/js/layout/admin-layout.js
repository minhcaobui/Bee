// Ghi đè hàm toggleSidebar để hỗ trợ xử lý cả Desktop và Mobile
window.toggleSidebar = function() {
    const sidebar = document.getElementById('main-sidebar');
    const overlay = document.getElementById('sidebar-overlay');

    if (window.innerWidth <= 768) {
        sidebar.classList.toggle('mobile-open');
        if (overlay) overlay.classList.toggle('show');
        sidebar.classList.remove('collapsed');
    } else {
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
    if (loader) setTimeout(() => loader.classList.remove('show'), 300);
};

function toggleDropdown(id) {
    const target = document.getElementById(id);
    const isOpen = target.classList.contains('show');
    document.querySelectorAll('.dropdown-menu').forEach(m => m.classList.remove('show'));
    if (!isOpen) target.classList.add('show');
}

function handleNotifClick() {
    toggleDropdown('notif-dropdown');
    const badge = document.getElementById('notif-badge');
    if (badge) badge.style.display = 'none';
}

window.addEventListener('click', e => {
    if (!e.target.closest('.header-action-container'))
        document.querySelectorAll('.dropdown-menu').forEach(m => m.classList.remove('show'));
});

function confirmLogout(event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    document.getElementById('confirmOverlay').style.display = 'flex';
}

function executeLogout() {
    const logoutForm = document.querySelector('form[action*="logout"]');
    if (logoutForm) {
        logoutForm.submit();
    } else {
        window.location.href = '/logout';
    }
}

/* =========================================
   HÀM TOAST THÔNG BÁO DÙNG CHUNG
   ========================================= */
window.toast = function(message, type = 'success') {
    let toastHost = document.getElementById('toastHost');
    if (!toastHost || toastHost.parentElement !== document.body) {
        if (toastHost) toastHost.remove();
        toastHost = document.createElement('div');
        toastHost.id = 'toastHost';
        toastHost.className = 'toast-container';
        document.body.appendChild(toastHost);
    }
    toastHost.innerHTML = '';
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

window.confirmDialog = function(title, message, onConfirmCallback) {
    let overlay = document.getElementById('genericConfirmOverlay');
    if (!overlay || overlay.parentElement !== document.body) {
        if (overlay) overlay.remove();
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

    btnOk.onclick = function() {
        overlay.style.display = 'none';
        if (typeof onConfirmCallback === 'function') onConfirmCallback();
    };
    btnCancel.onclick = function() { overlay.style.display = 'none'; };
};

/* =========================================
   HỆ THỐNG THÔNG BÁO MỚI (ORDERS & REVIEWS)
   ========================================= */
const NotifApp = {
    lastOrderId: localStorage.getItem('admin_last_order_id') || 0,
    lastReviewId: localStorage.getItem('admin_last_review_id') || 0,
    hasNewOrders: false,
    hasNewReviews: false,
    justReceivedRealtime: false,

    init: function() {
        this.bindDropdownEvent();
        this.fetchAllData();
        this.connectWebSocket();
    },

    connectWebSocket: function() {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.error("Thư viện SockJS hoặc StompJS chưa được tải!");
            return;
        }

        const socket = new SockJS('/ws');
        const stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, (frame) => {
            console.log('Đã kết nối WebSockets');
            stompClient.subscribe('/topic/admin/notifications', (message) => {
                if (message.body === "NEW_NOTIF") {
                    console.log("Có thông báo mới cho Admin");
                    this.justReceivedRealtime = true;
                    this.fetchAllData();
                }
            });
        }, (error) => {
            console.warn("Mất kết nối WebSockets, đang thử lại sau 5s", error);
            setTimeout(() => this.connectWebSocket(), 5000);
        });
    },

    bindDropdownEvent: function() {
        const btn = document.querySelector('button[onclick="handleNotifClick()"]');
        const dropdown = document.getElementById('notif-dropdown');

        if(btn) {
            btn.removeAttribute('onclick');
            btn.onclick = (e) => {
                e.stopPropagation();
                dropdown.classList.toggle('show');
                this.updateBadgeDisplay(false);

                if (this.hasNewOrders) {
                    localStorage.setItem('admin_last_order_id', this.lastOrderId);
                    this.hasNewOrders = false;
                    document.querySelectorAll('#notif-orders-content .notif-item.unread').forEach(item => item.classList.remove('unread'));
                }
                if (this.hasNewReviews) {
                    localStorage.setItem('admin_last_review_id', this.lastReviewId);
                    this.hasNewReviews = false;
                    document.querySelectorAll('#notif-reviews-content .notif-item.unread').forEach(item => item.classList.remove('unread'));
                }
            };
        }
    },

    switchTab: function(tabName) {
        document.querySelectorAll('.notif-tab').forEach(t => t.classList.remove('active'));
        document.getElementById('notif-orders-content').style.display = 'none';
        document.getElementById('notif-reviews-content').style.display = 'none';

        if (tabName === 'orders') {
            document.getElementById('tab-orders').classList.add('active');
            document.getElementById('notif-orders-content').style.display = 'block';
        } else if (tabName === 'reviews') {
            document.getElementById('tab-reviews').classList.add('active');
            document.getElementById('notif-reviews-content').style.display = 'block';
        }
    },

    fetchAllData: function() {
        this.fetchOrderNotifications();
        this.fetchReviewNotifications();
    },

    updateBadgeDisplay: function(forceShow = null) {
        const badge = document.getElementById('notif-badge');
        if (!badge) return;
        if (forceShow !== null) {
            badge.style.display = forceShow ? 'block' : 'none';
        } else {
            if (this.hasNewOrders || this.hasNewReviews) {
                badge.style.display = 'block';
            }
        }
    },

    fetchOrderNotifications: async function() {
        try {
            const res = await fetch('/api/hoa-don/thong-bao-moi');
            if (!res.ok) return;
            const data = await res.json();
            this.renderOrdersUI(data);
        } catch (error) { console.error("Lỗi lấy thông báo đơn hàng:", error); }
    },

    renderOrdersUI: function(orders) {
        const content = document.getElementById('notif-orders-content');
        if(!content) return;

        if (!orders || orders.length === 0) {
            this.hasNewOrders = false;
            content.innerHTML = `
                <div class="empty-notif">
                    <i class="bi bi-receipt" style="font-size:24px; display:block; margin-bottom:10px;"></i>
                    <span>Không có đơn hàng mới chờ xác nhận</span>
                </div>`;
            return;
        }

        const newestOrderId = orders[0].id;
        if (newestOrderId > parseInt(this.lastOrderId)) {
            this.hasNewOrders = true;
            this.updateBadgeDisplay();
            if (this.justReceivedRealtime) {
                this.playDingSound();
                if(window.toast) window.toast(`Có đơn hàng online mới: #${orders[0].ma}`, "success");
                this.justReceivedRealtime = false;
            }
            this.lastOrderId = newestOrderId;
        }

        let html = '<div class="notif-list">';
        orders.forEach(ord => {
            const total = new Intl.NumberFormat('vi-VN').format(ord.tongTien || 0) + ' đ';
            const timeAgo = this.timeSince(ord.ngayTao);
            const isUnread = ord.id > parseInt(localStorage.getItem('admin_last_order_id') || 0) ? 'unread' : '';

            html += `
                <div class="notif-item ${isUnread}" onclick="NotifApp.goToOrder()">
                    <div class="notif-icon order"><i class="bi bi-bag-check-fill"></i></div>
                    <div class="notif-content">
                        <div class="notif-title">Đơn hàng mới: #${ord.ma}</div>
                        <div class="notif-desc">Khách hàng <b>${ord.khachHang}</b> vừa đặt hàng với tổng giá trị <b>${total}</b>.</div>
                        <div class="notif-time"><i class="bi bi-clock"></i> ${timeAgo}</div>
                    </div>
                </div>`;
        });
        html += '</div>';
        html += `<div style="text-align:center; padding:12px; border-top:1px solid var(--border); font-size:12px; font-weight:600; cursor:pointer; color:var(--primary); background:var(--panel-hover);" onclick="NotifApp.goToOrder()">XEM TẤT CẢ HÓA ĐƠN <i class="bi bi-arrow-right"></i></div>`;
        content.innerHTML = html;
    },

    fetchReviewNotifications: async function() {
        try {
            const res = await fetch('/api/khach-hang/danh-gia/thong-bao-moi');
            if (!res.ok) return;
            const data = await res.json();
            this.renderReviewsUI(data);
        } catch (error) {
            const content = document.getElementById('notif-reviews-content');
            if (content && content.innerHTML.includes("Đang tải")) {
                content.innerHTML = `
                    <div class="empty-notif">
                        <i class="bi bi-star" style="font-size:24px; display:block; margin-bottom:10px;"></i>
                        <span>Chưa có dữ liệu đánh giá</span>
                    </div>`;
            }
        }
    },

    renderReviewsUI: function(reviews) {
        const content = document.getElementById('notif-reviews-content');
        if(!content) return;

        if (!reviews || reviews.length === 0) {
            this.hasNewReviews = false;
            content.innerHTML = `
                <div class="empty-notif">
                    <i class="bi bi-star" style="font-size:24px; display:block; margin-bottom:10px;"></i>
                    <span>Không có đánh giá mới nào</span>
                </div>`;
            return;
        }

        const newestReviewId = reviews[0].id;
        if (newestReviewId > parseInt(this.lastReviewId)) {
            this.hasNewReviews = true;
            this.updateBadgeDisplay();
            if (this.justReceivedRealtime) {
                this.playDingSound();
                if(window.toast) window.toast(`Có đánh giá mới từ khách hàng!`, "info");
                this.justReceivedRealtime = false;
            }
            this.lastReviewId = newestReviewId;
        }

        let html = '<div class="notif-list">';
        reviews.forEach(rev => {
            const timeAgo = this.timeSince(rev.ngayTao);
            let starsHtml = '';
            for(let i=0; i<5; i++) {
                if(i < (rev.soSao || 5)) starsHtml += '<i class="bi bi-star-fill" style="color: #ca8a04;"></i>';
                else starsHtml += '<i class="bi bi-star" style="color: #cbd5e1;"></i>';
            }
            const isUnread = rev.id > parseInt(localStorage.getItem('admin_last_review_id') || 0) ? 'unread' : '';

            html += `
                <div class="notif-item ${isUnread}" onclick="NotifApp.goToReviews()">
                    <div class="notif-icon review"><i class="bi bi-star-fill"></i></div>
                    <div class="notif-content">
                        <div class="notif-title">${rev.khachHang || 'Khách hàng'} vừa đánh giá</div>
                        <div class="notif-desc">
                            <div style="margin-bottom: 2px;">${starsHtml}</div>
                            <i>"${rev.noiDung || 'Đã để lại một đánh giá.'}"</i>
                        </div>
                        <div class="notif-time"><i class="bi bi-clock"></i> ${timeAgo}</div>
                    </div>
                </div>`;
        });
        html += '</div>';
        html += `<div style="text-align:center; padding:12px; border-top:1px solid var(--border); font-size:12px; font-weight:600; cursor:pointer; color:var(--primary); background:var(--panel-hover);" onclick="NotifApp.goToReviews()">ĐI TỚI TRANG ĐÁNH GIÁ <i class="bi bi-arrow-right"></i></div>`;
        content.innerHTML = html;
    },

    goToOrder: function() {
        document.getElementById('notif-dropdown').classList.remove('show');
        window.location.hash = 'orders';
    },

    goToReviews: function() {
        document.getElementById('notif-dropdown').classList.remove('show');
        window.location.hash = 'reviews';
    },

    playDingSound: function() {
        try {
            const audio = new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3');
            audio.volume = 0.3;
            audio.play().catch(e => console.log("Browser chặn âm thanh tự động"));
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

// Khởi chạy hệ thống sau khi load DOM
document.addEventListener("DOMContentLoaded", () => {
    startClock();
    NotifApp.init();

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