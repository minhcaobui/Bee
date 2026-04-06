/* =======================================================
 * File: customer-router.js
 * Chức năng: Điều hướng, Tải module cho trang Customer
 * Pattern: Giống hệt admin-router.js (hash-based SPA)
 * =======================================================
 */

// ==========================================
// 1. GLOBAL HELPERS
// ==========================================
window.toast = function(message, type = 'success') {
    const host = document.getElementById('toastHost');
    if (!host) return;
    const icons = {
        success: '<i class="fa-solid fa-circle-check"></i>',
        error:   '<i class="fa-solid fa-triangle-exclamation"></i>',
        warning: '<i class="fa-solid fa-circle-exclamation"></i>',
        info:    '<i class="fa-solid fa-circle-info"></i>',
    };
    const el = document.createElement('div');
    el.className = `bee-toast ${type}`;
    el.innerHTML = `${icons[type] || icons.info} <span>${message}</span>`;
    host.appendChild(el);
    requestAnimationFrame(() => el.classList.add('show'));
    setTimeout(() => {
        el.classList.remove('show');
        setTimeout(() => el.remove(), 400);
    }, 3000);
};

// ==========================================
// 2. LOADER
// ==========================================
window.showLoading = function() {
    document.getElementById('globalLoader')?.classList.add('show');
};
window.hideLoading = function() {
    setTimeout(() => document.getElementById('globalLoader')?.classList.remove('show'), 250);
};

// ==========================================
// 3. ROUTER LOGIC
// ==========================================
async function loadModule(moduleName) {
    const contentArea = document.getElementById('content-area');
    if (!contentArea) return;

    window.showLoading();

    // Map module → URL controller (giống switch-case bên admin)
    const moduleMap = {
        'home':     { url: '/customer/home',     title: 'Trang chủ' },
        'shop':     { url: '/customer/shop',     title: 'Sản phẩm' },
        'detail':   { url: '/customer/detail',   title: 'Chi tiết sản phẩm' },
        'cart':     { url: '/customer/cart',     title: 'Giỏ hàng' },
        'checkout': { url: '/customer/checkout', title: 'Thanh toán' },
        'order':   { url: '/customer/order',   title: 'Đơn hàng của tôi' },
        'account':  { url: '/customer/account',  title: 'Tài khoản' },
        'about':  { url: '/customer/about',  title: 'About Beemate' },
        'collection':  { url: '/customer/collection',  title: 'Bộ sưu tập' },
        'sale':  { url: '/customer/sale',  title: 'Sale' },
    };

    const module = moduleMap[moduleName];
    if (!module) {
        window.hideLoading();
        console.warn(`[Router] Module "${moduleName}" không tồn tại`);
        return;
    }

    document.title = `BEEMATE | ${module.title}`;

    // Active nav link
    document.querySelectorAll('.nav-links a[data-module]').forEach(link => {
        link.classList.toggle('active', link.dataset.module === moduleName);
    });

    try {
        // Lấy query string nếu có vd: #detail?id=5
        const queryStr = window.location.hash.includes('?')
            ? window.location.hash.split('?')[1] : '';
        const fetchUrl = queryStr ? `${module.url}?${queryStr}` : module.url;

        const res = await fetch(fetchUrl);
        if (!res.ok) throw new Error(`Lỗi ${res.status}: ${res.statusText}`);

        const html = await res.text();

        // Fade out → inject → fade in
        contentArea.style.opacity = '0';
        contentArea.style.transition = 'opacity 0.2s ease';

        setTimeout(() => {
            contentArea.innerHTML = html;
            executeScripts(contentArea, moduleName);
            contentArea.style.opacity = '1';
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }, 180);

    } catch (err) {
        console.error('[Router] Lỗi tải module:', err);
        contentArea.innerHTML = `
            <div style="text-align:center;padding:100px 20px;font-family:'DM Sans',sans-serif;">
                <div style="font-size:48px;margin-bottom:16px;">😕</div>
                <h3 style="font-size:18px;color:#1A1A1A;margin-bottom:8px;">Không thể tải trang</h3>
                <p style="color:#8A8A8A;font-size:14px;margin-bottom:24px;">${err.message}</p>
                <button onclick="window.location.hash='#home'"
                    style="padding:12px 32px;background:#3D5A4C;color:white;border:none;
                           font-family:'DM Sans',sans-serif;font-size:12px;
                           letter-spacing:0.1em;text-transform:uppercase;cursor:pointer;">
                    Về trang chủ
                </button>
            </div>`;
        contentArea.style.opacity = '1';
    } finally {
        window.hideLoading();
    }
}

// ==========================================
// 4. THỰC THI SCRIPT SAU KHI INJECT HTML
// ==========================================
function executeScripts(container, moduleName) {
    const scripts = container.querySelectorAll('script');
    console.log(`[Router] Tìm thấy ${scripts.length} script trong module: ${moduleName}`);

    Array.from(scripts).forEach(oldScript => {
        if (oldScript.src) {
            const newScript = document.createElement('script');
            Array.from(oldScript.attributes).forEach(attr =>
                newScript.setAttribute(attr.name, attr.value)
            );
            document.body.appendChild(newScript);
        } else {
            try {
                eval(oldScript.innerHTML);
                console.log(`[Router] ✅ Script OK`);
            } catch (e) {
                console.error(`[Router] ❌ Lỗi script ${moduleName}:`, e);
                window.toast(`Lỗi script: ${e.message}`, 'error');
            }
        }
        oldScript.remove();
    });

    // Kích hoạt init giống pattern bên admin
    setTimeout(() => {
        console.log(`[Router] 🚀 Init module: ${moduleName}`);
        if      (moduleName === 'home'     && window.HomeApp)      window.HomeApp.init();
        else if (moduleName === 'shop'     && window.ShopApp)      window.ShopApp.init();
        else if (moduleName === 'detail'   && window.DetailApp)    window.DetailApp.init();
        else if (moduleName === 'cart'     && window.CartApp)      window.CartApp.init();
        else if (moduleName === 'checkout' && window.CheckoutApp)  window.CheckoutApp.init();
        else if (moduleName === 'order'   && window.MyOrdersApp)  window.MyOrdersApp.init();
        else if (moduleName === 'account'  && window.AccountApp)   window.AccountApp.init();
        else if (moduleName === 'collection'  && window.CollectionApp)   window.CollectionApp.init();
        else if (moduleName === 'about'  && window.AboutApp)   window.AboutApp.init();
        else if (moduleName === 'sale'  && window.SaleApp)   window.SaleApp.init();
        else console.warn(`[Router] Chưa có init cho module: ${moduleName}`);
    }, 100);
}

// ==========================================
// 5. CẬP NHẬT BADGE GIỎ HÀNG
// ==========================================
window.updateCartBadge = async function() {
    try {
        const res = await fetch('/api/cart/count');
        if (!res.ok) return;
        const count = await res.json();
        const badge = document.getElementById('cartBadge');
        if (badge) {
            badge.textContent = count;
            badge.style.display = count > 0 ? 'flex' : 'none';
        }
    } catch (e) {
        console.warn('[Cart] Không thể cập nhật badge:', e);
    }
};

// ==========================================
// 6. KHỞI TẠO ROUTER
// ==========================================
function initRouter() {
    const hash = window.location.hash.slice(1);
    const moduleName = hash.split('?')[0];

    if (moduleName) {
        loadModule(moduleName);
    } else {
        window.location.hash = '#home'; // mặc định vào home
    }

    window.updateCartBadge();
}

window.addEventListener('hashchange', () => {
    const moduleName = window.location.hash.slice(1).split('?')[0];
    if (moduleName) loadModule(moduleName);
});

document.addEventListener('DOMContentLoaded', initRouter);