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
// 3. CART HELPER (BỌC THÉP CHỐNG CRASH)
// ==========================================
window.CartHelper = {
    // Lưu tạm vào LocalStorage
    saveToLocal(idSanPhamChiTiet, soLuong, productInfo) {
        let cart = JSON.parse(localStorage.getItem('bee_cart') || '[]');
        let exist = cart.find(item => item.idSanPhamChiTiet === idSanPhamChiTiet);
        if (exist) {
            exist.soLuongTrongGio += soLuong;
        } else {
            cart.push({
                id: 'local_' + Date.now(),
                idSanPhamChiTiet: idSanPhamChiTiet,
                soLuongTrongGio: soLuong,
                ...(productInfo || {})
            });
        }
        localStorage.setItem('bee_cart', JSON.stringify(cart));
        if (window.updateCartBadge) window.updateCartBadge();
    },

    // 🌟 VŨ KHÍ BÍ MẬT: HÀM DỊCH JSON AN TOÀN
    async safeJson(res) {
        const ct = res.headers.get("content-type");
        if (ct && ct.includes("application/json")) {
            return await res.json(); // An toàn thì dịch
        }
        const text = await res.text();
        console.error("🚨 Server không trả về JSON. Nội dung thực tế là:", text);
        return null;
    },

    async getCart() {
        try {
            const res = await fetch('/api/gio-hang', { headers: { 'X-Requested-With': 'XMLHttpRequest' } });

            if (res.ok) {
                const data = await this.safeJson(res);
                if (!data) return JSON.parse(localStorage.getItem('bee_cart') || '[]'); // Fallback cứu hộ

                // Đồng bộ từ LocalStorage lên DB
                let localCart = JSON.parse(localStorage.getItem('bee_cart') || '[]');
                if (localCart.length > 0) {
                    for (let item of localCart) {
                        try {
                            await fetch('/api/gio-hang/them', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                                body: JSON.stringify({ idSanPhamChiTiet: item.idSanPhamChiTiet, soLuong: item.soLuongTrongGio })
                            });
                        } catch(e) {}
                    }
                    localStorage.removeItem('bee_cart');

                    const syncRes = await fetch('/api/gio-hang', { headers: { 'X-Requested-With': 'XMLHttpRequest' } });
                    const syncData = await this.safeJson(syncRes);
                    return syncData || data;
                }
                return data;
            } else {
                return JSON.parse(localStorage.getItem('bee_cart') || '[]');
            }
        } catch (e) {
            return JSON.parse(localStorage.getItem('bee_cart') || '[]');
        }
    },

    async add(idSanPhamChiTiet, soLuong, productInfo = null) {
        try {
            const res = await fetch('/api/gio-hang/them', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                body: JSON.stringify({ idSanPhamChiTiet, soLuong })
            });

            if (res.status === 401 || res.status === 403) {
                this.saveToLocal(idSanPhamChiTiet, soLuong, productInfo);
                return true;
            }

            if (res.ok) {
                if (window.updateCartBadge) window.updateCartBadge();
                return true;
            }

            const err = await this.safeJson(res);
            throw new Error(err ? err.message : 'Lỗi hệ thống Backend');
        } catch (e) {
            throw e;
        }
    },

    async update(idGioHangChiTiet, idSanPhamChiTiet, soLuongMoi) {
        if (String(idGioHangChiTiet).startsWith('local_')) {
            let cart = JSON.parse(localStorage.getItem('bee_cart') || '[]');
            let item = cart.find(i => i.id === idGioHangChiTiet);
            if (item) {
                if (soLuongMoi <= 0) cart = cart.filter(i => i.id !== idGioHangChiTiet);
                else item.soLuongTrongGio = soLuongMoi;
                localStorage.setItem('bee_cart', JSON.stringify(cart));
                if (window.updateCartBadge) window.updateCartBadge();
            }
            return true;
        } else {
            const res = await fetch('/api/gio-hang/cap-nhat', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
                body: JSON.stringify({ idGioHangChiTiet, soLuong: soLuongMoi })
            });
            if (res.ok) {
                if (window.updateCartBadge) window.updateCartBadge();
                return true;
            }
            const err = await this.safeJson(res);
            throw new Error(err ? err.message : 'Lỗi cập nhật');
        }
    },

    async remove(idGioHangChiTiet) {
        if (String(idGioHangChiTiet).startsWith('local_')) {
            let cart = JSON.parse(localStorage.getItem('bee_cart') || '[]');
            cart = cart.filter(i => i.id !== idGioHangChiTiet);
            localStorage.setItem('bee_cart', JSON.stringify(cart));
            if (window.updateCartBadge) window.updateCartBadge();
            return true;
        } else {
            const res = await fetch(`/api/gio-hang/xoa/${idGioHangChiTiet}`, {
                method: 'DELETE', headers: { 'X-Requested-With': 'XMLHttpRequest' }
            });
            if (res.ok) {
                if (window.updateCartBadge) window.updateCartBadge();
                return true;
            }
            throw new Error("Lỗi khi xóa sản phẩm");
        }
    },

    async clear() {
        try {
            const res = await fetch('/api/gio-hang/xoa-tat-ca', {
                method: 'DELETE', headers: { 'X-Requested-With': 'XMLHttpRequest' }
            });
            if (res.status === 401 || res.status === 403) localStorage.removeItem('bee_cart');
        } catch(e) {
            localStorage.removeItem('bee_cart');
        }
        if (window.updateCartBadge) window.updateCartBadge();
    }
};

// ==========================================
// 4. ROUTER LOGIC
// ==========================================
async function loadModule(moduleName) {
    const contentArea = document.getElementById('content-area');
    if (!contentArea) return;

    window.showLoading();

    const moduleMap = {
        'home':       { url: '/customer/home',       title: 'Trang chủ' },
        'shop':       { url: '/customer/shop',       title: 'Sản phẩm' },
        'detail':     { url: '/customer/detail',     title: 'Chi tiết sản phẩm' },
        'cart':       { url: '/customer/cart',       title: 'Giỏ hàng' },
        'checkout':   { url: '/customer/checkout',   title: 'Thanh toán' },
        'order':      { url: '/customer/order',      title: 'Đơn hàng của tôi' },
        'account':    { url: '/customer/account',    title: 'Tài khoản' },
        'about':      { url: '/customer/about',      title: 'About Beemate' },
        'collection': { url: '/customer/collection', title: 'Bộ sưu tập' },
        'sale':       { url: '/customer/sale',       title: 'Sale' },
    };

    const module = moduleMap[moduleName];
    if (!module) {
        window.hideLoading();
        console.warn(`[Router] Module "${moduleName}" không tồn tại`);
        return;
    }

    document.title = `BEEMATE | ${module.title}`;

    document.querySelectorAll('.nav-links a[data-module]').forEach(link => {
        link.classList.toggle('active', link.dataset.module === moduleName);
    });

    try {
        const queryStr = window.location.hash.includes('?')
            ? window.location.hash.split('?')[1] : '';
        const fetchUrl = queryStr ? `${module.url}?${queryStr}` : module.url;

        const res = await fetch(fetchUrl);
        if (!res.ok) throw new Error(`Lỗi ${res.status}: ${res.statusText}`);

        const html = await res.text();

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
// 5. THỰC THI SCRIPT SAU KHI INJECT HTML
// ==========================================
function executeScripts(container, moduleName) {
    const scripts = container.querySelectorAll('script');

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
            } catch (e) {
                console.error(`[Router] ❌ Lỗi script ${moduleName}:`, e);
            }
        }
        oldScript.remove();
    });

    setTimeout(() => {
        if      (moduleName === 'home'       && window.HomeApp)       window.HomeApp.init();
        else if (moduleName === 'shop'       && window.ShopApp)       window.ShopApp.init();
        else if (moduleName === 'detail'     && window.DetailApp)     window.DetailApp.init();
        else if (moduleName === 'cart'       && window.CartApp)       window.CartApp.init();
        else if (moduleName === 'checkout'   && window.CheckoutApp)   window.CheckoutApp.init();
        else if (moduleName === 'order'      && window.MyOrdersApp)   window.MyOrdersApp.init();
        else if (moduleName === 'account'    && window.AccountApp)    window.AccountApp.init();
        else if (moduleName === 'collection' && window.CollectionApp) window.CollectionApp.init();
        else if (moduleName === 'about'      && window.AboutApp)      window.AboutApp.init();
        else if (moduleName === 'sale'       && window.SaleApp)       window.SaleApp.init();
    }, 100);
}

// ==========================================
// 6. CẬP NHẬT BADGE GIỎ HÀNG (VIẾT CHUẨN)
// ==========================================
window.updateCartBadge = async function() {
    try {
        const cart = await window.CartHelper.getCart();
        const count = cart.reduce((sum, item) => sum + parseInt(item.soLuongTrongGio || item.qty || 0), 0);

        const badge = document.getElementById('cart-badge-count');
        const iconWrap = document.querySelector('.cart-icon');

        if (badge) {
            const oldCount = parseInt(badge.textContent || 0);
            if (count > oldCount && iconWrap) {
                iconWrap.classList.remove('bounce');
                void iconWrap.offsetWidth; // Mẹo nhỏ để kích hoạt lại animation
                iconWrap.classList.add('bounce');
            }

            badge.textContent = count;
            badge.style.display = count > 0 ? 'flex' : 'none';
        }
    } catch (e) {
        console.warn('[Cart] Không thể cập nhật badge:', e);
    }
};

// ==========================================
// 7. KHỞI TẠO ROUTER
// ==========================================
function initRouter() {
    const hash = window.location.hash.slice(1);
    const moduleName = hash.split('?')[0];

    if (moduleName) {
        loadModule(moduleName);
    } else {
        window.location.hash = '#home';
    }
    window.updateCartBadge();
}

window.addEventListener('hashchange', () => {
    const moduleName = window.location.hash.slice(1).split('?')[0];
    if (moduleName) loadModule(moduleName);
});

document.addEventListener('DOMContentLoaded', initRouter);