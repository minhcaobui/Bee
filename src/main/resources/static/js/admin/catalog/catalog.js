window.toast = function (message, type = 'success') {
    const container = document.getElementById('toastHost');
    if (!container) return;
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    const prefix = type === 'success' ? 'THÀNH CÔNG: ' : (type === 'error' ? 'LỖI: ' : 'THÔNG BÁO: ');
    el.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: flex-start; width: 100%;">
            <div style="display: flex; flex-direction: column;">
                <span style="font-weight:700; font-size:11px; text-transform: uppercase; margin-bottom: 2px;">${prefix}</span>
                <span style="font-weight:500; font-size:13px;">${message}</span>
            </div>
            <i class="bi bi-x-lg" style="cursor: pointer; margin-left: 10px; font-size: 14px; margin-top: 2px;" onclick="this.parentElement.parentElement.parentElement.remove()"></i>
        </div>
    `;
    container.appendChild(el);
    setTimeout(() => {
        if (el.parentNode) {
            el.classList.add('hiding');
            el.addEventListener('transitionend', () => el.remove());
        }
    }, 3500);
};
const CatalogApp = {
    state: {
        activeTab: 'danh-muc',
        hasDesc: true,
        page: 0,
        size: 10,
        totalPages: 0,
        q: '',
        trangThai: '',
        editingId: null
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
        document.getElementById('prev').onclick = () => {
            if (self.state.page > 0) {
                self.state.page--;
                self.loadData();
            }
        };
        document.getElementById('next').onclick = () => {
            if (self.state.page < self.state.totalPages - 1) {
                self.state.page++;
                self.loadData();
            }
        };
        document.getElementById('fTrangThaiFilter').onchange = (e) => {
            self.state.trangThai = e.target.value;
            self.state.page = 0;
            self.loadData();
        };
    },
    getTabName: function () {
        const map = {
            'danh-muc': 'danh mục',
            'hang': 'thương hiệu',
            'chat-lieu': 'chất liệu',
            'mau-sac': 'màu sắc',
            'kich-thuoc': 'kích thước'
        };
        return map[this.state.activeTab] || 'dữ liệu';
    },
    handleSearch: function (val) {
        clearTimeout(this.timeout);
        this.timeout = setTimeout(() => {
            this.state.q = val.trim();
            this.state.page = 0;
            this.loadData();
        }, 500);
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
        } catch (err) {
            console.error(err);
        }
    },
    renderHeader: function () {
        const extraCols = this.state.hasDesc ? `<th>MÔ TẢ</th><th>NGÀY TẠO</th>` : '';
        document.getElementById('thead').innerHTML = `
                <tr>
                    <th style="width:50px; text-align:center;">STT</th>
                    <th style="width:80px">MÃ</th>
                    <th>TÊN</th>
                    ${extraCols} <th style="text-align:center">TRẠNG THÁI</th>
                    <th style="text-align:right">THAO TÁC</th>
                </tr>`;
    },
    renderTable: function (list) {
        const tbody = document.getElementById('tbody');
        const colSpan = this.state.hasDesc ? 7 : 5;
        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="${colSpan}" class="empty-state">Không có dữ liệu</td></tr>`;
            return;
        }
        tbody.innerHTML = list.map((item, index) => {
            const stt = (this.state.page * this.state.size) + index + 1;
            let extraCells = '';
            if (this.state.hasDesc) {
                const moTa = item.moTa || '—';
                const ngayTao = item.ngayTao ? new Date(item.ngayTao).toLocaleDateString('vi-VN') : '—';
                extraCells = `<td style="color:#666; max-width:200px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${moTa}</td>
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
                    <td>${item.ma || '—'}</td>
                    <td style="font-weight:600">${item.ten || '—'}</td>
                    ${extraCells}
                    <td style="text-align:center;">${toggleHtml}</td>
                    <td style="text-align:right">
                        <button class="btn-icon view" onclick="CatalogApp.openDetail(${item.id})"><i class="bi bi-eye"></i></button>
                        <button class="btn-icon" onclick="CatalogApp.openEdit(${item.id})"><i class="bi bi-pencil"></i></button>
                    </td>
                </tr>`;
        }).join('');
    },
    quickToggle: async function (id, el) {
        const originalState = !el.checked;
        const actionText = el.checked ? "kích hoạt" : "ngừng hoạt động";
        const name = this.getTabName();
        this.toastConfirm(
            `Bạn có chắc chắn muốn ${actionText} ${name} này không?`,
            async () => {
                const parent = el.closest('.switch-sm');
                try {
                    parent.classList.add('loading');
                    const res = await fetch(`/api/${this.state.activeTab}/${id}/trang-thai`, {method: 'PATCH'});
                    if (res.ok) {
                        window.toast(`Cập nhật trạng thái thành công.`, 'success');
                    } else {
                        const err = await res.json();
                        window.toast(err.message || "Lỗi cập nhật.", 'error');
                        el.checked = originalState; // Rollback
                    }
                } catch (e) {
                    window.toast("Mất kết nối server!", 'error');
                    el.checked = originalState;
                } finally {
                    parent.classList.remove('loading');
                }
            },
            () => {
                el.checked = originalState;
            }
        );
    },
    toastConfirm: function (message, onConfirm, onCancel) {
        const container = document.getElementById('toastHost');
        if (!container) return;
        const el = document.createElement('div');
        el.className = `toast warning`;
        el.style.flexDirection = 'column';
        el.style.alignItems = 'flex-start';
        el.style.gap = '10px';
        el.innerHTML = `
                <div style="display: flex; gap: 10px; align-items: center;">
                    <i class="bi bi-exclamation-circle-fill" style="color:#f59e0b; font-size:18px;"></i>
                    <span style="font-weight:600; font-size:13px;">${message}</span>
                </div>
                <div style="display: flex; gap: 8px; width: 100%; justify-content: flex-end;">
                    <button class="btn btn-secondary" id="toastCancel" style="height: 24px; font-size: 10px; padding: 0 10px;">HỦY</button>
                    <button class="btn" id="toastOk" style="height: 24px; font-size: 10px; padding: 0 10px;">XÁC NHẬN</button>
                </div>
            `;
        container.appendChild(el);
        el.querySelector('#toastOk').onclick = () => {
            el.remove();
            onConfirm();
        };
        el.querySelector('#toastCancel').onclick = () => {
            el.remove();
            onCancel();
        };
    },
    renderPagination: function (data) {
        this.state.totalPages = data.totalPages;
        document.getElementById('info').textContent = `${data.numberOfElements}/${data.totalElements} bản ghi`;
        document.getElementById('prev').disabled = data.first;
        document.getElementById('next').disabled = data.last;
        const pageNumbers = document.getElementById('pageNumbers');
        pageNumbers.innerHTML = '';
        for (let i = 0; i < data.totalPages; i++) {
            if (i === 0 || i === data.totalPages - 1 || (i >= this.state.page - 1 && i <= this.state.page + 1)) {
                const btn = document.createElement('button');
                btn.className = `page-number ${i === this.state.page ? 'active' : ''}`;
                btn.textContent = i + 1;
                btn.onclick = () => {
                    this.state.page = i;
                    this.loadData();
                };
                pageNumbers.appendChild(btn);
            }
        }
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
            titleEl.textContent = 'THÊM MỚI';
            btnSave.style.display = 'block';
            groupFullDetail.style.display = 'none';
            fMa.value = '';
            fMa.disabled = false;
            fTen.value = '';
            fTen.disabled = false;
            fMoTa.value = '';
            fMoTa.disabled = false;
            fTrangThai.checked = true;
            fTrangThai.disabled = false;
        } else if (mode === 'edit') {
            titleEl.textContent = 'CẬP NHẬT';
            btnSave.style.display = 'block';
            groupFullDetail.style.display = 'none';
            fMa.value = item.ma || '';
            fMa.disabled = true;
            fTen.value = item.ten || '';
            fTen.disabled = false;
            fMoTa.value = item.moTa || '';
            fMoTa.disabled = false;
            fTrangThai.checked = item.trangThai;
            fTrangThai.disabled = false;
        } else if (mode === 'view') {
            titleEl.textContent = 'CHI TIẾT';
            btnSave.style.display = 'none';
            groupFullDetail.style.display = (this.state.hasDesc) ? 'block' : 'none';
            fMa.value = item.ma || '';
            fMa.disabled = true;
            fTen.value = item.ten || '';
            fTen.disabled = true;
            fMoTa.value = item.moTa || '';
            fMoTa.disabled = true;
            fTrangThai.checked = item.trangThai;
            fTrangThai.disabled = true;
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
        } catch (e) {
            console.error(e);
        }
    },
    openDetail: async function (id) {
        try {
            const res = await fetch(`/api/${this.state.activeTab}/${id}`);
            const item = await res.json();
            this.openDrawer('view', item);
        } catch (e) {
            console.error(e);
        }
    },
    saveItem: async function () {
        const ten = document.getElementById('fTen').value.trim();
        if (!ten) {
            window.toast('Tên không được để trống', 'error');
            return;
        }
        const payload = {ten: ten, trangThai: document.getElementById('fTrangThai').checked};
        const fMa = document.getElementById('fMa');
        if (fMa && fMa.value.trim()) payload.ma = fMa.value.trim();
        if (this.state.hasDesc) payload.moTa = document.getElementById('fMoTa').value.trim();
        const id = this.state.editingId;
        const url = id ? `/api/${this.state.activeTab}/${id}` : `/api/${this.state.activeTab}`;
        const name = this.getTabName();
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
            if (id) window.toast(`Cập nhật thông tin ${name} thành công!`, 'success');
            else window.toast(`Thêm mới ${name} vào hệ thống thành công!`, 'success');
            this.closeDrawer();
            this.loadData();
        } catch (e) {
            window.toast(e.message, 'error');
        }
    }
};
window.CatalogApp = CatalogApp;
window.initCatalogs = function () {
    CatalogApp.init();
};
CatalogApp.init();