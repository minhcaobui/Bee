/* =======================================================
 * File: admin-router.js
 * Chức năng: Điều hướng, Tải module và Kích hoạt Script
 * =======================================================
 */

// 1. GLOBAL HELPERS (Giữ nguyên để file con dùng)
const $ = (s) => document.querySelector(s);
const $$ = (s) => document.querySelectorAll(s);

// Toast xịn (Kết nối với giao diện mới)
function showToast(message, type = 'success') {
    const host = document.getElementById('toastHost');
    if (!host) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    // SỬA ĐOẠN NÀY: Bỏ div class="title" đi, chỉ để lại msg
    // Thêm icon cho sinh động nếu thích
    const icon = type === 'success' ? '<i class="bi bi-check-circle-fill"></i>' : '<i class="bi bi-exclamation-triangle-fill"></i>';

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

// Gán vào window để file con gọi được
window.toast = showToast;


// 2. LOGIC ROUTER (QUAN TRỌNG NHẤT)
async function loadModule(moduleName) {
    const contentArea = document.getElementById('content-area');
    const pageTitle = document.getElementById('pageTitle');

    // 1. Hiện loading
    contentArea.innerHTML = `
        <div style="text-align:center; padding:50px;">
            <div style="font-size:20px; font-weight:700;">ĐANG TẢI DỮ LIỆU...</div>
        </div>`;

    // 2. Map tên module -> Đường dẫn file HTML
    // LƯU Ý: Nếu file mày nằm trong thư mục con thì sửa lại đường dẫn ở đây
    let url = '';
    let title = '';

    switch (moduleName) {
        case 'dashboard':
            url = '/dashboard.html';
            title = 'DASHBOARD';
            break;

        case 'pos':
            url = '/pos';
            title = 'BÁN HÀNG';
            break;
        case 'catalogs':
            url = '/catalogs';
            title = 'QUẢN LÝ THUỘC TÍNH';
            break;
        case 'products':
            url = '/products.html';
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
        case 'users':
            url = '/users.html';
            title = 'QUẢN LÝ TÀI KHOẢN';
            break;
        default:
            contentArea.innerHTML = `<div style="padding:40px; text-align:center;">Module không tồn tại</div>`;
            return;
    }

    // Cập nhật tiêu đề trang
    if (pageTitle) pageTitle.textContent = title;

    try {
        // 3. Tải file HTML
        const res = await fetch(url);
        if (!res.ok) throw new Error(`Không tìm thấy file ${url} (Lỗi ${res.status})`);

        const html = await res.text();

        // 4. Nhúng HTML vào trang
        contentArea.innerHTML = html;

        // 5. CHẠY SCRIPT CỦA MODULE (Đoạn này fix lỗi "không bấm được")
        executeScripts(contentArea, moduleName);

        // Cập nhật Sidebar Active
        document.querySelectorAll('.sidebar-item').forEach(item => {
            item.classList.remove('active');
            if (item.getAttribute('href') === '#' + moduleName) {
                item.classList.add('active');
            }
        });

    } catch (err) {
        console.error(err);
        contentArea.innerHTML = `
            <div style="padding:40px; text-align:center; color:red;">
                <h3>LỖI TẢI TRANG</h3>
                <p>${err.message}</p>
                <p>Kiểm tra lại xem file HTML có đúng vị trí không.</p>
            </div>`;
    }
}

// Hàm xử lý Script tách riêng cho gọn
function executeScripts(container, moduleName) {
    const scripts = container.querySelectorAll('script');
    console.log(`🔍 Tìm thấy ${scripts.length} thẻ script trong module ${moduleName}`);

    Array.from(scripts).forEach(oldScript => {
        // 1. Nếu là script có src (ví dụ thư viện ngoài)
        if (oldScript.src) {
            const newScript = document.createElement('script');
            Array.from(oldScript.attributes).forEach(attr => newScript.setAttribute(attr.name, attr.value));
            document.body.appendChild(newScript);
        }
        // 2. Nếu là script nội tuyến (code logic của module)
        else {
            try {
                // Dùng eval để chạy ngay lập tức code trong thẻ script
                // Đây là cách "bàn tay sắt" để ép code chạy
                eval(oldScript.innerHTML);
                console.log("Đã chạy script nội tuyến thành công");
            } catch (e) {
                console.error("Lỗi cú pháp trong script của module:", e);
                // Hiển thị lỗi lên màn hình cho dễ sửa
                window.toast(`Lỗi Script: ${e.message}`, 'error');
            }
        }
        oldScript.remove(); // Dọn dẹp
    });

    // 3. GỌI HÀM INIT (Quan trọng nhất)
    setTimeout(() => {
        console.log(`Đang kích hoạt init cho: ${moduleName}`);

        if (moduleName === 'catalogs') {
            if (typeof window.initCatalogs === 'function') {
                try {
                    window.initCatalogs();
                } catch (err) {
                    console.error(" Lỗi khi chạy initCatalogs:", err);
                }
            } else {
                console.warn("Không tìm thấy hàm window.initCatalogs");
            }
        } else if (moduleName === 'products' && typeof window.initProducts === 'function') {
            window.initProducts();
        } else if (moduleName === 'promotions') {
            if (typeof window.PromotionApp !== 'undefined') {
                window.PromotionApp.init(); // Gọi hàm init của file promotions.html
            } else {
                console.warn("Chưa tìm thấy PromotionApp");
            }
        } else if (moduleName === 'pos') {
            if (typeof window.PosApp !== 'undefined') {
                window.PosApp.init();
            } else {
                console.warn("Chưa tìm thấy PosApp");
            }
        }
    }, 100);
}

// 3. KHỞI TẠO ROUTER
function initRouter() {
    const hash = window.location.hash.slice(1) || 'dashboard'; // Mặc định vào dashboard
    loadModule(hash);
}

// Lắng nghe sự kiện đổi URL (#)
window.addEventListener('hashchange', () => {
    const hash = window.location.hash.slice(1);
    loadModule(hash);
});

// Chạy lần đầu khi mở web
document.addEventListener('DOMContentLoaded', initRouter);