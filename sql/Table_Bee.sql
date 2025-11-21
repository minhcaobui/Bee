/* =======================================================================
   E-COMMERCE CLOTHING – SCHEMA (SQL SERVER)  -- CÁCH 1 (ONLINE & POS TÁCH)
   ======================================================================= */

-- === A. COMMON / ACCOUNT =================================================
IF OBJECT_ID('vai_tro','U') IS NULL
CREATE TABLE vai_tro(
                        id INT IDENTITY(1,1) PRIMARY KEY,
                        ma VARCHAR(50) UNIQUE NOT NULL,
                        ten NVARCHAR(100) NOT NULL
);

IF OBJECT_ID('tai_khoan','U') IS NULL
CREATE TABLE tai_khoan(
                          id INT IDENTITY(1,1) PRIMARY KEY,
                          ten_dang_nhap VARCHAR(100) UNIQUE NOT NULL,
                          email VARCHAR(150) UNIQUE NOT NULL,
                          mat_khau VARBINARY(MAX) NOT NULL,
                          trang_thai BIT NOT NULL DEFAULT 1,
                          ngay_tao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME()
);

IF OBJECT_ID('tai_khoan_vai_tro','U') IS NULL
CREATE TABLE tai_khoan_vai_tro(
                                  id INT IDENTITY(1,1) PRIMARY KEY,
                                  id_tai_khoan INT NOT NULL,
                                  id_vai_tro INT NOT NULL,
                                  CONSTRAINT fk_tkvt_tk FOREIGN KEY(id_tai_khoan) REFERENCES tai_khoan(id),
                                  CONSTRAINT fk_tkvt_vt FOREIGN KEY(id_vai_tro) REFERENCES vai_tro(id),
                                  CONSTRAINT uq_tkvt UNIQUE(id_tai_khoan,id_vai_tro)
);

IF OBJECT_ID('khach_hang','U') IS NULL
CREATE TABLE khach_hang(
                           id INT IDENTITY(1,1) PRIMARY KEY,
                           id_tai_khoan INT NOT NULL UNIQUE,
                           ho_ten NVARCHAR(150),
                           so_dien_thoai VARCHAR(20),
                           ngay_sinh DATE NULL,
                           gioi_tinh NVARCHAR(10) NULL,
                           CONSTRAINT fk_kh_tk FOREIGN KEY(id_tai_khoan) REFERENCES tai_khoan(id)
);

IF OBJECT_ID('dia_chi','U') IS NULL
CREATE TABLE dia_chi(
                        id INT IDENTITY(1,1) PRIMARY KEY,
                        id_tai_khoan INT NOT NULL,
                        ho_ten NVARCHAR(150) NOT NULL,
                        so_dien_thoai VARCHAR(20) NOT NULL,
                        tinh NVARCHAR(100) NOT NULL,
                        quan NVARCHAR(100) NOT NULL,
                        phuong NVARCHAR(100) NOT NULL,
                        dia_chi_chi_tiet NVARCHAR(255) NOT NULL,
                        mac_dinh BIT NOT NULL DEFAULT 0,
                        CONSTRAINT fk_dc_tk FOREIGN KEY(id_tai_khoan) REFERENCES tai_khoan(id)
);

-- === B. CATALOG ==========================================================
IF OBJECT_ID('danh_muc','U') IS NULL
CREATE TABLE danh_muc(
                         id INT IDENTITY(1,1) PRIMARY KEY,
                         ma VARCHAR(50) UNIQUE NOT NULL,
                         ten NVARCHAR(150) NOT NULL,
                         mo_ta NVARCHAR(255) NULL,
                         trang_thai BIT NOT NULL DEFAULT 1
);

IF OBJECT_ID('hang','U') IS NULL
CREATE TABLE hang(
                     id INT IDENTITY(1,1) PRIMARY KEY,
                     ma VARCHAR(50) UNIQUE NOT NULL,
                     ten NVARCHAR(150) NOT NULL,
                     mo_ta NVARCHAR(255) NULL,
                     trang_thai BIT NOT NULL DEFAULT 1
);

IF OBJECT_ID('chat_lieu','U') IS NULL
CREATE TABLE chat_lieu(
                          id INT IDENTITY(1,1) PRIMARY KEY,
                          ma VARCHAR(50) UNIQUE NOT NULL,
                          ten NVARCHAR(150) NOT NULL,
                          mo_ta NVARCHAR(255) NULL,
                          trang_thai BIT NOT NULL DEFAULT 1
);

IF OBJECT_ID('kich_thuoc','U') IS NULL
CREATE TABLE kich_thuoc(
                           id INT IDENTITY(1,1) PRIMARY KEY,
                           ma VARCHAR(50) UNIQUE NOT NULL,
                           ten NVARCHAR(50) NOT NULL
);

IF OBJECT_ID('mau_sac','U') IS NULL
CREATE TABLE mau_sac(
                        id INT IDENTITY(1,1) PRIMARY KEY,
                        ma VARCHAR(50) UNIQUE NOT NULL, -- dùng mã HEX cũng được
                        ten NVARCHAR(50) NOT NULL
);

IF OBJECT_ID('san_pham','U') IS NULL
CREATE TABLE san_pham(
                         id INT IDENTITY(1,1) PRIMARY KEY,
                         ma VARCHAR(50) UNIQUE NOT NULL,
                         ten NVARCHAR(200) NOT NULL,
                         mo_ta NVARCHAR(MAX) NULL,
                         hinh_anh_dai_dien NVARCHAR(255) NULL,
                         id_danh_muc INT NOT NULL,
                         id_hang INT NOT NULL,
                         id_chat_lieu INT NOT NULL,
                         trang_thai BIT NOT NULL DEFAULT 1,
                         ngay_tao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                         CONSTRAINT fk_sp_dm FOREIGN KEY(id_danh_muc) REFERENCES danh_muc(id),
                         CONSTRAINT fk_sp_hang FOREIGN KEY(id_hang) REFERENCES hang(id),
                         CONSTRAINT fk_sp_cl FOREIGN KEY(id_chat_lieu) REFERENCES chat_lieu(id)
);

IF OBJECT_ID('hinh_anh_san_pham','U') IS NULL
CREATE TABLE hinh_anh_san_pham(
                                  id INT IDENTITY(1,1) PRIMARY KEY,
                                  id_san_pham INT NOT NULL,
                                  url NVARCHAR(255) NOT NULL,
                                  thu_tu INT NOT NULL DEFAULT 0,
                                  CONSTRAINT fk_has_sp FOREIGN KEY(id_san_pham) REFERENCES san_pham(id)
);

IF OBJECT_ID('san_pham_bien_the','U') IS NULL
CREATE TABLE san_pham_bien_the(
                                  id INT IDENTITY(1,1) PRIMARY KEY,
                                  id_san_pham INT NOT NULL,
                                  sku VARCHAR(100) UNIQUE NOT NULL,
                                  gia DECIMAL(12,2) NOT NULL,
                                  so_luong INT NOT NULL DEFAULT 0,
                                  id_kich_thuoc INT NOT NULL,
                                  id_mau_sac INT NOT NULL,
                                  trang_thai BIT NOT NULL DEFAULT 1,
                                  CONSTRAINT fk_spbt_sp FOREIGN KEY(id_san_pham) REFERENCES san_pham(id),
                                  CONSTRAINT fk_spbt_size FOREIGN KEY(id_kich_thuoc) REFERENCES kich_thuoc(id),
                                  CONSTRAINT fk_spbt_color FOREIGN KEY(id_mau_sac) REFERENCES mau_sac(id),
                                  CONSTRAINT uq_spbt UNIQUE(id_san_pham,id_kich_thuoc,id_mau_sac)
);

-- Tùy chọn kho_hang chi tiết theo vị trí/kho
IF OBJECT_ID('kho_hang','U') IS NULL
CREATE TABLE kho_hang(
                         id INT IDENTITY(1,1) PRIMARY KEY,
                         id_san_pham_bien_the INT NOT NULL,
                         vi_tri NVARCHAR(100) NULL,
                         so_luong_ton INT NOT NULL DEFAULT 0,
                         CONSTRAINT fk_kho_spbt FOREIGN KEY(id_san_pham_bien_the) REFERENCES san_pham_bien_the(id)
);

-- === C. CART & WISHLIST & REVIEWS =======================================
IF OBJECT_ID('gio_hang','U') IS NULL
CREATE TABLE gio_hang(
                         id INT IDENTITY(1,1) PRIMARY KEY,
                         id_tai_khoan INT NOT NULL UNIQUE,
                         cap_nhat_cuoi DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                         CONSTRAINT fk_gh_tk FOREIGN KEY(id_tai_khoan) REFERENCES tai_khoan(id)
);

IF OBJECT_ID('gio_hang_chi_tiet','U') IS NULL
CREATE TABLE gio_hang_chi_tiet(
                                  id INT IDENTITY(1,1) PRIMARY KEY,
                                  id_gio_hang INT NOT NULL,
                                  id_san_pham_bien_the INT NOT NULL,
                                  so_luong INT NOT NULL CHECK(so_luong>0),
                                  CONSTRAINT fk_ghct_gh FOREIGN KEY(id_gio_hang) REFERENCES gio_hang(id),
                                  CONSTRAINT fk_ghct_spbt FOREIGN KEY(id_san_pham_bien_the) REFERENCES san_pham_bien_the(id),
                                  CONSTRAINT uq_ghct UNIQUE(id_gio_hang,id_san_pham_bien_the)
);

IF OBJECT_ID('yeu_thich','U') IS NULL
CREATE TABLE yeu_thich(
                          id INT IDENTITY(1,1) PRIMARY KEY,
                          id_tai_khoan INT NOT NULL,
                          id_san_pham INT NOT NULL,
                          ngay DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                          CONSTRAINT fk_yt_tk FOREIGN KEY(id_tai_khoan) REFERENCES tai_khoan(id),
                          CONSTRAINT fk_yt_sp FOREIGN KEY(id_san_pham) REFERENCES san_pham(id),
                          CONSTRAINT uq_yt UNIQUE(id_tai_khoan,id_san_pham)
);

IF OBJECT_ID('danh_gia','U') IS NULL
CREATE TABLE danh_gia(
                         id INT IDENTITY(1,1) PRIMARY KEY,
                         id_tai_khoan INT NOT NULL,
                         id_san_pham INT NOT NULL,
                         so_sao INT NOT NULL CHECK(so_sao BETWEEN 1 AND 5),
                         noi_dung NVARCHAR(1000) NULL,
                         ngay DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                         CONSTRAINT fk_dg_tk FOREIGN KEY(id_tai_khoan) REFERENCES tai_khoan(id),
                         CONSTRAINT fk_dg_sp FOREIGN KEY(id_san_pham) REFERENCES san_pham(id),
                         CONSTRAINT uq_dg UNIQUE(id_tai_khoan,id_san_pham)
);

-- === D. ORDER (ONLINE) ===================================================
IF OBJECT_ID('trang_thai_don_hang','U') IS NULL
CREATE TABLE trang_thai_don_hang(
                                    id INT IDENTITY(1,1) PRIMARY KEY,
                                    ma VARCHAR(30) UNIQUE NOT NULL, -- NEW/CONFIRMED/PACKED/SHIPPING/COMPLETED/CANCELLED
                                    mo_ta NVARCHAR(100) NULL
);

IF OBJECT_ID('don_hang','U') IS NULL
CREATE TABLE don_hang(
                         id INT IDENTITY(1,1) PRIMARY KEY,
                         ma VARCHAR(50) UNIQUE NOT NULL,
                         id_tai_khoan INT NOT NULL,
                         id_dia_chi INT NOT NULL,
                         id_trang_thai INT NOT NULL,
                         gia_tam DECIMAL(12,2) NOT NULL,
                         gia_giam DECIMAL(12,2) NOT NULL DEFAULT 0,
                         phi_van_chuyen DECIMAL(12,2) NOT NULL DEFAULT 0,
                         gia_tong DECIMAL(12,2) NOT NULL,
                         code_ma_giam_gia VARCHAR(50) NULL,
                         ngay_tao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                         CONSTRAINT fk_dh_kh FOREIGN KEY(id_tai_khoan) REFERENCES tai_khoan(id),
                         CONSTRAINT fk_dh_dc FOREIGN KEY(id_dia_chi) REFERENCES dia_chi(id),
                         CONSTRAINT fk_dh_tt FOREIGN KEY(id_trang_thai) REFERENCES trang_thai_don_hang(id)
);

IF OBJECT_ID('don_hang_chi_tiet','U') IS NULL
CREATE TABLE don_hang_chi_tiet(
                                  id INT IDENTITY(1,1) PRIMARY KEY,
                                  id_don_hang INT NOT NULL,
                                  id_san_pham_bien_the INT NOT NULL,
                                  so_luong INT NOT NULL CHECK(so_luong>0),
                                  don_gia DECIMAL(12,2) NOT NULL,
                                  giam_gia DECIMAL(12,2) NOT NULL DEFAULT 0,
                                  CONSTRAINT fk_dhct_dh FOREIGN KEY(id_don_hang) REFERENCES don_hang(id),
                                  CONSTRAINT fk_dhct_spbt FOREIGN KEY(id_san_pham_bien_the) REFERENCES san_pham_bien_the(id)
);

IF OBJECT_ID('lich_su_trang_thai_don','U') IS NULL
CREATE TABLE lich_su_trang_thai_don(
                                       id INT IDENTITY(1,1) PRIMARY KEY,
                                       id_don_hang INT NOT NULL,
                                       tu_trang_thai VARCHAR(30) NULL,
                                       den_trang_thai VARCHAR(30) NOT NULL,
                                       ghi_chu NVARCHAR(255) NULL,
                                       thoi_gian DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                                       CONSTRAINT fk_ls_dh FOREIGN KEY(id_don_hang) REFERENCES don_hang(id)
);

IF OBJECT_ID('van_chuyen','U') IS NULL
CREATE TABLE van_chuyen(
                           id INT IDENTITY(1,1) PRIMARY KEY,
                           id_don_hang INT NOT NULL,
                           dvvc NVARCHAR(50) NOT NULL,
                           ma_van_don NVARCHAR(100) NULL,
                           trang_thai NVARCHAR(50) NULL, -- CREATED/PICKED/TRANSIT/DELIVERED/FAILED
                           phi_van_chuyen DECIMAL(12,2) NULL,
                           ngay_tao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                           CONSTRAINT fk_vc_dh FOREIGN KEY(id_don_hang) REFERENCES don_hang(id)
);

IF OBJECT_ID('thanh_toan','U') IS NULL
CREATE TABLE thanh_toan(
                           id INT IDENTITY(1,1) PRIMARY KEY,
                           id_don_hang INT NULL,
                           id_hoa_don_offline INT NULL, -- POS: dùng trường này
                           phuong_thuc NVARCHAR(30) NOT NULL, -- COD/VNPAY/MOMO/CASH/CARD/QR
                           so_tien DECIMAL(12,2) NOT NULL,
                           trang_thai NVARCHAR(20) NOT NULL, -- PENDING/PAID/FAILED/REFUNDED
                           ma_giao_dich NVARCHAR(100) NULL,
                           ngay_tao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                           CONSTRAINT fk_tt_dh FOREIGN KEY(id_don_hang) REFERENCES don_hang(id)
);

-- === E. PROMO / VOUCHER ==================================================
IF OBJECT_ID('khuyen_mai','U') IS NULL
CREATE TABLE khuyen_mai(
                           id INT IDENTITY(1,1) PRIMARY KEY,
                           ten NVARCHAR(150) NOT NULL,
                           loai NVARCHAR(20) NOT NULL, -- PERCENT/AMOUNT
                           gia_tri DECIMAL(12,2) NOT NULL,
                           ngay_bat_dau DATETIME2(0) NOT NULL,
                           ngay_ket_thuc DATETIME2(0) NOT NULL,
                           trang_thai BIT NOT NULL DEFAULT 1
);

IF OBJECT_ID('khuyen_mai_san_pham','U') IS NULL
CREATE TABLE khuyen_mai_san_pham(
                                    id INT IDENTITY(1,1) PRIMARY KEY,
                                    id_khuyen_mai INT NOT NULL,
                                    id_san_pham INT NOT NULL,
                                    CONSTRAINT fk_kmsp_km FOREIGN KEY(id_khuyen_mai) REFERENCES khuyen_mai(id),
                                    CONSTRAINT fk_kmsp_sp FOREIGN KEY(id_san_pham) REFERENCES san_pham(id),
                                    CONSTRAINT uq_kmsp UNIQUE(id_khuyen_mai,id_san_pham)
);

IF OBJECT_ID('ma_giam_gia','U') IS NULL
CREATE TABLE ma_giam_gia(
                            id INT IDENTITY(1,1) PRIMARY KEY,
                            code VARCHAR(50) UNIQUE NOT NULL,
                            loai NVARCHAR(20) NOT NULL, -- PERCENT/AMOUNT
                            gia_tri DECIMAL(12,2) NOT NULL,
                            gia_tri_toi_da DECIMAL(12,2) NULL,
                            don_toi_thieu DECIMAL(12,2) NULL,
                            so_luong INT NOT NULL DEFAULT 0, -- quota
                            so_lan_su_dung INT NOT NULL DEFAULT 0,
                            ngay_bat_dau DATETIME2(0) NOT NULL,
                            ngay_ket_thuc DATETIME2(0) NOT NULL,
                            trang_thai BIT NOT NULL DEFAULT 1
);

IF OBJECT_ID('ma_giam_gia_khach_hang','U') IS NULL
CREATE TABLE ma_giam_gia_khach_hang(
                                       id INT IDENTITY(1,1) PRIMARY KEY,
                                       id_ma_giam_gia INT NOT NULL,
                                       id_tai_khoan INT NOT NULL,
                                       CONSTRAINT fk_mggkh_mgg FOREIGN KEY(id_ma_giam_gia) REFERENCES ma_giam_gia(id),
                                       CONSTRAINT fk_mggkh_tk FOREIGN KEY(id_tai_khoan) REFERENCES tai_khoan(id),
                                       CONSTRAINT uq_mggkh UNIQUE(id_ma_giam_gia,id_tai_khoan)
);

-- === F. INVOICE ONLINE (E-INVOICE LINK) ==================================
IF OBJECT_ID('hoa_don','U') IS NULL
CREATE TABLE hoa_don(
                        id INT IDENTITY(1,1) PRIMARY KEY,
                        so_hoa_don VARCHAR(50) UNIQUE NOT NULL,
                        id_don_hang INT NOT NULL,
                        tong_tien DECIMAL(12,2) NOT NULL,
                        thue_vat DECIMAL(12,2) NULL,
                        file_pdf NVARCHAR(255) NULL,
                        ngay_xuat DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                        CONSTRAINT fk_hd_dh FOREIGN KEY(id_don_hang) REFERENCES don_hang(id)
);

-- === G. POS OFFLINE =======================================================
IF OBJECT_ID('nhan_vien','U') IS NULL
CREATE TABLE nhan_vien(
                          id INT IDENTITY(1,1) PRIMARY KEY,
                          ma VARCHAR(50) UNIQUE NOT NULL,
                          ho_ten NVARCHAR(150) NOT NULL,
                          id_tai_khoan INT NULL, -- nếu NV đăng nhập POS
                          CONSTRAINT fk_nv_tk FOREIGN KEY(id_tai_khoan) REFERENCES tai_khoan(id)
);

IF OBJECT_ID('cua_hang','U') IS NULL
CREATE TABLE cua_hang(
                         id INT IDENTITY(1,1) PRIMARY KEY,
                         ma VARCHAR(50) UNIQUE NOT NULL,
                         ten NVARCHAR(150) NOT NULL,
                         dia_chi NVARCHAR(255) NULL,
                         so_dien_thoai VARCHAR(20) NULL,
                         trang_thai BIT NOT NULL DEFAULT 1
);

IF OBJECT_ID('ca_ban_hang','U') IS NULL
CREATE TABLE ca_ban_hang(
                            id INT IDENTITY(1,1) PRIMARY KEY,
                            id_cua_hang INT NOT NULL,
                            id_nhan_vien_mo INT NOT NULL,
                            thoi_gian_mo DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                            id_nhan_vien_dong INT NULL,
                            thoi_gian_dong DATETIME2(0) NULL,
                            tien_mat_ban_dau DECIMAL(12,2) NOT NULL DEFAULT 0,
                            tien_mat_thuc_te_cuoi_ca DECIMAL(12,2) NULL,
                            ghi_chu NVARCHAR(255) NULL,
                            CONSTRAINT fk_cbh_ch FOREIGN KEY(id_cua_hang) REFERENCES cua_hang(id),
                            CONSTRAINT fk_cbh_nvmo FOREIGN KEY(id_nhan_vien_mo) REFERENCES nhan_vien(id),
                            CONSTRAINT fk_cbh_nvdong FOREIGN KEY(id_nhan_vien_dong) REFERENCES nhan_vien(id)
);

IF OBJECT_ID('hoa_don_offline','U') IS NULL
CREATE TABLE hoa_don_offline(
                                id INT IDENTITY(1,1) PRIMARY KEY,
                                ma VARCHAR(50) UNIQUE NOT NULL,
                                id_cua_hang INT NOT NULL,
                                id_ca_ban_hang INT NOT NULL,
                                id_nhan_vien INT NOT NULL,
                                id_tai_khoan_khach INT NULL, -- thành viên (nếu có)
                                tong_tien DECIMAL(12,2) NOT NULL,
                                thue_vat DECIMAL(12,2) NULL,
                                giam_gia DECIMAL(12,2) NOT NULL DEFAULT 0,
                                thanh_tien DECIMAL(12,2) NOT NULL, -- = tong + vat - giam
                                hinh_thuc NVARCHAR(20) NOT NULL,   -- CASH|CARD|QR|MIXED
                                ngay_tao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                                CONSTRAINT fk_hdof_ch FOREIGN KEY(id_cua_hang) REFERENCES cua_hang(id),
                                CONSTRAINT fk_hdof_cabh FOREIGN KEY(id_ca_ban_hang) REFERENCES ca_ban_hang(id),
                                CONSTRAINT fk_hdof_nv FOREIGN KEY(id_nhan_vien) REFERENCES nhan_vien(id),
                                CONSTRAINT fk_hdof_tk FOREIGN KEY(id_tai_khoan_khach) REFERENCES tai_khoan(id)
);

IF OBJECT_ID('hoa_don_offline_chi_tiet','U') IS NULL
CREATE TABLE hoa_don_offline_chi_tiet(
                                         id INT IDENTITY(1,1) PRIMARY KEY,
                                         id_hoa_don_offline INT NOT NULL,
                                         id_san_pham_bien_the INT NOT NULL,
                                         so_luong INT NOT NULL CHECK(so_luong>0),
                                         don_gia DECIMAL(12,2) NOT NULL,
                                         giam_gia DECIMAL(12,2) NOT NULL DEFAULT 0,
                                         CONSTRAINT fk_hdofct_hd FOREIGN KEY(id_hoa_don_offline) REFERENCES hoa_don_offline(id),
                                         CONSTRAINT fk_hdofct_spbt FOREIGN KEY(id_san_pham_bien_the) REFERENCES san_pham_bien_the(id)
);

-- Liên kết thanh_toan với hoa_don_offline (đã tạo bảng thanh_toan ở trên)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE Name = N'id_hoa_don_offline' AND Object_ID = Object_ID('thanh_toan'))
BEGIN
ALTER TABLE thanh_toan ADD id_hoa_don_offline INT NULL;
ALTER TABLE thanh_toan ADD CONSTRAINT fk_tt_hdof FOREIGN KEY(id_hoa_don_offline) REFERENCES hoa_don_offline(id);
END

-- Trả/đổi offline
IF OBJECT_ID('phieu_tra_offline','U') IS NULL
CREATE TABLE phieu_tra_offline(
                                  id INT IDENTITY(1,1) PRIMARY KEY,
                                  id_hoa_don_offline INT NOT NULL,
                                  id_nhan_vien INT NOT NULL,
                                  ly_do NVARCHAR(255) NULL,
                                  ngay_tao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                                  CONSTRAINT fk_ptof_hdof FOREIGN KEY(id_hoa_don_offline) REFERENCES hoa_don_offline(id),
                                  CONSTRAINT fk_ptof_nv FOREIGN KEY(id_nhan_vien) REFERENCES nhan_vien(id)
);

IF OBJECT_ID('phieu_tra_offline_chi_tiet','U') IS NULL
CREATE TABLE phieu_tra_offline_chi_tiet(
                                           id INT IDENTITY(1,1) PRIMARY KEY,
                                           id_phieu_tra_offline INT NOT NULL,
                                           id_san_pham_bien_the INT NOT NULL,
                                           so_luong INT NOT NULL CHECK(so_luong>0),
                                           don_gia DECIMAL(12,2) NOT NULL,
                                           CONSTRAINT fk_ptofct_pt FOREIGN KEY(id_phieu_tra_offline) REFERENCES phieu_tra_offline(id),
                                           CONSTRAINT fk_ptofct_spbt FOREIGN KEY(id_san_pham_bien_the) REFERENCES san_pham_bien_the(id)
);

-- === H. PURCHASE RECEIPTS (NHẬP KHO) ====================================
IF OBJECT_ID('phieu_nhap','U') IS NULL
CREATE TABLE phieu_nhap(
                           id INT IDENTITY(1,1) PRIMARY KEY,
                           ma VARCHAR(50) UNIQUE NOT NULL,
                           id_nhan_vien INT NOT NULL,
                           ngay DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                           ghi_chu NVARCHAR(255) NULL,
                           CONSTRAINT fk_pn_nv FOREIGN KEY(id_nhan_vien) REFERENCES nhan_vien(id)
);

IF OBJECT_ID('phieu_nhap_chi_tiet','U') IS NULL
CREATE TABLE phieu_nhap_chi_tiet(
                                    id INT IDENTITY(1,1) PRIMARY KEY,
                                    id_phieu_nhap INT NOT NULL,
                                    id_san_pham_bien_the INT NOT NULL,
                                    so_luong INT NOT NULL CHECK(so_luong>0),
                                    don_gia DECIMAL(12,2) NOT NULL,
                                    CONSTRAINT fk_pnct_pn FOREIGN KEY(id_phieu_nhap) REFERENCES phieu_nhap(id),
                                    CONSTRAINT fk_pnct_spbt FOREIGN KEY(id_san_pham_bien_the) REFERENCES san_pham_bien_the(id)
);

/* ================================================================
   I. RETURNS / EXCHANGES (YÊU CẦU ĐỔI/TRẢ – ONLINE)
   Phụ thuộc: don_hang, don_hang_chi_tiet, san_pham_bien_the, thanh_toan
   ================================================================ */

-- 1) Bảng trạng thái yêu cầu (tuỳ chọn nhưng nên có)
IF OBJECT_ID('ycdt_trang_thai','U') IS NULL
CREATE TABLE ycdt_trang_thai (
                                 id INT IDENTITY(1,1) PRIMARY KEY,
                                 ma VARCHAR(30) UNIQUE NOT NULL,   -- PENDING_REVIEW/APPROVED/RECEIVED/REJECTED/COMPLETED/CANCELLED
                                 mo_ta NVARCHAR(100) NULL
);

-- Seed trạng thái cơ bản (idempotent)
IF NOT EXISTS (SELECT 1 FROM ycdt_trang_thai WHERE ma='PENDING_REVIEW')
INSERT INTO ycdt_trang_thai(ma, mo_ta) VALUES
('PENDING_REVIEW', N'Chờ duyệt'),
('APPROVED',       N'Đã duyệt'),
('RECEIVED',       N'Kho đã nhận hàng hoàn'),
('REJECTED',       N'Từ chối'),
('COMPLETED',      N'Hoàn tất'),
('CANCELLED',      N'Đã huỷ');

-- 2) Master yêu cầu đổi/trả
IF OBJECT_ID('yeu_cau_doi_tra','U') IS NULL
CREATE TABLE yeu_cau_doi_tra(
                                id INT IDENTITY(1,1) PRIMARY KEY,
                                so_yc VARCHAR(50) UNIQUE NOT NULL,          -- ví dụ: RT2025-00001
                                id_don_hang INT NOT NULL,
                                loai NVARCHAR(10) NOT NULL CHECK (loai IN ('DOI','TRA')), -- ĐỔI/ TRẢ
                                id_trang_thai INT NOT NULL,
                                ly_do NVARCHAR(255) NULL,
                                ghi_chu NVARCHAR(255) NULL,
                                phi_xu_ly DECIMAL(12,2) NOT NULL DEFAULT 0, -- phí xử lý/ship nếu có
                                ngay_tao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                                ngay_cap_nhat DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                                CONSTRAINT fk_ycdt_dh FOREIGN KEY (id_don_hang) REFERENCES don_hang(id),
                                CONSTRAINT fk_ycdt_tt FOREIGN KEY (id_trang_thai) REFERENCES ycdt_trang_thai(id)
);
CREATE INDEX ix_ycdt_dh ON yeu_cau_doi_tra(id_don_hang);

-- 3) Chi tiết yêu cầu (mỗi dòng trỏ đến dòng đơn hàng gốc)
IF OBJECT_ID('yeu_cau_doi_tra_ct','U') IS NULL
CREATE TABLE yeu_cau_doi_tra_ct(
                                   id INT IDENTITY(1,1) PRIMARY KEY,
                                   id_yeu_cau INT NOT NULL,
                                   id_don_hang_chi_tiet INT NOT NULL,
                                   so_luong INT NOT NULL CHECK (so_luong > 0),
                                   id_bien_the_doi_moi INT NULL,               -- nếu là ĐỔI: biến thể mới
                                   chenh_lech DECIMAL(12,2) NULL,              -- + khách bù, - hoàn lại
                                   CONSTRAINT fk_ycdtct_yc   FOREIGN KEY (id_yeu_cau) REFERENCES yeu_cau_doi_tra(id),
                                   CONSTRAINT fk_ycdtct_dhct FOREIGN KEY (id_don_hang_chi_tiet) REFERENCES don_hang_chi_tiet(id),
                                   CONSTRAINT fk_ycdtct_bt   FOREIGN KEY (id_bien_the_doi_moi) REFERENCES san_pham_bien_the(id)
);
CREATE INDEX ix_ycdtct_yc   ON yeu_cau_doi_tra_ct(id_yeu_cau);
CREATE INDEX ix_ycdtct_dhct ON yeu_cau_doi_tra_ct(id_don_hang_chi_tiet);

-- 4) File đính kèm (ảnh/biên nhận …)
IF OBJECT_ID('yeu_cau_doi_tra_file','U') IS NULL
CREATE TABLE yeu_cau_doi_tra_file(
                                     id INT IDENTITY(1,1) PRIMARY KEY,
                                     id_yeu_cau INT NOT NULL,
                                     url NVARCHAR(255) NOT NULL,
                                     mo_ta NVARCHAR(150) NULL,
                                     ngay_tao DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                                     CONSTRAINT fk_ycdtf_yc FOREIGN KEY (id_yeu_cau) REFERENCES yeu_cau_doi_tra(id)
);

-- 5) Lịch sử trạng thái (log)
IF OBJECT_ID('yeu_cau_doi_tra_log','U') IS NULL
CREATE TABLE yeu_cau_doi_tra_log(
                                    id INT IDENTITY(1,1) PRIMARY KEY,
                                    id_yeu_cau INT NOT NULL,
                                    tu_trang_thai NVARCHAR(30) NULL,
                                    den_trang_thai NVARCHAR(30) NOT NULL,
                                    ghi_chu NVARCHAR(255) NULL,
                                    thoi_gian DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
                                    CONSTRAINT fk_ycdtlog_yc FOREIGN KEY (id_yeu_cau) REFERENCES yeu_cau_doi_tra(id)
);
CREATE INDEX ix_ycdtlog_yc ON yeu_cau_doi_tra_log(id_yeu_cau);

-- 6) Liên kết thanh_toan với yêu cầu (để ghi nhận REFUND)
IF NOT EXISTS (
  SELECT 1 FROM sys.columns 
  WHERE Name = N'id_yeu_cau_doi_tra' AND Object_ID = Object_ID('thanh_toan')
)
BEGIN
ALTER TABLE thanh_toan
    ADD id_yeu_cau_doi_tra INT NULL;

ALTER TABLE thanh_toan ADD CONSTRAINT fk_tt_ycdt
    FOREIGN KEY(id_yeu_cau_doi_tra) REFERENCES yeu_cau_doi_tra(id);

CREATE INDEX ix_tt_ycdt ON thanh_toan(id_yeu_cau_doi_tra);
END

-- 7) View hỗ trợ kiểm tra số lượng đã yêu cầu (để chặn yêu cầu vượt số đã mua)
IF OBJECT_ID('v_ycdt_da_yeu_cau','V') IS NOT NULL DROP VIEW v_ycdt_da_yeu_cau;
GO
CREATE VIEW v_ycdt_da_yeu_cau AS
SELECT
    ct.id_don_hang_chi_tiet,
    SUM(ct.so_luong) AS so_luong_da_yeu_cau
FROM yeu_cau_doi_tra_ct ct
         JOIN yeu_cau_doi_tra yc ON yc.id = ct.id_yeu_cau
         JOIN ycdt_trang_thai tt ON tt.id = yc.id_trang_thai
WHERE tt.ma NOT IN ('REJECTED','CANCELLED')
GROUP BY ct.id_don_hang_chi_tiet;
GO

/* ============ (Tuỳ chọn) SP minh hoạ: Kho xác nhận nhận hàng hoàn ============ */
CREATE OR ALTER PROCEDURE dbo.sp_ycdt_receive
    @YeuCauId INT
    AS
BEGIN
  SET NOCOUNT ON;
  SET XACT_ABORT ON;

BEGIN TRY
BEGIN TRAN;

    DECLARE @tt_approved INT = (SELECT id FROM dbo.ycdt_trang_thai WHERE ma='APPROVED');
    DECLARE @tt_received INT = (SELECT id FROM dbo.ycdt_trang_thai WHERE ma='RECEIVED');
    DECLARE @cur_tt INT = (SELECT id_trang_thai FROM dbo.yeu_cau_doi_tra WHERE id=@YeuCauId);

    IF @cur_tt <> @tt_approved
      RAISERROR(N'Chỉ xử lý nhận hàng cho yêu cầu ở trạng thái APPROVED',16,1);

    ;WITH lines AS (
    SELECT
        ct.id_don_hang_chi_tiet,
        ct.so_luong,                                   -- so_luong của yêu cầu
        dhct.id_san_pham_bien_the AS bien_the_cu
    FROM dbo.yeu_cau_doi_tra_ct AS ct
             JOIN dbo.don_hang_chi_tiet   AS dhct ON dhct.id = ct.id_don_hang_chi_tiet
    WHERE ct.id_yeu_cau = @YeuCauId
)
UPDATE spbt
SET spbt.so_luong = spbt.so_luong + l.so_luong   -- <-- định danh alias rõ ràng
    FROM dbo.san_pham_bien_the AS spbt
    INNER JOIN lines AS l
ON l.bien_the_cu = spbt.id;

UPDATE dbo.yeu_cau_doi_tra
SET id_trang_thai = @tt_received, ngay_cap_nhat = SYSDATETIME()
WHERE id=@YeuCauId;

INSERT INTO dbo.yeu_cau_doi_tra_log(id_yeu_cau, tu_trang_thai, den_trang_thai)
VALUES (@YeuCauId, 'APPROVED', 'RECEIVED');

COMMIT TRAN;
END TRY
BEGIN CATCH
IF @@TRANCOUNT>0 ROLLBACK TRAN;
    THROW;
END CATCH
END
GO

