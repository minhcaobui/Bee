/* =======================================================
 * File: admin-router.js
 * Chức năng: Điều hướng, Tải module và Kích hoạt Script
 * =======================================================
 */

// ==========================================
// 1. GLOBAL HELPERS
// ==========================================
const $ = (s) => document.querySelector(s);
const $$ = (s) => document.querySelectorAll(s);

// Toast xịn (Hiển thị thông báo)
function showToast(message, type = 'success') {
    const host = document.getElementById('toastHost');
    if (!host) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    // Icon tương ứng
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

// Gán vào window để các file con có thể gọi window.toast(...)
window.toast = showToast;


// ==========================================
// 2. LOGIC ROUTER (QUAN TRỌNG NHẤT)
// ==========================================
async function loadModule(moduleName) {
    const contentArea = document.getElementById('content-area');
    const pageTitle = document.getElementById('pageTitle');

    // 1. Hiện loading khi đang chuyển trang
    window.showLoading();

    // 2. Map tên module -> Đường dẫn file HTML và Tiêu đề
    // LƯU Ý: Kiểm tra kỹ đường dẫn file thực tế của bạn (có .html hay không)
    let url = '';
    let title = '';

    switch (moduleName) {
        case 'dashboard':
            url = '/dashboards';
            title = 'DASHBOARD';
            break;

        case 'pos':
            url = '/pos'; // Hoặc '/pos.html' tùy server bạn config
            title = 'BÁN HÀNG TẠI QUẦY';
            break;

        case 'catalogs':
            url = '/catalogs';
            title = 'QUẢN LÝ THUỘC TÍNH';
            break;

        case 'products':
            url = '/products'; // Code cũ bạn để có .html
            title = 'QUẢN LÝ SẢN PHẨM';
            break;

        case 'promotions':
            url = '/promotions';
            title = 'QUẢN LÝ KHUYẾN MÃI';
            break;

        case 'orders':
            url = '/orders.html';
            title = 'QUẢN LÝ ĐƠN HÀNG';
            break;

        case 'customers':
            url = '/customers';
            title = 'QUẢN LÝ KHÁCH HÀNG';
            break;

        case 'users':
            url = '/users.html';
            title = 'QUẢN LÝ TÀI KHOẢN';
            break;

        default:
            window.hideLoading(); // Tắt mèo nếu không tìm thấy module
            return;
    }

    if (pageTitle) pageTitle.textContent = title;

    try {

        const res = await fetch(url);
        if (!res.ok) throw new Error(`Lỗi ${res.status}`);

        const html = await res.text();
        contentArea.innerHTML = html;

        executeScripts(contentArea, moduleName);

        // Active menu
        document.querySelectorAll('.sidebar-item').forEach(item => {
            item.classList.remove('active');
            if (item.getAttribute('href') === '#' + moduleName) item.classList.add('active');
        });

    } catch (err) {
        console.error(err);
        contentArea.innerHTML = `<h3 style="color:red; text-align:center; margin-top:50px;">LỖI TẢI TRANG: ${err.message}</h3>`;
    } finally {
        // --- QUAN TRỌNG NHẤT ---
        // Dù tải thành công hay thất bại, cũng phải CẤT CON MÈO ĐI
        window.hideLoading();
    }
}

// ==========================================
// 3. XỬ LÝ SCRIPT & INIT (LOGIC ENGINE)
// ==========================================
function executeScripts(container, moduleName) {
    const scripts = container.querySelectorAll('script');
    console.log(`🔍 Tìm thấy ${scripts.length} thẻ script trong module ${moduleName}`);

    Array.from(scripts).forEach(oldScript => {
        // Trường hợp 1: Script thư viện ngoài (có src)
        if (oldScript.src) {
            const newScript = document.createElement('script');
            Array.from(oldScript.attributes).forEach(attr => newScript.setAttribute(attr.name, attr.value));
            document.body.appendChild(newScript);
        }
        // Trường hợp 2: Script nội tuyến (logic của module)
        else {
            try {
                // Dùng eval để ép trình duyệt chạy code ngay lập tức
                eval(oldScript.innerHTML);
                console.log("✅ Đã chạy script nội tuyến thành công");
            } catch (e) {
                console.error("❌ Lỗi cú pháp trong script của module:", e);
                window.toast(`Lỗi Script: ${e.message}`, 'error');
            }
        }
        oldScript.remove(); // Dọn dẹp thẻ script cũ
    });

    // KÍCH HOẠT HÀM KHỞI TẠO (INIT) CỦA TỪNG MODULE
    setTimeout(() => {
        console.log(`🚀 Đang kích hoạt init cho module: ${moduleName}`);

        if (moduleName === 'catalogs') {
            if (typeof window.initCatalogs !== 'undefined') {
                window.initCatalogs();
            } else {
                console.warn("Chưa tìm thấy CatalogApp");
            }
        }
        else if (moduleName === 'products') {
            if (typeof window.initProducts !== 'undefined') {
                window.initProducts();
            } else {
                console.warn("⚠️ Không tìm thấy hàm window.initProducts");
            }
        }
        else if (moduleName === 'promotions') {
            if (typeof window.PromotionApp !== 'undefined') {
                window.PromotionApp.init();
            } else {
                console.warn("Chưa tìm thấy PromotionApp");
            }
        }
        else if (moduleName === 'pos') {
            if (typeof window.PosApp !== 'undefined') {
                window.PosApp.init();
            } else {
                console.warn("Chưa tìm thấy PosApp");
            }
        }
        else if (moduleName === 'customers') {
            if (typeof window.CustomerApp !== 'undefined') {
                window.CustomerApp.init();
            } else {
                console.warn("Chưa tìm thấy CustomerApp");
            }
        }
    }, 100);
}

// ==========================================
// 4. KHỞI TẠO ROUTER
// ==========================================
function initRouter() {
    // Lấy phần sau dấu # (ví dụ: products, orders...)
    const hash = window.location.hash.slice(1);

    // LOGIC SỬA ĐỔI:
    // Chỉ khi nào CÓ hash (tức là người dùng đã bấm menu) thì mới tải module.
    // Nếu KHÔNG có hash (vừa mới vào trang), thì giữ nguyên HTML gốc (cái bảng Xin Chào).
    if (hash) {
        loadModule(hash);
    }
}

// Lắng nghe sự kiện đổi URL (#)
window.addEventListener('hashchange', () => {
    const hash = window.location.hash.slice(1);
    loadModule(hash);
});

// Chạy lần đầu khi mở web
document.addEventListener('DOMContentLoaded', initRouter);