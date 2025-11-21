/* =======================================================
 * File: admin-router.js (CORE: Global Helpers & Router Logic)
 * Chú ý: File này định nghĩa các hàm Global: $, toast, request, uiConfirm.
 * =======================================================
 */

// 1. GLOBAL HELPERS (Được sử dụng bởi tất cả module con)
const $ = (s) => document.querySelector(s);

// Toast
function showToast(message, type = 'info', title){
    const host = $('#toastHost');
    if (!host) { console.error('Toast host element not found!'); return; }
    const box = document.createElement('div');
    box.className = `toast ${type}`;
    box.innerHTML = `<div class="title">${title || (type==='success'?'Thành công': type==='error'?'Lỗi':'Thông báo')}</div><div class="msg">${message}</div>`;
    host.appendChild(box);
    requestAnimationFrame(()=> box.classList.add('show'));
    const timer = setTimeout(close, 2600);
    function close(){ box.classList.remove('show'); setTimeout(()=> box.remove(), 200); clearTimeout(timer); }
    box.addEventListener('click', close);
}
const toast = (m,t='info',title)=>showToast(m,t,title);

// Confirm (Giả định HTML Confirm Dialog đã có trong admin-layout.html)
function uiConfirm(message){
    return new Promise((resolve)=>{
        const ov = $('#confirmOverlay');
        if (!ov) { resolve(window.confirm(message)); return; } // Fallback

        $('#confirmMsg').textContent = message;
        ov.style.display = 'flex';
        const ok = ()=>{ cleanup(); resolve(true); };
        const cancel = ()=>{ cleanup(); resolve(false); };
        function cleanup(){
            ov.style.display = 'none';
            // Dùng removeEventListener để tránh lỗi
            document.removeEventListener('keydown', onKey);
        }
        $('#confirmOk').onclick = ok;
        $('#confirmCancel').onclick = cancel;
        function onKey(e){ if(e.key==='Escape'){ cancel(); } }
        document.addEventListener('keydown', onKey);
    });
}

// Fetch Request
async function request(url, opts={}){
    const res = await fetch(url, { headers: { 'Content-Type':'application/json', 'Accept':'application/json' }, ...opts });
    if(!res.ok){ const text = await res.text(); throw new Error(text || res.statusText); }
    if(res.status === 204) return null;
    const ct = res.headers.get('content-type') || '';
    return ct.includes('application/json') ? res.json() : res.text();
}

// Router Helpers
function parseHash(){
    const p = new URLSearchParams(location.hash.replace(/^#/, ''));
    // Giả định các tab con (như variants) sẽ dùng #tab=products&id=1
    return { tab: p.get('tab') || null, id: p.get('id') };
}

function setHash(nextModule, id){
    const h = `#${nextModule}${id?`&id=${id}`:''}`;
    if(location.hash !== h) location.hash = h; else updateLayout(h);
}


// === CORE ROUTER LOGIC (Load Content) ===

/**
 * Hàm tải nội dung HTML của module và chèn vào khu vực chính.
 */
/* ================= SỬA LẠI HÀM LOAD ĐỂ CHỜ SCRIPT TẢI XONG ================= */
async function loadModuleContent(moduleName) {
    const contentArea = $('#content-area');
    contentArea.innerHTML = '<div style="text-align:center; padding:50px;"><div class="spin"></div> Đang tải nội dung...</div>';

    if (moduleName === 'pos') {
        contentArea.innerHTML = '<div class="card" style="padding:30px; color:var(--red);">Màn hình POS là trang Fullscreen.</div>';
        return;
    }

    // MAP MODULE NAME -> FILE URL
    let url = "";
    switch(moduleName) {
        case 'dashboard': url = `/dashboard`; break;
        case 'catalogs':  url = `/catalogs`;  break;
        case 'products':  url = `/products`;  break;
        case 'orders':    url = `/orders`;    break;
        case 'promo':     url = `/promo`;     break;
        case 'reports':   url = `/reports`;   break;
        case 'users':     url = `/users`;     break;
        default:          url = `/${moduleName}`; break;
    }

    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`Lỗi tải module ${url}`);

        const htmlText = await response.text();
        contentArea.innerHTML = htmlText; // 1. Nhúng HTML vào trước

        // 2. TÌM VÀ CHẠY LẠI SCRIPT
        const scripts = contentArea.querySelectorAll('script');

        scripts.forEach(oldScript => {
            const newScript = document.createElement('script');

            // Copy attributes (src, type, etc)
            Array.from(oldScript.attributes).forEach(attr => newScript.setAttribute(attr.name, attr.value));

            // Nếu là script nội tuyến (inline)
            if (oldScript.innerHTML) {
                newScript.innerHTML = oldScript.innerHTML;
                document.body.appendChild(newScript); // Chạy ngay
            }
            // Nếu là script ngoại tuyến (src="...") -> QUAN TRỌNG
            else if (oldScript.src) {
                newScript.src = oldScript.src;

                // --- FIX LỖI: CHỜ TẢI XONG MỚI GỌI INIT ---
                newScript.onload = () => {
                    // Map tên module sang hàm Init tương ứng
                    if (moduleName === 'catalogs' && window.initCatalogModule) {
                        window.initCatalogModule();
                    } else if (moduleName === 'products' && window.initProductModule) {
                        window.initProductModule();
                    }
                };

                document.body.appendChild(newScript);
            }

            oldScript.remove(); // Xóa script cũ
        });

    } catch (error) {
        contentArea.innerHTML = `<div style="padding:30px; color:var(--red);">Lỗi: ${error.message}</div>`;
    }
}

// === ROUTER EXECUTION ===

function updateLayout(hash) {
    // Nếu hash là #products&id=1, thì currentModule = products
    const currentModule = hash.replace('#', '').split('&')[0];

    const moduleNameMap = { 'dashboard': 'Dashboard', 'catalogs': 'Quản lý Danh mục', 'products': 'Quản lý Sản phẩm', 'orders': 'Quản lý Đơn hàng', 'promo': 'Quản lý Giảm giá', 'reports': 'Báo cáo', 'users': 'Quản lý Tài khoản' };
    $('#pageTitle').textContent = moduleNameMap[currentModule] || 'Quản trị Hệ thống';

    document.querySelectorAll('.sidebar-item').forEach(item => {
        item.classList.remove('active');
        // Chỉ so sánh phần module chính (#catalogs)
        if (item.getAttribute('href') === '#' + currentModule) {
            item.classList.add('active');
        }
    });

    // Nếu đang ở tab con (như #products&tab=variants) thì vẫn tải module cha
    if (currentModule) {
        loadModuleContent(currentModule);
    }
}

// Khởi tạo
window.addEventListener('hashchange', () => updateLayout(location.hash));

document.addEventListener('DOMContentLoaded', () => {
    // Lần tải đầu tiên
    const hash = location.hash;
    if (!hash || hash === '#') {
        updateLayout('#dashboard');
    } else {
        updateLayout(hash);
    }
});