/* =======================================================
 * File: catalog.js (Module Quản lý Danh mục)
 * FIX LỖI MẤT TABS: Tự định nghĩa hàm selector _get
 * =======================================================
 */

/* ================= API CONFIG ================= */
{
const API_CATA = {
    danh_muc: '/api/danh-muc',
    hang: '/api/hang',
    chat_lieu: '/api/chat-lieu',
    kich_thuoc: '/api/kich-thuoc',
    mau_sac: '/api/mau-sac',
};

const TABS = [
    { key: 'danh_muc',  label: 'Danh mục',  showMoTa: true,  showTrangThai: true,  requireMa: false },
    { key: 'hang',      label: 'Hãng',      showMoTa: true,  showTrangThai: true,  requireMa: false },
    { key: 'chat_lieu', label: 'Chất liệu', showMoTa: true,  showTrangThai: true,  requireMa: false },
    { key: 'kich_thuoc',label: 'Size',      showMoTa: false, showTrangThai: false, requireMa: true  },
    { key: 'mau_sac',   label: 'Màu',       showMoTa: false, showTrangThai: false, requireMa: true  },
];

const CHECK_DUP_TEN = true;

/* ================= STATE ================= */
let cata_active = 'danh_muc';
let cata_q = '';
let cata_page = 1;
const cata_size = 5;
let cata_rows = [];
let cata_totalElements = 0;
let cata_totalPages = 1;
let cata_editingId = null;

/* ================= HELPERS (ĐÃ SỬA ĐỂ KHÔNG PHỤ THUỘC $) ================= */
// FIX LỖI TẠI ĐÂY: Tự định nghĩa hàm selector để chắc chắn tìm được #tabs
const _get = (s) => document.querySelector(s);

function endpoint(){ return API_CATA[cata_active]; }

async function apiList(){
    const base = endpoint();
    const url = `${base}?page=${cata_page-1}&size=${cata_size}${cata_q?`&q=${encodeURIComponent(cata_q)}`:''}`;
    // request là Global Function, thường sẽ OK. Nếu lỗi, báo tao.
    const data = await request(url);
    if(data && Array.isArray(data.content)){
        cata_rows = data.content; cata_totalElements = data.totalElements ?? cata_rows.length; cata_totalPages = data.totalPages ?? 1;
    } else if(Array.isArray(data)){
        cata_rows = data; cata_totalElements = data.length; cata_totalPages = Math.max(1, Math.ceil(cata_totalElements/cata_size));
    } else { cata_rows = []; cata_totalElements = 0; cata_totalPages = 1; }
}

async function apiCreate(payload){ return request(endpoint(), { method:'POST', body: JSON.stringify(payload) }); }
async function apiUpdate(id, payload){ return request(`${endpoint()}/${id}`, { method:'PUT', body: JSON.stringify(payload) }); }
async function apiDelete(id){ return request(`${endpoint()}/${id}`, { method:'DELETE' }); }

// Check trùng
function isDupLocal(field, value, excludeId){
    if(!value) return false;
    const v = String(value).toLowerCase();
    return cata_rows.some(x => String(x[field]||'').toLowerCase() === v && x.id !== excludeId);
}
async function isDupRemote(field, value, excludeId){
    if(!value) return false;
    const base = endpoint();
    const url = `${base}?page=0&size=5&q=${encodeURIComponent(value)}`;
    try{
        const data = await request(url);
        const list = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : []);
        const v = String(value).toLowerCase();
        return list.some(x => String(x[field]||'').toLowerCase() === v && x.id !== excludeId);
    }catch(e){ return false; }
}
async function checkDuplicateAll(payload, excludeId){
    if(payload.ma){
        if(isDupLocal('ma', payload.ma, excludeId) || await isDupRemote('ma', payload.ma, excludeId)){
            toast('Mã đã tồn tại', 'error'); return false;
        }
    }
    if(CHECK_DUP_TEN && payload.ten){
        if(isDupLocal('ten', payload.ten, excludeId) || await isDupRemote('ten', payload.ten, excludeId)){
            toast('Tên đã tồn tại', 'error'); return false;
        }
    }
    return true;
}

/* ================= UI RENDER ================= */
function renderTabs(){
    const wrap = _get('#tabs');
    if (!wrap) { console.error("Không tìm thấy #tabs"); return; } // Debug log

    wrap.innerHTML='';
    TABS.forEach(t=>{
        const b=document.createElement('button');
        b.className='tab'+(t.key===cata_active?' active':'');
        b.textContent=t.label;
        b.onclick=()=>{ cata_active=t.key; cata_page=1; loadAndRender(); };
        wrap.appendChild(b);
    });
}

function visibleCols(){
    const cfg = TABS.find(t=>t.key===cata_active);
    return 3 + (cfg.showMoTa?1:0) + (cfg.showTrangThai?1:0);
}

function renderTable(){
    const tbody = _get('#tbody');
    if (!tbody) return;

    const cfg = TABS.find(t=>t.key===cata_active);
    if(_get('#thMoTa')) _get('#thMoTa').style.display = cfg.showMoTa ? '' : 'none';
    if(_get('#thTrangThai')) _get('#thTrangThai').style.display = cfg.showTrangThai ? '' : 'none';

    tbody.innerHTML='';
    if(cata_rows.length===0){
        const tr=document.createElement('tr');
        const td=document.createElement('td');
        td.colSpan = visibleCols();
        td.className='muted'; td.style.textAlign='center'; td.style.padding='28px 12px';
        td.textContent='Không có dữ liệu.';
        tr.appendChild(td); tbody.appendChild(tr);
    } else {
        cata_rows.forEach(row=>{
            const tr=document.createElement('tr');
            const tdMa=document.createElement('td'); tdMa.textContent=row.ma; tr.appendChild(tdMa);
            const tdTen=document.createElement('td'); tdTen.textContent=row.ten; tr.appendChild(tdTen);
            if(cfg.showMoTa){ const tdMoTa=document.createElement('td'); tdMoTa.textContent=row.moTa||'—'; tr.appendChild(tdMoTa); }
            if(cfg.showTrangThai){
                const tdTT=document.createElement('td'); tdTT.style.textAlign='center';
                const span=document.createElement('span'); const on = !!row.trangThai;
                span.className = 'badge ' + (on ? 'green':'amber'); span.textContent = on ? 'Hiển thị' : 'Ẩn';
                tdTT.appendChild(span); tr.appendChild(tdTT);
            }
            const tdAct=document.createElement('td'); tdAct.style.textAlign='right'; tdAct.style.paddingRight='16px';
            const bEdit=document.createElement('button'); bEdit.className='btn icon'; bEdit.textContent='Sửa'; bEdit.onclick=()=>openDrawer('edit', row);
            const bToggle=document.createElement('button'); bToggle.className='btn icon'; bToggle.textContent='Ẩn/Hiện'; bToggle.onclick=()=>toggleStatus(row);

            if(cfg.showTrangThai) tdAct.append(bEdit, bToggle); else tdAct.append(bEdit);
            tr.appendChild(tdAct); tbody.appendChild(tr);
        });
    }

    if(_get('#info')) _get('#info').textContent = `Hiển thị ${cata_rows.length} / ${cata_totalElements} bản ghi`;
    if(_get('#pageKpi')) _get('#pageKpi').textContent = `${cata_page} / ${cata_totalPages}`;
    if(_get('#prev')) _get('#prev').disabled = cata_page<=1;
    if(_get('#next')) _get('#next').disabled = cata_page>=cata_totalPages;
}

function setLoading(on){
    const tbody = _get('#tbody');
    if(on && tbody){ tbody.innerHTML = `<tr><td colspan="${visibleCols()}" style="text-align:center;padding:24px"><span class="spin"></span> Đang tải…</td></tr>`; }
}

async function loadAndRender(){
    renderTabs();
    setLoading(true);
    try{ await apiList(); }catch(e){ cata_rows=[]; cata_totalElements=0; cata_totalPages=1; toast(e.message, 'error', 'Lỗi'); }
    renderTable();
}

// ================= Drawer logic =================
function openDrawer(mode, row){
    const cfg = TABS.find(t=>t.key===cata_active);
    _get('#drawerTitle').textContent = (mode==='edit'?'Cập nhật':'Thêm mới')+ ' — ' + cfg.label;
    _get('#statusRow').style.display = cfg.showTrangThai ? '' : 'none';
    _get('#moTaRow').style.display = cfg.showMoTa ? '' : 'none';

    _get('#reqMa').style.display = cfg.requireMa ? '' : 'none';
    _get('#fMa').placeholder = cfg.requireMa ? 'Bắt buộc' : 'Để trống nếu muốn sinh tự động (tuỳ API)';

    cata_editingId = mode==='edit' ? row.id : null;
    _get('#drawer').dataset.mode = mode;

    _get('#fMa').value = row?.ma || '';
    _get('#fTen').value = row?.ten || '';
    _get('#fMoTa').value = row?.moTa || '';
    _get('#fTrangThai').checked = (row?.trangThai ?? true);

    _get('#overlay').classList.add('show'); _get('#drawer').classList.add('show');
}
function closeDrawer(){ _get('#overlay').classList.remove('show'); _get('#drawer').classList.remove('show'); cata_editingId = null; }

async function saveFromDrawer(){
    const mode = _get('#drawer').dataset.mode || 'create';
    const cfg = TABS.find(t=>t.key===cata_active);
    const payload = { ma: (_get('#fMa').value||'').trim() || undefined, ten: (_get('#fTen').value||'').trim() };
    if(cfg.showMoTa) payload.moTa = (_get('#fMoTa').value||'').trim() || null;
    if(cfg.showTrangThai) payload.trangThai = _get('#fTrangThai').checked;

    if(!payload.ten){ toast('Tên là bắt buộc', 'error'); return; }
    if(cfg.requireMa && !payload.ma){ toast('Mã là bắt buộc cho Size và Màu', 'error'); return; }

    const toCheck = { ...payload };
    if(cata_editingId!=null){
        const current = cata_rows.find(x=>x.id===cata_editingId) || {};
        toCheck.ma = toCheck.ma ?? current.ma;
        toCheck.ten = toCheck.ten ?? current.ten;
    }
    const dupOk = await checkDuplicateAll(toCheck, cata_editingId);
    if(!dupOk) return;

    const verb = mode==='edit' ? 'Cập nhật' : 'Thêm mới';
    const ok = await uiConfirm(`${verb} "${payload.ten}"?`);
    if(!ok) return;

    try{
        if(cata_editingId==null){
            await apiCreate(payload); cata_page = 1; toast('Đã tạo thành công', 'success');
        } else {
            await apiUpdate(cata_editingId, payload); toast('Đã cập nhật', 'success');
        }
        closeDrawer(); await loadAndRender();
    }catch(e){ toast(e.message, 'error', 'Lỗi'); }
}

async function toggleStatus(row){
    const cfg = TABS.find(t=>t.key===cata_active);
    if(!cfg.showTrangThai){ toast('Mục này không có trạng thái.', 'info'); return; }
    const ok = await uiConfirm(`${row.trangThai ? 'Ẩn' : 'Hiển thị'} "${row.ten}"?`);
    if(!ok) return;
    try{
        const body = { ma: row.ma, ten: row.ten };
        if(cfg.showMoTa) body.moTa = row.moTa;
        body.trangThai = !row.trangThai;
        await apiUpdate(row.id, body); toast('Đã cập nhật trạng thái', 'success'); await loadAndRender();
    }catch(e){ toast(e.message, 'error', 'Lỗi'); }
}

/* ================= INIT ================= */
function initCatalogModule() {
    // Wire-up events
    _get('#btnAdd').onclick = ()=>openDrawer('create');
    _get('#btnSave').onclick = saveFromDrawer;
    _get('#btnClose').onclick = closeDrawer;
    _get('#overlay').onclick = closeDrawer;

    let debounce;
    _get('#search').addEventListener('input', (e)=>{
        cata_q = e.target.value || '';
        cata_page = 1;
        clearTimeout(debounce);
        debounce = setTimeout(loadAndRender, 300);
    });
    _get('#prev').onclick = ()=>{ if(cata_page>1){ cata_page--; loadAndRender(); } };
    _get('#next').onclick = ()=>{ if(cata_page<cata_totalPages){ cata_page++; loadAndRender(); } };

    // START
    loadAndRender();

}}