{ // BẮT ĐẦU BLOCK SCOPE ĐỂ NGĂN XUNG ĐỘT GLOBAL

    /* ================= API CONFIG ================= */
    const API_PROD = {
        danh_muc: '/api/danh-muc',
        hang: '/api/hang',
        chat_lieu: '/api/chat-lieu',
        kich_thuoc: '/api/kich-thuoc',
        mau_sac: '/api/mau-sac',

        san_pham: '/api/san-pham', // CRUD Sản phẩm

        // Các API con (Biến thể, Ảnh)
        variant: (pid) => `/api/san-pham/${pid}/bien-the`,
        variant_crud: '/api/san-pham-bien-the',
        image: (pid) => `/api/san-pham/${pid}/images`,
        image_crud: '/api/hinh-anh-san-pham'
    };

    /* ================= STATE ================= */
    let prod_tab = 'products';
    let prod_id = null;
    let selectedProductData = null;

    let prod_q = '';
    let prod_page = 1;
    const prod_size = 5;

    let prod_rows = [];
    let prod_totalElements = 0;
    let prod_totalPages = 1;
    let prod_editingId = null;

    let fDm='', fHang='', fCl='';
    let catalogs = { dm: [], hang: [], cl: [], sz: [], color: [] };

    /* ================= HELPERS (Local) ================= */
    const _get = (s) => document.querySelector(s); // Selector cục bộ

    function _fmtMoney(n){
        if(n==null) return '—';
        try{ return Number(n).toLocaleString('vi-VN'); }catch{ return String(n); }
    }
    function findById(arr, id){ return arr.find(x=>String(x.id)===String(id)); }

    // FIX DB INTEGRITY: Chuyển chuỗi rỗng ("" hoặc "0") thành NULL trước khi gửi lên Java DTO
    const _valOrNull = (v) => (v === "" || v === "0") ? null : v;


    /* ================= CORE: LOAD CATALOGS ================= */
    async function loadCatalogsOnce(){
        if(catalogs.dm.length) return;
        try{
            const [dm, h, cl, sz, color] = await Promise.all([
                request(`${API_PROD.danh_muc}?page=0&size=100`),
                request(`${API_PROD.hang}?page=0&size=100`),
                request(`${API_PROD.chat_lieu}?page=0&size=100`),
                request(`${API_PROD.kich_thuoc}?page=0&size=100`),
                request(`${API_PROD.mau_sac}?page=0&size=100`)
            ]);
            catalogs.dm = dm.content || dm || [];
            catalogs.hang = h.content || h || [];
            catalogs.cl = cl.content || cl || [];
            catalogs.sz = sz.content || sz || [];
            catalogs.color = color.content || color || [];

            // Fill Filters
            fillSelect(_get('#fDanhMuc'), catalogs.dm, '— Danh mục —');
            fillSelect(_get('#fHang'), catalogs.hang, '— Hãng —');
            fillSelect(_get('#fChatLieu'), catalogs.cl, '— Chất liệu —');
        }catch(e){ console.error(e); }
    }

    function fillSelect(sel, list, placeholder){
        if(!sel) return;
        const val = sel.value;
        sel.innerHTML = `<option value="">${placeholder}</option>` + list.map(x=>`<option value="${x.id}">${x.ten}</option>`).join('');
        sel.value = val;
    }

    /* ================= RENDER MAIN ================= */
    async function loadAndRender(){
        if(prod_tab === 'products') {
            _get('#filtersBar').style.display = 'flex';
            _get('#subHeader').style.display = 'none';
            await renderProducts();
        } else {
            _get('#filtersBar').style.display = 'none';
            _get('#subHeader').style.display = 'flex';

            const spName = selectedProductData ? `SP: ${selectedProductData.ten} (${selectedProductData.ma})` : `SP #${prod_id}`;
            _get('#selProduct').textContent = spName;

            if(prod_tab === 'variants') await renderVariants();
            else if(prod_tab === 'images') await renderImages();
        }
        updatePager();
        updateTabsState();
    }

    function updateTabsState(){
        document.querySelectorAll('.tab').forEach(b => {
            b.classList.toggle('active', b.dataset.tab === prod_tab);
        });

        const btn = _get('#btnAdd');
        if(prod_tab === 'products') {
            btn.textContent = '➕ Thêm Sản phẩm';
            btn.disabled = false;
        } else if(prod_tab === 'variants') {
            btn.textContent = '➕ Thêm Biến thể';
            btn.disabled = !prod_id;
        } else {
            btn.textContent = '➕ Thêm Ảnh';
            btn.disabled = !prod_id;
        }
    }

    function setLoading(on){
        const tbody = _get('#tbody');
        const cols = prod_tab === 'products' ? 8 : (prod_tab === 'variants' ? 7 : 4);
        if(on && tbody) tbody.innerHTML = `<tr><td colspan="${cols}" style="text-align:center;padding:20px"><span class="spin"></span> Đang tải...</td></tr>`;
    }

    function updatePager(){
        _get('#info').textContent = `Hiển thị ${prod_rows.length} / ${prod_totalElements} bản ghi`;
        _get('#pageKpi').textContent = `${prod_page} / ${prod_totalPages}`;
        _get('#prev').disabled = prod_page<=1;
        _get('#next').disabled = prod_page>=prod_totalPages;
    }

    /* ================= LOGIC: PRODUCTS ================= */
    async function renderProducts(){
        _get('#thead').innerHTML = `
            <tr>
                <th>Mã</th><th>Tên</th><th>Danh mục</th><th>Hãng</th><th>Chất liệu</th><th style="width:140px">Ngày tạo</th><th>Trạng thái</th><th style="text-align:right">Thao tác</th>
            </tr>`;
        setLoading(true);
        try {
            await loadCatalogsOnce();
            const params = new URLSearchParams({
                page: prod_page-1, size: prod_size,
                q: prod_q, idDanhMuc: fDm, idHang: fHang, idChatLieu: fCl
            });

            const data = await request(`${API_PROD.san_pham}?${params}`);
            if(data && data.content) {
                prod_rows = data.content;
                prod_totalElements = data.totalElements;
                prod_totalPages = data.totalPages;
            } else { prod_rows=[]; prod_totalElements=0; prod_totalPages=1; }

            const tbody = _get('#tbody'); tbody.innerHTML = '';
            if(!prod_rows.length) { tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px" class="muted">Không có dữ liệu</td></tr>'; return; }

            prod_rows.forEach(row => {
                const dm = findById(catalogs.dm, row.idDanhMuc)?.ten || '—';
                const hang = findById(catalogs.hang, row.idHang)?.ten || '—';
                const cl = findById(catalogs.cl, row.idChatLieu)?.ten || '—';
                const on = !!row.trangThai;
                const ngayTao = row.ngayTao ? row.ngayTao.slice(0, 10) : '—';

                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${row.ma||'—'}</td>
                    <td style="font-weight:500">${row.ten}</td>
                    <td>${dm}</td>
                    <td>${hang}</td>
                    <td>${cl}</td>
                    <td>${ngayTao}</td> 
                    <td style="text-align:center"><span class="badge ${on?'green':'amber'}">${on?'Hiển thị':'Ẩn'}</span></td>
                    <td style="text-align:right">
                        <button class="btn icon" data-act="variants" title="Xem Biến thể">📦</button>
                        <button class="btn icon" data-act="images" title="Xem Ảnh">🖼️</button>
                        <button class="btn icon" data-act="edit">Sửa</button>
                        <button class="btn icon" data-act="toggle">${on?'Ẩn':'Hiện'}</button>
                    </td>
                `;
                // Events
                tr.querySelector('[data-act="variants"]').onclick = () => { prod_id = row.id; selectedProductData = row; switchTab('variants'); };
                tr.querySelector('[data-act="images"]').onclick = () => { prod_id = row.id; selectedProductData = row; switchTab('images'); };
                tr.querySelector('[data-act="edit"]').onclick = () => openDrawerProduct('edit', row);
                tr.querySelector('[data-act="toggle"]').onclick = () => toggleStatus(row, API_PROD.san_pham);
                tbody.appendChild(tr);
            });
        } catch(e){ console.error(e); prod_rows=[]; toast('Lỗi tải sản phẩm', 'error'); }
    }

    /* ================= LOGIC: VARIANTS/IMAGES (KHÔNG ĐỔI) ================= */
    async function renderVariants(){ /* ... */ }
    async function renderImages(){ /* ... */ }

    // ... (renderVariants và renderImages logic không đổi) ...


    /* ================= DRAWER & SAVE LOGIC ================= */
    function openDrawerProduct(mode, row){
        prod_editingId = mode==='edit' ? row.id : null;
        _get('#drawerTitle').textContent = (mode==='edit'?'Cập nhật':'Thêm mới') + ' Sản phẩm';

        const dmOpts = catalogs.dm.map(x=>`<option value="${x.id}">${x.ten}</option>`).join('');
        const hOpts = catalogs.hang.map(x=>`<option value="${x.id}">${x.ten}</option>`).join('');
        const clOpts = catalogs.cl.map(x=>`<option value="${x.id}">${x.ten}</option>`).join('');

        _get('#drawerBody').innerHTML = `
            <div><label class="label">Mã</label><input id="fMa" class="input" value="${row?.ma||''}" placeholder="Tự sinh nếu trống"></div>
            <div><label class="label">Tên <span class="req">*</span></label><input id="fTen" class="input" value="${row?.ten||''}"></div>
            <div><label class="label">Danh mục <span class="req">*</span></label><select id="fDM" class="select"><option value="">-- Chọn --</option>${dmOpts}</select></div>
            <div class="row">
                <div style="flex:1"><label class="label">Hãng</label><select id="fH" class="select"><option value="">-- Chọn --</option>${hOpts}</select></div>
                <div style="flex:1"><label class="label">Chất liệu</label><select id="fCL" class="select"><option value="">-- Chọn --</option>${clOpts}</select></div>
            </div>
            <div><label class="label">Mô tả</label><textarea id="fMoTa" class="textarea">${row?.moTa||''}</textarea></div>
            <div class="row between" style="margin-top:10px;border:1px solid var(--border);padding:10px;border-radius:8px">
                <div>Trạng thái</div><input type="checkbox" id="fTT" class="switch" ${ (row?.trangThai ?? true) ? 'checked':''}>
            </div>
        `;
        if(row){
            _get('#fDM').value = row.idDanhMuc; _get('#fH').value = row.idHang; _get('#fCL').value = row.idChatLieu;
        }
        _openDrawer();
    }

    function openDrawerVariant(mode, row){
        // ... (openDrawerVariant logic không đổi) ...
    }

    function openDrawerImage(){
        // ... (openDrawerImage logic không đổi) ...
    }


    async function save(){
        const btn = _get('#btnSave');
        if (!btn) return;
        const drawerTab = _get('#drawer').dataset.tab;

        // 1. KHÓA NÚT NGAY LẬP TỨC
        btn.disabled = true;
        btn.textContent = "Đang lưu...";

        try {
            if(drawerTab === 'products') {
                // --- LOGIC LƯU SẢN PHẨM CHÍNH (FIX NOT NULL & VALIDATION) ---
                const _valOrNull = (v) => (v === "" || v === "0") ? null : v; // Hàm fix rỗng thành null

                const payload = {
                    ma: _valOrNull(_get('#fMa').value.trim()),
                    ten: _get('#fTen').value.trim(),

                    // FIX NOT NULL FKs: GỬI NULL thay vì chuỗi rỗng
                    idDanhMuc: _valOrNull(_get('#fDM').value),
                    idHang: _valOrNull(_get('#fH').value),
                    idChatLieu: _valOrNull(_get('#fCL').value),

                    moTa: _get('#fMoTa').value,
                    trangThai: _get('#fTT').checked
                };

                // VALIDATION FE
                if(!payload.ten || !payload.idDanhMuc) throw new Error('Thiếu Tên hoặc Danh mục bắt buộc');

                const url = prod_editingId ? `${API_PROD.san_pham}/${prod_editingId}` : API_PROD.san_pham;
                const method = prod_editingId ? 'PUT' : 'POST';

                await request(url, { method, body: JSON.stringify(payload) }); // <--- API CALL

                prod_page = 1; // Reset page
                prod_editingId = null; // Reset edit state

            } else if(drawerTab === 'variants') {
                // ... (Logic Save Variant sẽ nằm ở đây) ...
                throw new Error("Logic Save Variant chưa hoàn thành."); // Tạm thời ném lỗi
            } else if(drawerTab === 'images') {
                // ... (Logic Save Image sẽ nằm ở đây) ...
                throw new Error("Logic Save Image chưa hoàn thành."); // Tạm thời ném lỗi
            }

            // 3. THÀNH CÔNG: Toast, đóng Drawer, Reload
            toast('Lưu thành công', 'success');
            _closeDrawer();
            loadAndRender();

        } catch(e){
            // 4. THẤT BẠI: (Bao gồm lỗi DB Rollback)
            toast(e.message || 'Lỗi lưu dữ liệu', 'error');

        } finally {
            // 5. MỞ KHÓA NÚT DÙ THÀNH CÔNG HAY THẤT BẠI
            btn.disabled = false;
            btn.textContent = "Lưu";
        }
    }

    async function toggleStatus(row, apiBase){
        // ... (toggleStatus logic không đổi) ...
        if(!await uiConfirm('Đổi trạng thái mục này?')) return;
        try {
            const body = { ...row, trangThai: !row.trangThai };
            // Cần check lại logic update 1 phần nếu BE hỗ trợ PATCH, hoặc gửi full body
            await request(`${apiBase}/${row.id}`, { method:'PUT', body: JSON.stringify(body) });
            toast('Đã cập nhật', 'success');
            loadAndRender();
        } catch(e){ toast('Lỗi cập nhật', 'error'); }
    }

    function _openDrawer(){ _get('#overlay').classList.add('show'); _get('#drawer').classList.add('show'); }
    function _closeDrawer(){ _get('#overlay').classList.remove('show'); _get('#drawer').classList.remove('show'); }

    /* ================= NAVIGATION ================= */
    function switchTab(tabName){
        prod_tab = tabName;
        prod_page = 1;
        loadAndRender();
    }

    /* ================= INIT ================= */
    window.initProductModule = function() {
        // Wire Events
        _get('#btnAdd').onclick = () => {
            if(prod_tab === 'products') openDrawerProduct('create');
            else if(prod_tab === 'variants') openDrawerVariant('create');
            else openDrawerImage();
        };
        _get('#btnSave').onclick = save;
        _get('#btnClose').onclick = _closeDrawer;
        _get('#overlay').onclick = _closeDrawer;

        // Filter Events
        _get('#search').oninput = (e) => {
            prod_q = e.target.value; prod_page=1;
            if(prod_tab==='products') setTimeout(loadAndRender, 500); // Debounce thô
        };
        _get('#fDanhMuc').onchange = ()=>{ fDm = _get('#fDanhMuc').value; prod_page=1; loadAndRender(); };
        _get('#fHang').onchange = ()=>{ fHang = _get('#fHang').value; prod_page=1; loadAndRender(); };
        _get('#fChatLieu').onchange = ()=>{ fCl = _get('#fChatLieu').value; prod_page=1; loadAndRender(); };

        _get('#prev').onclick = ()=>{ if(prod_page>1){ prod_page--; loadAndRender(); } };
        _get('#next').onclick = ()=>{ if(prod_page<prod_totalPages){ prod_page++; loadAndRender(); } };

        // Tab Click & Back Button
        _get('#btnBack').onclick = () => switchTab('products');
        loadAndRender();
    };
}