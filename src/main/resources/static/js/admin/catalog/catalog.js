window.toast = function (message, type = 'success') {
    let container = document.getElementById('beeToastHost');
    if (!container) {
        container = document.createElement('div');
        container.id = 'beeToastHost';
        document.body.appendChild(container);
    }

    const el = document.createElement('div');
    el.className = `bee-toast-box ${type}`;
    const prefix = type === 'success' ? 'THÀNH CÔNG' : (type === 'error' ? 'LỖI' : 'THÔNG BÁO');
    const color = type === 'success' ? '#10b981' : (type === 'error' ? '#ef4444' : '#f59e0b');
    const icon = type === 'success' ? 'bi-check-circle-fill' : (type === 'error' ? 'bi-x-circle-fill' : 'bi-exclamation-triangle-fill');

    el.innerHTML = `
        <i class="bi ${icon}" style="color:${color}; font-size:22px; margin-top:2px;"></i>
        <div style="display: flex; flex-direction: column; flex: 1;">
            <span style="font-weight:800; font-size:12px; text-transform: uppercase; margin-bottom: 4px; color:${color}">${prefix}</span>
            <span style="font-weight:500; font-size:13px; color:#333; line-height: 1.4;">${message}</span>
        </div>
        <i class="bi bi-x-lg" style="cursor: pointer; margin-left: 10px; font-size: 18px; color:#999; transition: 0.2s;" onmouseover="this.style.color='#000'" onmouseout="this.style.color='#999'" onclick="this.parentElement.remove()"></i>
    `;
    container.appendChild(el);

    setTimeout(() => {
        if (el.parentNode) {
            el.classList.add('hiding');
            setTimeout(() => el.remove(), 300);
        }
    }, 3500);
};

const CatalogApp = {
    state: {
        activeTab: 'danh-muc', hasDesc: true, page: 0, size: 10, totalPages: 0,
        q: '', trangThai: '', editingId: null
    },

    init: function () {
        this.bindEvents();
        this.loadData();
    },

    bindEvents: function () {
        const self = this;
        document.querySelectorAll('.tab').forEach(tab => {
            tab.onclick = function () {
                if (self.state.activeTab === this.dataset.tab) return;
                document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
                this.classList.add('active');
                self.state.activeTab = this.dataset.tab;
                self.state.hasDesc = (this.dataset.hasDesc === 'true');
                self.state.page = 0;
                self.state.q = '';
                self.state.trangThai = '';
                document.getElementById('search').value = '';
                document.getElementById('fTrangThaiFilter').value = '';
                self.loadData();
            };
        });
        document.getElementById('btnAdd').onclick = () => self.openDrawer('create');
        document.getElementById('btnSave').onclick = () => self.saveItem();
        document.getElementById('btnClose').onclick = () => self.closeDrawer();
        document.getElementById('overlay').onclick = () => self.closeDrawer();

        document.getElementById('fTrangThaiFilter').onchange = (e) => {
            self.state.trangThai = e.target.value;
            self.state.page = 0;
            self.loadData();
        };
    },

    getTabName: function () {
        const map = { 'danh-muc': 'Danh mục', 'hang': 'Thương hiệu', 'chat-lieu': 'Chất liệu', 'mau-sac': 'Màu sắc', 'kich-thuoc': 'Kích thước' };
        return map[this.state.activeTab] || 'Dữ liệu';
    },

    handleSearch: function (val) {
        clearTimeout(this.timeout);
        this.timeout = setTimeout(() => {
            this.state.q = val.trim();
            this.state.page = 0;
            this.loadData();
        }, 400);
    },

    loadData: async function () {
        this.renderHeader();
        const {activeTab, page, size, q, trangThai} = this.state;
        const url = `/api/${activeTab}?page=${page}&size=${size}&q=${q}&trangThai=${trangThai}`;
        try {
            const res = await fetch(url);
            const data = await res.json();
            this.renderTable(data.content || []);
            this.renderPagination(data);
        } catch (err) { console.error(err); }
    },

    renderHeader: function () {
        const extraCols = this.state.hasDesc ? `<th>MÔ TẢ</th><th>NGÀY TẠO</th>` : '';
        document.getElementById('thead').innerHTML = `
                <tr>
                    <th style="width:50px; text-align:center;">STT</th>
                    <th style="width:120px">MÃ THUỘC TÍNH</th>
                    <th>TÊN THUỘC TÍNH</th>
                    ${extraCols} <th style="text-align:center">TRẠNG THÁI</th>
                    <th style="text-align:right">THAO TÁC</th>
                </tr>`;
    },

    renderTable: function (list) {
        const tbody = document.getElementById('tbody');
        const colSpan = this.state.hasDesc ? 7 : 5;
        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="${colSpan}" class="empty-state">Không tìm thấy dữ liệu phù hợp</td></tr>`;
            return;
        }
        tbody.innerHTML = list.map((item, index) => {
            const stt = (this.state.page * this.state.size) + index + 1;
            let extraCells = '';
            if (this.state.hasDesc) {
                const moTa = item.moTa || '—';
                const ngayTao = item.ngayTao ? new Date(item.ngayTao).toLocaleDateString('vi-VN') : '—';
                extraCells = `<td style="color:#555; max-width:250px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${moTa}</td>
                              <td style="font-size:12px; color:#888">${ngayTao}</td>`;
            }
            const toggleHtml = `
                    <label class="switch-sm">
                        <input type="checkbox" ${item.trangThai ? 'checked' : ''} onchange="window.CatalogApp.quickToggle(${item.id}, this)">
                        <span class="slider-sm"></span>
                    </label>
                `;
            return `<tr>
                    <td style="text-align:center;">${stt}</td>
                    <td style="font-weight:600; color:#555;">${item.ma || '—'}</td>
                    <td style="font-weight:700; color:#000;">${item.ten || '—'}</td>
                    ${extraCells}
                    <td style="text-align:center;">${toggleHtml}</td>
                    <td style="text-align:right">
                        <button class="btn-icon view" title="Xem chi tiết" onclick="CatalogApp.openDetail(${item.id})"><i class="bi bi-eye"></i></button>
                        <button class="btn-icon" title="Chỉnh sửa" onclick="CatalogApp.openEdit(${item.id})"><i class="bi bi-pencil-square"></i></button>
                    </td>
                </tr>`;
        }).join('');
    },

    quickToggle: async function (id, el) {
        const originalState = !el.checked;
        const actionText = el.checked ? "Kích hoạt" : "Ngừng hoạt động";
        const name = this.getTabName();
        this.toastConfirm(
            `Xác nhận ${actionText}?`,
            `Bạn có chắc chắn muốn ${actionText.toLowerCase()} ${name.toLowerCase()} này không?`,
            async () => {
                const parent = el.closest('.switch-sm');
                try {
                    parent.classList.add('loading');
                    const res = await fetch(`/api/${this.state.activeTab}/${id}/trang-thai`, {method: 'PATCH'});
                    if (res.ok) {
                        window.toast(`Đã ${actionText.toLowerCase()} thành công.`, 'success');
                    } else {
                        const err = await res.json();
                        window.toast(err.message || "Lỗi cập nhật.", 'error');
                        el.checked = originalState;
                    }
                } catch (e) {
                    window.toast("Mất kết nối server!", 'error');
                    el.checked = originalState;
                } finally {
                    parent.classList.remove('loading');
                }
            },
            () => { el.checked = originalState; }
        );
    },

    toastConfirm: function (title, message, onConfirm, onCancel) {
        let container = document.getElementById('beeToastHost');
        if (!container) {
            container = document.createElement('div');
            container.id = 'beeToastHost';
            document.body.appendChild(container);
        }

        const el = document.createElement('div');
        el.className = `bee-toast-box warning`;
        el.style.flexDirection = 'column';
        el.style.alignItems = 'stretch';

        el.innerHTML = `
            <div style="display: flex; gap: 12px; align-items: flex-start; width: 100%;">
                <i class="bi bi-question-circle-fill" style="color:#f59e0b; font-size:24px; margin-top:2px;"></i>
                <div style="display:flex; flex-direction:column; flex:1;">
                    <span style="font-weight:800; font-size:13px; text-transform:uppercase; margin-bottom:6px; color:#000;">${title}</span>
                    <span style="font-weight:500; font-size:13px; color:#555; line-height: 1.5;">${message}</span>
                </div>
            </div>
            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 10px; border-top: 1px dashed #eee; padding-top: 12px;">
                <button class="btn btn-secondary" id="toastCancel" style="height: 32px; font-size: 11px; padding: 0 16px; border-radius: 4px;">HỦY BỎ</button>
                <button class="btn" id="toastOk" style="height: 32px; font-size: 11px; padding: 0 16px; border-radius: 4px;">XÁC NHẬN</button>
            </div>
        `;
        container.appendChild(el);

        el.querySelector('#toastOk').onclick = () => {
            el.classList.add('hiding');
            setTimeout(() => el.remove(), 300);
            if(onConfirm) onConfirm();
        };
        el.querySelector('#toastCancel').onclick = () => {
            el.classList.add('hiding');
            setTimeout(() => el.remove(), 300);
            if(onCancel) onCancel();
        };
    },

    renderPagination: function (data) {
        this.state.totalPages = data.totalPages || 0;
        const container = document.getElementById('paginationContainer');

        if (this.state.totalPages <= 0) {
            container.innerHTML = '';
            return;
        }

        let html = `<div class="pagination-info">Hiển thị ${data.numberOfElements}/${data.totalElements} bản ghi</div><div class="pagination">`;

        html += `<button class="page-number" ${this.state.page === 0 ? 'disabled' : ''} onclick="CatalogApp.changePage(${this.state.page - 1})"><i class="bi bi-chevron-left"></i></button>`;

        const current = this.state.page + 1;
        const last = this.state.totalPages;
        const delta = 1;
        const left = current - delta;
        const right = current + delta;
        const range = [];
        const rangeWithDots = [];
        let l;

        for (let i = 1; i <= last; i++) {
            if (i === 1 || i === last || (i >= left && i <= right)) range.push(i);
        }

        for (let i of range) {
            if (l) {
                if (i - l === 2) { rangeWithDots.push(l + 1); }
                else if (i - l !== 1) { rangeWithDots.push('...'); }
            }
            rangeWithDots.push(i);
            l = i;
        }

        for (let i of rangeWithDots) {
            if (i === '...') {
                html += `<span class="page-number" style="border:none; background:transparent; pointer-events:none; cursor:default; min-width: 15px;">...</span>`;
            } else {
                const pageIndex = i - 1;
                const activeClass = pageIndex === this.state.page ? 'active' : '';
                html += `<button class="page-number ${activeClass}" onclick="CatalogApp.changePage(${pageIndex})">${i}</button>`;
            }
        }

        html += `<button class="page-number" ${this.state.page === this.state.totalPages - 1 ? 'disabled' : ''} onclick="CatalogApp.changePage(${this.state.page + 1})"><i class="bi bi-chevron-right"></i></button></div>`;

        container.innerHTML = html;
    },

    changePage: function(p) {
        if (p < 0 || p >= this.state.totalPages) return;
        this.state.page = p;
        this.loadData();
    },

    openDrawer: function (mode, item) {
        this.state.editingId = item ? item.id : null;
        const titleEl = document.getElementById('drawerTitle');
        const btnSave = document.getElementById('btnSave');
        const fMa = document.getElementById('fMa');
        const fTen = document.getElementById('fTen');
        const fMoTa = document.getElementById('fMoTa');
        const fTrangThai = document.getElementById('fTrangThai');
        const groupFullDetail = document.getElementById('groupFullDetail');

        if (mode === 'create') {
            titleEl.innerHTML = '<i class="bi bi-plus-circle" style="color:var(--black); margin-right:8px;"></i>THÊM MỚI';
            btnSave.style.display = 'block';
            groupFullDetail.style.display = 'none';
            fMa.value = ''; fMa.disabled = false;
            fTen.value = ''; fTen.disabled = false;
            fMoTa.value = ''; fMoTa.disabled = false;
            fTrangThai.checked = true; fTrangThai.disabled = false;
        } else if (mode === 'edit') {
            titleEl.innerHTML = '<i class="bi bi-pencil-square" style="color:var(--black); margin-right:8px;"></i>CẬP NHẬT';
            btnSave.style.display = 'block';
            groupFullDetail.style.display = 'none';
            fMa.value = item.ma || ''; fMa.disabled = true;
            fTen.value = item.ten || ''; fTen.disabled = false;
            fMoTa.value = item.moTa || ''; fMoTa.disabled = false;
            fTrangThai.checked = item.trangThai; fTrangThai.disabled = false;
        } else if (mode === 'view') {
            titleEl.innerHTML = '<i class="bi bi-eye" style="color:var(--black); margin-right:8px;"></i>CHI TIẾT';
            btnSave.style.display = 'none';
            groupFullDetail.style.display = (this.state.hasDesc) ? 'block' : 'none';
            fMa.value = item.ma || ''; fMa.disabled = true;
            fTen.value = item.ten || ''; fTen.disabled = true;
            fMoTa.value = item.moTa || ''; fMoTa.disabled = true;
            fTrangThai.checked = item.trangThai; fTrangThai.disabled = true;
            if (this.state.hasDesc) {
                document.getElementById('fNgayTao').value = item.ngayTao ? new Date(item.ngayTao).toLocaleString('vi-VN') : '';
                document.getElementById('fNgaySua').value = item.ngaySua ? new Date(item.ngaySua).toLocaleString('vi-VN') : '';
            }
        }
        if (document.getElementById('groupMoTa')) {
            document.getElementById('groupMoTa').style.display = this.state.hasDesc ? 'block' : 'none';
        }
        document.getElementById('overlay').classList.add('show');
        document.getElementById('drawer').classList.add('show');
    },

    closeDrawer: function () {
        document.getElementById('overlay').classList.remove('show');
        document.getElementById('drawer').classList.remove('show');
    },

    openEdit: async function (id) {
        try {
            const res = await fetch(`/api/${this.state.activeTab}/${id}`);
            const item = await res.json();
            this.openDrawer('edit', item);
        } catch (e) { console.error(e); }
    },

    openDetail: async function (id) {
        try {
            const res = await fetch(`/api/${this.state.activeTab}/${id}`);
            const item = await res.json();
            this.openDrawer('view', item);
        } catch (e) { console.error(e); }
    },

    saveItem: function () {
        const ten = document.getElementById('fTen').value.trim();
        if (!ten) {
            window.toast('Tên không được để trống!', 'error');
            document.getElementById('fTen').focus();
            return;
        }

        const name = this.getTabName();
        const actionTitle = this.state.editingId ? 'Cập nhật thông tin' : 'Thêm mới bản ghi';
        const actionDesc = `Bạn có chắc chắn muốn lưu thông tin ${name.toLowerCase()} này vào hệ thống?`;

        this.toastConfirm(actionTitle, actionDesc, async () => {
            const payload = {ten: ten, trangThai: document.getElementById('fTrangThai').checked};
            const fMa = document.getElementById('fMa');
            if (fMa && fMa.value.trim()) payload.ma = fMa.value.trim();
            if (this.state.hasDesc) payload.moTa = document.getElementById('fMoTa').value.trim();

            const id = this.state.editingId;
            const url = id ? `/api/${this.state.activeTab}/${id}` : `/api/${this.state.activeTab}`;

            try {
                const res = await fetch(url, {
                    method: id ? 'PUT' : 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(payload)
                });
                if (!res.ok) {
                    const err = await res.json();
                    throw new Error(err.message || "Có lỗi xảy ra, vui lòng thử lại.");
                }

                if (id) window.toast(`Cập nhật ${name.toLowerCase()} thành công!`, 'success');
                else window.toast(`Thêm mới ${name.toLowerCase()} thành công!`, 'success');

                this.closeDrawer();
                this.loadData();
            } catch (e) {
                window.toast(e.message, 'error');
            }
        });
    }
};

window.CatalogApp = CatalogApp;
window.initCatalogs = function () {
    CatalogApp.init();
};