    /* ================= API CẦN THIẾT ================= */
    const API = {
    // API tìm kiếm biến thể (sản phẩm) theo SKU/Tên
    searchVariant: '/api/san-pham-bien-the/search?q=',
    // API tạo hóa đơn offline (quan trọng nhất)
    createInvoice: '/api/hoa-don/offline',
    // API tìm khách hàng theo SĐT (dùng cho tích điểm)
    findCustomer: '/api/khach-hang/by-tel?tel=',
};

    /* ================= STATE ================= */
    let cartState = JSON.parse(sessionStorage.getItem('posCart')) || [];
    let currentCustomer = null;
    let currentEmployee = { id: 1, hoTen: 'Ngô Việt Hoàng' }; // Hardcode tạm ID nhân viên và tên mày

    /* ================= HELPERS (CORE UI/LOGIC) ================= */
    const $ = (s) => document.querySelector(s);

    function formatMoney(n) {
    if(n==null || isNaN(n)) return '0';
    try{ return Number(n).toLocaleString('vi-VN'); }catch{ return String(n); }
}

    // --- Toast & Confirm (Dùng lại từ code cũ) ---
    function showToast(message, type = 'info', title){
    const host = $('#toastHost');
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

    function uiConfirm(message){
    return new Promise((resolve)=>{
    $('#confirmMsg').textContent = message;
    const ov = $('#confirmOverlay');
    ov.style.display = 'flex';
    const ok = ()=>{ cleanup(); resolve(true); };
    const cancel = ()=>{ cleanup(); resolve(false); };
    function onKey(e){ if(e.key==='Escape'){ cancel(); } }
    function cleanup(){
    ov.style.display = 'none';
    $('#confirmOk').removeEventListener('click', ok);
    $('#confirmCancel').removeEventListener('click', cancel);
    document.removeEventListener('keydown', onKey);
}
    $('#confirmOk').addEventListener('click', ok);
    $('#confirmCancel').addEventListener('click', cancel);
    document.addEventListener('keydown', onKey);
});
}

    async function request(url, opts={}){
    const res = await fetch(url, { headers: { 'Content-Type':'application/json', 'Accept':'application/json' }, ...opts });
    if(!res.ok){ const text = await res.text(); throw new Error(text || res.statusText); }
    if(res.status === 204) return null;
    const ct = res.headers.get('content-type') || '';
    return ct.includes('application/json') ? res.json() : res.text();
}

    function debounce(func, timeout = 300) {
    let timer;
    return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => { func.apply(this, args); }, timeout);
};
}
    // --- End Toast & Confirm ---


    /* ================= LOGIC GIỎ HÀNG ================= */
    function saveCart() {
    sessionStorage.setItem('posCart', JSON.stringify(cartState));
}

    function addToCart(variant) {
    // Mày phải đảm bảo variant có các trường: id, sku, ten, gia, soLuong (tồn kho)
    if (variant.soLuong <= 0) {
    toast(`SKU ${variant.sku} đã hết hàng!`, 'error');
    return;
}

    const existing = cartState.find(item => item.id === variant.id);
    if (existing) {
    if (existing.quantity + 1 > variant.soLuong) {
    toast(`SKU ${variant.sku} chỉ còn ${variant.soLuong} sản phẩm!`, 'error');
    return;
}
    existing.quantity += 1;
} else {
    cartState.push({
    id: variant.id,
    sku: variant.sku,
    name: variant.ten,
    price: variant.gia,
    quantity: 1,
    maxQty: variant.soLuong,
});
}

    saveCart();
    renderCart();
    renderSummary();
    toast(`Đã thêm ${variant.sku} vào giỏ.`, 'success', 'Thêm SP');
}

    function removeFromCart(id) {
    cartState = cartState.filter(item => String(item.id) !== String(id));
    saveCart();
    renderCart();
    renderSummary();
}

    function updateCartQuantity(inputElement) {
    const id = inputElement.dataset.id;
    let newQty = parseInt(inputElement.value) || 1;

    const item = cartState.find(item => String(item.id) === String(id));
    if (!item) return;

    // Validation
    if (newQty < 1) newQty = 1;
    if (newQty > item.maxQty) {
    newQty = item.maxQty;
    toast(`Số lượng tối đa chỉ là ${item.maxQty}.`, 'error');
}

    inputElement.value = newQty; // Cập nhật lại giá trị hiển thị (nếu bị giới hạn)
    item.quantity = newQty;

    saveCart();
    renderSummary();
}


    /* ================= RENDER ================= */
    function renderCart() {
    const itemsEl = $('#cartItems');
    const emptyEl = $('#emptyCart');
    const count = cartState.reduce((sum, item) => sum + item.quantity, 0);

    $('#cartCount').textContent = `(${count})`;

    if (cartState.length === 0) {
    itemsEl.innerHTML = '';
    emptyEl.style.display = 'block';
    return;
}
    emptyEl.style.display = 'none';

    // Code render Giỏ hàng
    let html = '';
    cartState.forEach(item => {
    html += `
                <div class="cart-item-row" data-id="${item.id}">
                    <div class="row" style="justify-content:space-between; align-items:flex-start;">
                        <div style="flex-basis: 60%; overflow:hidden;">
                            <div style="font-weight: 600;">${item.name}</div>
                            <div class="muted">SKU: ${item.sku} | Giá: ${formatMoney(item.price)}</div>
                        </div>
                        <div class="row" style="flex-basis: 40%; justify-content:flex-end;">
                            <input type="number" data-id="${item.id}" value="${item.quantity}" min="1" max="${item.maxQty}" class="input" style="width: 60px; text-align: center; padding: 6px;" onchange="updateCartQuantity(this)">
                            <button class="btn destructive" data-id="${item.id}" onclick="removeFromCart(this.dataset.id)" style="padding: 6px 10px;">🗑</button>
                        </div>
                    </div>
                    <div style="text-align:right; font-weight:600; padding-top:4px;">Tổng: ${formatMoney(item.price * item.quantity)}</div>
                </div>
            `;
});
    itemsEl.innerHTML = html;
}

    function renderSummary() {
    const subTotal = cartState.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const discount = 0; // Logic giảm giá/Voucher
    const grandTotal = subTotal - discount;

    $('#subTotal').textContent = formatMoney(subTotal);
    $('#discount').textContent = formatMoney(discount);
    $('#grandTotal').textContent = formatMoney(grandTotal);

    $('#memberInfo').textContent = `Khách hàng: ${currentCustomer?.hoTen || 'Khách lẻ'} / NV: ${currentEmployee.hoTen}`;
}

    function renderProductList(variants) {
    const listEl = $('#productList');
    listEl.innerHTML = ''; // Xóa nội dung cũ

    if (variants.length === 0) {
    listEl.innerHTML = '<div class="muted" style="text-align:center; padding:20px;">Không tìm thấy sản phẩm.</div>';
    return;
}

    variants.forEach(v => {
    const item = document.createElement('div');
    item.className = 'product-item';

    // Fix dữ liệu: nếu API trả về SPBTHE, nó có id, sku, gia, soLuong, TEN (của BTH)
    const productName = `${v.ten} (${v.sku})`;

    item.innerHTML = `
                <div>
                    <div style="font-weight: 600;">${productName}</div>
                    <div class="muted">${formatMoney(v.gia)} | Tồn: ${v.soLuong || 0}</div>
                </div>
                <button class="btn primary" onclick='addToCart(${JSON.stringify(v)})'>+</button>
            `;
    listEl.appendChild(item);
});
}

    /* ================= XỬ LÝ SỰ KIỆN API ================= */

    async function fetchAndRenderProducts(q) {
    const listEl = $('#productList');
    if (q.length < 3) {
    listEl.innerHTML = '<div id="productHint" class="muted" style="text-align:center; padding:20px;">Nhập ít nhất 3 ký tự để tìm kiếm, hoặc quét mã.</div>';
    return;
}

    listEl.innerHTML = '<div style="text-align:center; padding:20px;"><span class="spin"></span> Đang tìm...</div>';

    try {
    const data = await request(`${API.searchVariant}${q}`);
    const variants = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : []);
    renderProductList(variants);
} catch (e) {
    toast('Lỗi tìm kiếm sản phẩm: ' + e.message, 'error');
    listEl.innerHTML = '<div class="muted" style="text-align:center; padding:20px; color:var(--red);">Lỗi khi tìm kiếm.</div>';
}
}

    async function handleScan(q) {
    if (!q) return;

    try {
    const data = await request(`${API.searchVariant}${q}`);
    const variants = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : []);

    if (variants.length === 1) {
    addToCart(variants[0]);
    $('#posSearch').value = '';
    $('#productList').innerHTML = '<div id="productHint" class="muted" style="text-align:center; padding:20px;">Đã thêm vào giỏ. Quét mã tiếp theo.</div>';
} else if (variants.length > 1) {
    renderProductList(variants);
} else {
    toast('Không tìm thấy sản phẩm nào khớp với mã/tên.', 'error');
    $('#productList').innerHTML = '<div id="productHint" class="muted" style="text-align:center; padding:20px;">Không tìm thấy.</div>';
}

} catch (e) {
    toast('Lỗi tìm kiếm sản phẩm: ' + e.message, 'error');
}
}

    // Sự kiện HOÀN TẤT GIAO DỊCH
    $('#btnCompleteSale').onclick = async () => {
    if (cartState.length === 0) {
    toast('Giỏ hàng trống, không thể thanh toán!', 'error');
    return;
}

    // Kiểm tra lại tồn kho lần cuối (nếu cần)

    if (!await uiConfirm('Xác nhận tạo hóa đơn và thanh toán?')) return;

    const subTotal = cartState.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const payload = {
    // Tạm hardcode ID theo DB
    idCuaHang: 1,
    idCaBanHang: 1,
    idNhanVien: currentEmployee.id,
    idTaiKhoanKhach: currentCustomer?.idTaiKhoan || null,

    tongTien: subTotal,
    giamGia: 0,
    thanhTien: subTotal,
    hinhThuc: $('#paymentMethod').value,

    // Chi tiết hóa đơn (HoaDonOfflineChiTiet)
    chiTiet: cartState.map(item => ({
    idSanPhamBienThe: item.id,
    soLuong: item.quantity,
    donGia: item.price,
    giamGia: 0
}))
};

    try {
    // API BE phải xử lý TẠO HÓA ĐƠN + TRỪ TỒN KHO
    await request(API.createInvoice, { method: 'POST', body: JSON.stringify(payload) });
    toast('Tạo hóa đơn thành công! Tồn kho đã được trừ.', 'success', 'Hoàn tất');

    // Reset trạng thái
    cartState = [];
    sessionStorage.removeItem('posCart');
    currentCustomer = null;
    renderCart();
    renderSummary();
    $('#posSearch').value = '';
} catch (e) {
    toast('Lỗi tạo hóa đơn: ' + e.message, 'error');
}
};

    // Sự kiện HỦY ĐƠN HÀNG
    $('#btnCancelSale').onclick = async () => {
    if (cartState.length === 0) {
    toast('Giỏ hàng đã trống.', 'info');
    return;
}
    if (!await uiConfirm('Hủy toàn bộ đơn hàng hiện tại?')) return;

    cartState = [];
    sessionStorage.removeItem('posCart');
    currentCustomer = null;
    renderCart();
    renderSummary();
    toast('Đã hủy đơn hàng.', 'info');
};


    /* ================= WIRE-UP ================= */

    // Tìm kiếm bằng từ khóa (Input)
    $('#posSearch').addEventListener('input', debounce((e) => {
    fetchAndRenderProducts(e.target.value.trim());
}));

    // Tìm kiếm bằng Quét/Enter (Scan)
    $('#posSearch').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
    e.preventDefault();
    handleScan(e.target.value.trim());
}
});

    // Khởi tạo
    document.addEventListener('DOMContentLoaded', () => {
    // Cập nhật tên nhân viên hardcode
    $('#memberInfo').textContent = `Khách lẻ / NV: ${currentEmployee.hoTen}`;
    renderCart();
    renderSummary();
});
