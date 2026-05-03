const $ = (s) => document.querySelector(s);
const $$ = (s) => document.querySelectorAll(s);

// ======================================================================
// HỆ THỐNG QUẢN LÝ VÒNG LẶP (Tránh rò rỉ bộ nhớ & lỗi spam console)
// Tự động ghi nhận mọi setInterval được gọi trong các module
// ======================================================================
window.activeIntervals = [];
const originalSetInterval = window.setInterval;
window.setInterval = function(fn, delay) {
    const id = originalSetInterval(fn, delay);
    window.activeIntervals.push(id);
    return id;
};
// ======================================================================

function showToast(message, type = 'success') {
    const host = document.getElementById('toastHost');
    if (!host) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    const icon = type === 'success'
        ? '<i class="bi bi-check-circle-fill"></i>'
        : '<i class="bi bi-exclamation-triangle-fill"></i>';
    toast.innerHTML = `
        <div class="msg" style="display:flex; align-items:center; gap:10px; font-weight:700;">
            ${icon} <span>${message}</span>
        </div>
    `;
    host.appendChild(toast);
    requestAnimationFrame(() => toast.classList.add('show'));
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

window.toast = showToast;

// ======================================================================
// HÀM MỚI: QUÉT DỌN CÁC MODAL BỊ KẸT TRÊN BODY TRƯỚC KHI CHUYỂN TRANG
// ======================================================================
function cleanupGarbage() {
    // 1. Dọn dẹp tất cả các vòng lặp đếm ngược (setInterval) đang chạy ngầm
    if (window.activeIntervals && window.activeIntervals.length > 0) {
        window.activeIntervals.forEach(id => clearInterval(id));
        window.activeIntervals = [];
    }

    // 2. CHỈ XÓA ĐÚNG CÁI OTP MODAL CỦA TRANG PROFILE
    // Tuyệt đối tha mạng cho genericConfirmOverlay của hệ thống!
    const garbageIds = ['otpModal'];

    garbageIds.forEach(id => {
        const el = document.getElementById(id);
        if (el && el.parentElement === document.body) {
            el.remove();
        }
    });

    // 3. Dọn dẹp Chart.js instances
    if (window.DashboardApp) {
        if (window.DashboardApp.chartDoanhThuInstance) window.DashboardApp.chartDoanhThuInstance.dispose();
        if (window.DashboardApp.chartTiLeInstance) window.DashboardApp.chartTiLeInstance.dispose();
        window.DashboardApp.chartDoanhThuInstance = null;
        window.DashboardApp.chartTiLeInstance = null;
    }
}
// ======================================================================

async function loadModule(moduleName) {
    const contentArea = document.getElementById('content-area');
    const pageTitle = document.getElementById('pageTitle');

    // Quét dọn trước khi tải trang mới
    cleanupGarbage();

    window.showLoading();
    let url = '';
    let title = '';
    switch (moduleName) {
        case 'dashboard':
            url = '/dashboards';
            title = 'DASHBOARD';
            break;

        case 'pos':
            url = '/pos';
            title = 'BÁN HÀNG TẠI QUẦY';
            break;

        case 'orders':
            url = '/orders';
            title = 'QUẢN LÝ HÓA ĐƠN';
            break;

        case 'catalogs':
            url = '/catalogs';
            title = 'QUẢN LÝ THUỘC TÍNH';
            break;

        case 'products':
            url = '/products';
            title = 'QUẢN LÝ SẢN PHẨM';
            break;

        case 'promotions':
            url = '/promotions';
            title = 'QUẢN LÝ KHUYẾN MÃI';
            break;

        case 'customers':
            url = '/customers';
            title = 'QUẢN LÝ KHÁCH HÀNG';
            break;

        case 'staff':
            url = '/staff';
            title = 'QUẢN LÝ NHÂN VIÊN';
            break;

        case 'profiles':
            url = '/profiles';
            title = 'QUẢN LÝ TÀI KHOẢN';
            break;

        case 'reviews':
            url = '/reviews';
            title = 'QUẢN LÝ ĐÁNH GIÁ';
            break;

        default:
            window.hideLoading();
            return;
    }
    if (pageTitle) pageTitle.textContent = title;
    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error(`Lỗi ${res.status}`);
        const html = await res.text();
        contentArea.innerHTML = html;
        executeScripts(contentArea, moduleName);
        document.querySelectorAll('.sidebar-item').forEach(item => {
            item.classList.remove('active');
            if (item.getAttribute('href') === '#' + moduleName) item.classList.add('active');
        });
    } catch (err) {
        console.error(err);
        contentArea.innerHTML = `<h3 style="color:red; text-align:center; margin-top:50px;">LỖI TẢI TRANG: ${err.message}</h3>`;
    } finally {
        window.hideLoading();
    }
}

function executeScripts(container, moduleName) {
    const scripts = container.querySelectorAll('script');
    Array.from(scripts).forEach(oldScript => {
        if (oldScript.src) {
            const newScript = document.createElement('script');
            Array.from(oldScript.attributes).forEach(attr => newScript.setAttribute(attr.name, attr.value));
            document.body.appendChild(newScript);
        } else {
            try {
                eval(oldScript.innerHTML);
            } catch (e) {
                console.error(e);
                window.toast(`${e.message}`, 'error');
            }
        }
        oldScript.remove();
    });

    setTimeout(() => {
        if (moduleName === 'catalogs') {
            if (typeof window.initCatalogs !== 'undefined') window.initCatalogs();
        }
        else if (moduleName === 'orders') {
            if (typeof window.OrderApp !== 'undefined') window.OrderApp.init();
        }
        else if (moduleName === 'products') {
            if (typeof window.initProducts !== 'undefined') window.initProducts();
        }
        else if (moduleName === 'promotions') {
            if (typeof window.PromotionApp !== 'undefined') window.PromotionApp.init();
        }
        else if (moduleName === 'pos') {
            if (typeof window.PosApp !== 'undefined') window.PosApp.init();
        }
        else if (moduleName === 'customers') {
            if (typeof window.CustomerApp !== 'undefined') window.CustomerApp.init();
        }
        else if (moduleName === 'profiles') {
            if (typeof window.ProfileApp !== 'undefined') window.ProfileApp.init();
        }
        else if (moduleName === 'dashboard') {
            if (typeof window.DashboardApp !== 'undefined') window.DashboardApp.init(); // GỌI ĐÚNG DashboardApp
        }
        else if (moduleName === 'staff') {
            if (typeof window.StaffApp !== 'undefined') window.StaffApp.init();
        }
        else if (moduleName === 'reviews') {
            if (typeof window.ReviewApp !== 'undefined') window.ReviewApp.init();
        }
    }, 100);
}

function initRouter() {
    const hash = window.location.hash.slice(1);
    if (hash) {
        loadModule(hash);
    }
}

window.addEventListener('hashchange', () => {
    const hash = window.location.hash.slice(1);
    loadModule(hash);
});
document.addEventListener('DOMContentLoaded', initRouter);