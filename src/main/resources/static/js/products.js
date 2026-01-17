{
    /* ================= API ================= */
    const API = {
        products: '/api/products',
        danh_muc: '/api/danh-muc',
        hang: '/api/hang',
        chat_lieu: '/api/chat-lieu'
    };

    let isLoading = false;


    /* ================= STATE ================= */
    let activeTab = 'products';
    let selectedProduct = null;

    let q = '';
    let page = 1;
    const size = 10;

    let rows = [];
    let totalElements = 0;
    let totalPages = 1;
    let editingId = null;

    /* ================= HELPERS ================= */
    const _get = s => document.querySelector(s);

    /* ================= API ================= */
    async function apiList() {
        const params = new URLSearchParams({page: page - 1, size});

        if (q) params.append('q', q);
        if (_get('#fDanhMuc').value) params.append('idDanhMuc', _get('#fDanhMuc').value);
        if (_get('#fHang').value) params.append('idHang', _get('#fHang').value);
        if (_get('#fChatLieu').value) params.append('idChatLieu', _get('#fChatLieu').value);

        const st = _get('#fTrangThaiFilter');
        if (st && st.value !== '') {
            params.append('trangThai', st.value === 'true');
        }

        const data = await request(`${API.products}?${params}`);
        let list = data.content || [];

        rows = list;
        totalElements = data.totalElements ?? list.length;
        totalPages = data.totalPages ?? 1;
    }

    async function apiCreate(payload) {
        return request(API.products, {method: 'POST', body: JSON.stringify(payload)});
    }

    async function apiUpdate(id, payload) {
        return request(`${API.products}/${id}`, {method: 'PUT', body: JSON.stringify(payload)});
    }

    /* ================= LOAD OPTIONS ================= */
    async function loadOptions(select, url) {
        select.innerHTML = '<option value=""></option>';
        const data = await request(`${url}?page=0&size=100`);
        (data.content || data)
            .filter(x => x.trangThai === true)
            .forEach(x => {
                const o = document.createElement('option');
                o.value = x.id;
                o.textContent = x.ten;
                select.appendChild(o);
            });
    }

    /* ================= TABLE ================= */
    function renderTable() {
        const thead = _get('#thead');
        const tbody = _get('#tbody');

        thead.innerHTML = `
<tr>
  <th>Mã</th>
  <th>Tên</th>
  <th>Mô tả</th>
  <th>Danh mục</th>
  <th>Hãng</th>
  <th>Chất liệu</th>
  <th style="text-align:center">Trạng thái</th>
  <th style="text-align:right;padding-right:16px">Hành động</th>
</tr>
`;

        // ✅ KHÔNG CÓ DATA
        if (rows.length === 0) {
            tbody.innerHTML = `
<tr>
  <td colspan="8" style="height:360px;text-align:center" class="muted">
    Không có dữ liệu
  </td>
</tr>`;
            updatePager();
            return;
        }

        // ✅ RENDER 1 LẦN DUY NHẤT
        const frag = document.createDocumentFragment();

        rows.forEach(r => {
            const tr = document.createElement('tr');
            tr.style.cursor = 'pointer';

            tr.innerHTML = `
<td><div class="ellipsis">${r.ma}</div></td>
<td><div class="ellipsis">${r.ten}</div></td>
<td><div class="ellipsis">${r.moTa || '—'}</div></td>
<td><div class="ellipsis">${r.danhMuc?.ten || '—'}</div></td>
<td><div class="ellipsis">${r.hang?.ten || '—'}</div></td>
<td><div class="ellipsis">${r.chatLieu?.ten || '—'}</div></td>
<td style="text-align:center">
  <span class="badge ${r.trangThai ? 'green' : 'amber'}">
    ${r.trangThai ? 'Hiện' : 'Ẩn'}
  </span>
</td>
<td style="text-align:right;padding-right:16px"></td>
`;

            // click row
            tr.onclick = e => {
                if (e.target.closest('button')) return;
                const isActive = tr.classList.contains('active-row');
                document.querySelectorAll('#tbody tr')
                    .forEach(x => x.classList.remove('active-row'));
                if (!isActive) tr.classList.add('active-row');
            };

            // actions
            const act = tr.lastElementChild;
            act.style.display = 'flex';
            act.style.justifyContent = 'flex-end';
            act.style.gap = '6px';

            const bEdit = document.createElement('button');
            bEdit.className = 'btn icon';
            bEdit.innerHTML = '<i class="bi bi-pencil-fill"></i>';
            bEdit.onclick = e => {
                e.stopPropagation();
                openDrawer('edit', r);
            };

            const bToggle = document.createElement('button');
            bToggle.className = 'btn icon';
            bToggle.innerHTML = '<i class="bi bi-arrow-repeat"></i>';
            bToggle.onclick = e => {
                e.stopPropagation();
                toggleStatus(r);
            };

            const bVariant = document.createElement('button');
            bVariant.className = 'btn icon';
            bVariant.innerHTML = '<i class="bi bi-diagram-3-fill"></i>';
            bVariant.onclick = e => {
                e.stopPropagation();
                goVariants(r);
            };

            act.append(bEdit, bToggle, bVariant);
            frag.appendChild(tr);
        });

        tbody.replaceChildren(frag);
        updatePager();
    }



    function updatePager() {
        _get('#info').textContent = `Hiện${rows.length} / ${totalElements}`;
        _get('#pageKpi').textContent = `${page} / ${totalPages}`;
        _get('#prev').disabled = page <= 1;
        _get('#next').disabled = page >= totalPages;
    }

    /* ================= LOAD ================= */
    function setLoading() {
        const tbody = _get('#tbody');
        let html = '';

        for (let i = 0; i < size; i++) {
            html += `
        <tr class="skeleton-row">
            <td><div class="skeleton sm"></div></td>
            <td><div class="skeleton lg"></div></td>
            <td><div class="skeleton lg"></div></td>
            <td><div class="skeleton md"></div></td>
            <td><div class="skeleton md"></div></td>
            <td><div class="skeleton md"></div></td>
            <td style="text-align:center"><div class="skeleton sm"></div></td>
            <td style="text-align:right"><div class="skeleton sm"></div></td>
        </tr>
        `;
        }

        tbody.innerHTML = html;
    }

    async function loadAndRender() {
        if (isLoading) return;
        isLoading = true;

        setFiltersDisabled(true);
        setLoading();

        await apiList();
        renderTable();

        setFiltersDisabled(false);
        isLoading = false;
    }


    /* ================= DRAWER ================= */
    function openDrawer(mode, row) {
        editingId = mode === 'edit' ? row.id : null;
        _get('#drawerTitle').textContent = mode === 'edit' ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm';

        _get('#drawerBody').innerHTML = `
    <div><label class="label">Mã</label><input id="fMa" class="input"/></div>
    <div><label class="label">Tên *</label><input id="fTen" class="input"/></div>
    <div><label class="label">Danh mục *</label><select id="fDM" class="select"></select></div>
    <div><label class="label">Hãng *</label><select id="fHang2" class="select"></select></div>
    <div><label class="label">Chất liệu *</label><select id="fCL" class="select"></select></div>
    <div><label class="label">Mô tả</label><textarea id="fMoTa" class="textarea"></textarea></div>
    <div><label class="label">Trạng thái</label><input type="checkbox" id="fTrangThai" class="switch"/></div>
  `;

        loadOptions(_get('#fDM'), API.danh_muc);
        loadOptions(_get('#fHang2'), API.hang);
        loadOptions(_get('#fCL'), API.chat_lieu);

        if (mode === 'edit') {
            _get('#fMa').value = row.ma;
            _get('#fMa').disabled = true;
            _get('#fTen').value = row.ten;
            _get('#fMoTa').value = row.moTa || '';
            _get('#fTrangThai').checked = !!row.trangThai;
            setTimeout(() => {
                _get('#fDM').value = row.danhMuc?.id || '';
                _get('#fHang2').value = row.hang?.id || '';
                _get('#fCL').value = row.chatLieu?.id || '';
            }, 200);
        } else {
            _get('#fTrangThai').checked = true;
        }

        _get('#overlay').classList.add('show');
        _get('#drawer').classList.add('show');
    }

    function closeDrawer() {
        editingId = null;
        _get('#overlay').classList.remove('show');
        _get('#drawer').classList.remove('show');
    }

    /* ================= SAVE / ACTION ================= */
    async function saveFromDrawer() {
        const payload = {
            ten: _get('#fTen').value.trim(),
            moTa: _get('#fMoTa').value.trim() || null,
            idDanhMuc: +_get('#fDM').value,
            idHang: +_get('#fHang2').value,
            idChatLieu: +_get('#fCL').value,
            trangThai: _get('#fTrangThai').checked
        };

        const ma = _get('#fMa').value.trim();
        if (!editingId && ma) payload.ma = ma;

        const ok = await uiConfirm(editingId ? 'Cập nhật sản phẩm?' : 'Thêm sản phẩm mới?');
        if (!ok) return;

        editingId ? await apiUpdate(editingId, payload) : await apiCreate(payload);
        toast('Thành công', 'success');
        closeDrawer();
        page = 1;
        loadAndRender();
    }

    async function toggleStatus(row) {
        const ok = await uiConfirm(
            `${row.trangThai ? 'Ẩn' : 'Hiển thị'} "${row.ten}"?`
        );
        if (!ok) return;

        await apiUpdate(row.id, {
            ten: row.ten,
            moTa: row.moTa,
            idDanhMuc: row.danhMuc.id,
            idHang: row.hang.id,
            idChatLieu: row.chatLieu.id,
            trangThai: !row.trangThai
        });

        toast('Đã cập nhật trạng thái', 'success');
        loadAndRender();
    }



    /* ================= VARIANTS ================= */
    function goVariants(product) {
        selectedProduct = product;
        activeTab = 'variants';

        _get('#subHeader').style.display = '';
        _get('#selProduct').textContent = `SP: ${product.ma} - ${product.ten}`;

        document.querySelectorAll('.tab')
            .forEach(t => t.classList.toggle('active', t.dataset.tab === 'variants'));

        _get('#thead').innerHTML = '';
        _get('#tbody').innerHTML = `
    <tr>
      <td colspan="8" style="height:360px;text-align:center">
        Biến thể của <b>${product.ten}</b>
      </td>
    </tr>`;
    }

    /* ================= INIT ================= */
    function init() {
        document.querySelectorAll('.tab').forEach(tab => {
            tab.onclick = () => {
                const key = tab.dataset.tab;
                if ((key === 'variants' || key === 'images') && !selectedProduct) {
                    toast('Vui lòng chọn sản phẩm trước', 'info');
                    return;
                }
                activeTab = key;
                document.querySelectorAll('.tab')
                    .forEach(t => t.classList.toggle('active', t.dataset.tab === activeTab));
                if (key === 'products') {
                    selectedProduct = null;
                    _get('#subHeader').style.display = 'none';
                    loadAndRender();
                }
                if (key === 'variants') goVariants(selectedProduct);
            };
        });

        _get('#search').style.flex = '3';

        ['#fDanhMuc', '#fHang', '#fChatLieu', '#fTrangThaiFilter']
            .forEach(s => {
                if (_get(s)) _get(s).style.flex = '1';
            });

        _get('#btnAdd').onclick = () => openDrawer('create');
        _get('#btnSave').onclick = saveFromDrawer;
        _get('#btnClose').onclick = closeDrawer;
        _get('#overlay').onclick = closeDrawer;
        _get('#prev').onclick = () => {
            if (page > 1) {
                page--;
                loadAndRender();
            }
        };
        _get('#next').onclick = () => {
            if (page < totalPages) {
                page++;
                loadAndRender();
            }
        };

        loadOptions(_get('#fDanhMuc'), API.danh_muc);
        loadOptions(_get('#fHang'), API.hang);
        loadOptions(_get('#fChatLieu'), API.chat_lieu);

        loadAndRender();
    }

    init();
    let debounce;
    _get('#search').addEventListener('input', e => {
        q = e.target.value.trim();
        if (q.length === 1) return; // ❌ chặn
        page = 1;
        clearTimeout(debounce);
        debounce = setTimeout(loadAndRender, 400);
    });
    ['#fDanhMuc', '#fHang', '#fChatLieu', '#fTrangThaiFilter']
        .forEach(s => {
            const el = _get(s);
            if (el) {
                el.addEventListener('change', () => {
                    page = 1;
                    loadAndRender();
                });
            }
        });

    function setFiltersDisabled(disabled) {
        ['#search','#fDanhMuc','#fHang','#fChatLieu','#fTrangThaiFilter']
            .forEach(s => {
                const el = _get(s);
                if (el) el.disabled = disabled;
            });
    }
    setFiltersDisabled(true);
// load
    setFiltersDisabled(false);
}
