-- 1. TẠO DATABASE
CREATE DATABASE beemate;
GO

USE beemate;
GO

-- ==========================================
-- PHẦN 1: CÁC BẢNG DANH MỤC ĐỘC LẬP (KHÔNG CHỨA KHÓA NGOẠI)
-- ==========================================

CREATE TABLE vai_tro (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(50) NOT NULL UNIQUE,
    ten NVARCHAR(100) NOT NULL
);

CREATE TABLE chuc_vu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(50) NOT NULL UNIQUE,
    ten NVARCHAR(150) NOT NULL,
    mo_ta NVARCHAR(MAX) NULL
);

CREATE TABLE chat_lieu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(20) NOT NULL,
    ten NVARCHAR(100) NOT NULL,
    mo_ta NVARCHAR(MAX),
    ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
    ngay_sua DATETIME NULL,
    trang_thai BIT NOT NULL DEFAULT 1
);

CREATE TABLE danh_muc (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(20) NOT NULL,
    ten NVARCHAR(100) NOT NULL,
    mo_ta NVARCHAR(MAX) NULL,
    ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
    ngay_sua DATETIME NULL,
    trang_thai BIT NOT NULL DEFAULT 1
);

CREATE TABLE hang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(20) NOT NULL,
    ten NVARCHAR(100) NOT NULL,
    mo_ta NVARCHAR(MAX) NULL,
    ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
    ngay_sua DATETIME NULL,
    trang_thai BIT NOT NULL DEFAULT 1
);

CREATE TABLE kich_thuoc (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(20) NOT NULL,
    ten NVARCHAR(100) NOT NULL,
    trang_thai BIT NOT NULL DEFAULT 1
);

CREATE TABLE mau_sac (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(20) NOT NULL,
    ten NVARCHAR(100) NOT NULL,
    trang_thai BIT NOT NULL DEFAULT 1
);

CREATE TABLE trang_thai_hoa_don (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(50) NOT NULL,
    ten NVARCHAR(100) NOT NULL
);

CREATE TABLE khuyen_mai (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(10) NOT NULL,
    ten NVARCHAR(150) NOT NULL,
    loai VARCHAR(20) NOT NULL,
    gia_tri DECIMAL(18,2) NOT NULL,
    ngay_bat_dau DATETIME NOT NULL,
    ngay_ket_thuc DATETIME NOT NULL,
    cho_phep_cong_don BIT DEFAULT 0,
    trang_thai BIT NOT NULL DEFAULT 1
);

CREATE TABLE ma_giam_gia (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_code VARCHAR(100) NOT NULL UNIQUE,
    ten NVARCHAR(100) NOT NULL,
    loai_giam_gia VARCHAR(50) NOT NULL,
    gia_tri_giam_gia DECIMAL(18,2) NOT NULL,
    gia_tri_giam_gia_toi_da DECIMAL(18,2),
    dieu_kien DECIMAL(18,2) NOT NULL,
    so_luong INT NOT NULL,
    luot_su_dung INT NOT NULL DEFAULT 0,
    ngay_bat_dau DATETIME NOT NULL,
    ngay_ket_thuc DATETIME NOT NULL,
    cho_phep_cong_don BIT DEFAULT 0,
    trang_thai BIT DEFAULT 1
);

-- ==========================================
-- PHẦN 2: CÁC BẢNG LIÊN QUAN TÀI KHOẢN VÀ CON NGƯỜI
-- ==========================================

CREATE TABLE tai_khoan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ten_dang_nhap VARCHAR(100) UNIQUE,
    mat_khau VARCHAR(255),
    trang_thai BIT,
    id_vai_tro INT NOT NULL,
    da_doi_ten_dang_nhap BIT DEFAULT 0,
    CONSTRAINT fk_tk_vt FOREIGN KEY (id_vai_tro) REFERENCES vai_tro(id)
);

CREATE TABLE gio_hang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_tai_khoan INT NOT NULL UNIQUE,
    cap_nhat_cuoi DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_gh_tk FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan(id)
);

CREATE TABLE khach_hang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(50) NOT NULL UNIQUE,
    ho_ten NVARCHAR(150) NOT NULL,
    gioi_tinh NVARCHAR(20) NULL,
    ngay_sinh DATE NULL,
    dia_chi NVARCHAR(MAX) NULL,
    so_dien_thoai VARCHAR(12) NULL,
    email VARCHAR(150) NULL,
    hinh_anh NVARCHAR(MAX) NULL,
    trang_thai BIT DEFAULT 1,
    id_tai_khoan INT,
    CONSTRAINT fk_kh_tk FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan(id)
);

CREATE TABLE dia_chi_khach_hang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_khach_hang INT NOT NULL,
    ho_ten_nhan NVARCHAR(150),
    sdt_nhan VARCHAR(15),
    dia_chi_chi_tiet NVARCHAR(255) NOT NULL,
    phuong_xa NVARCHAR(100),
    quan_huyen NVARCHAR(100),
    tinh_thanh_pho NVARCHAR(100),
    ma_tinh INT,
    ma_huyen INT,
    ma_xa VARCHAR(50),
    loai_dia_chi NVARCHAR(50) DEFAULT N'Nhà riêng',
    la_mac_dinh BIT DEFAULT 0,
    trang_thai BIT DEFAULT 1,
    CONSTRAINT fk_dckh_kh FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id)
);

CREATE TABLE nhan_vien (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(50) NOT NULL UNIQUE,
    ho_ten NVARCHAR(150) NOT NULL,
    gioi_tinh NVARCHAR(50),
    ngay_sinh DATE,
    dia_chi NVARCHAR(MAX),
    so_dien_thoai VARCHAR(12),
    email VARCHAR(150),
    hinh_anh NVARCHAR(MAX),
    trang_thai BIT DEFAULT 1,
    id_chuc_vu INT,
    id_tai_khoan INT,
    CONSTRAINT fk_nv_cv FOREIGN KEY (id_chuc_vu) REFERENCES chuc_vu(id),
    CONSTRAINT fk_nv_tk FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan(id)
);

CREATE TABLE thong_bao (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_tai_khoan INT,
    tieu_de NVARCHAR(255),
    noi_dung NVARCHAR(MAX),
    loai_thong_bao VARCHAR(50),
    da_doc BIT DEFAULT 0,
    da_xoa BIT DEFAULT 0,
    ngay_tao DATETIME DEFAULT GETDATE()
    CONSTRAINT fk_tb_tk FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan(id)
);

-- ==========================================
-- PHẦN 3: CÁC BẢNG LIÊN QUAN SẢN PHẨM
-- ==========================================

CREATE TABLE san_pham (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(50) NOT NULL UNIQUE,
    ten NVARCHAR(100) NOT NULL,
    mo_ta NVARCHAR(MAX),
    ngay_tao DATETIME DEFAULT GETDATE(),
    ngay_sua DATETIME,
    id_hang INT,
    id_chat_lieu INT,
    id_danh_muc INT,
    trang_thai BIT DEFAULT 1,
    CONSTRAINT fk_sp_hang FOREIGN KEY (id_hang) REFERENCES hang(id),
    CONSTRAINT fk_sp_cl FOREIGN KEY (id_chat_lieu) REFERENCES chat_lieu(id),
    CONSTRAINT fk_sp_dm FOREIGN KEY (id_danh_muc) REFERENCES danh_muc(id)
);

CREATE TABLE hinh_anh_san_pham (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_san_pham INT,
    url NVARCHAR(MAX) NOT NULL,
    CONSTRAINT fk_hasp_sp FOREIGN KEY (id_san_pham) REFERENCES san_pham(id)
);

CREATE TABLE san_pham_chi_tiet (
    id INT IDENTITY(1,1) PRIMARY KEY,
    sku VARCHAR(100) NOT NULL,
    gia_ban DECIMAL(18,2) NOT NULL,
    so_luong INT NOT NULL DEFAULT 0,
    so_luong_tam_giu INT NOT NULL DEFAULT 0,
    hinh_anh VARCHAR(2048),
    trang_thai BIT NOT NULL DEFAULT 1,
    id_san_pham INT NOT NULL,
    id_mau_sac INT NOT NULL,
    id_kich_thuoc INT NOT NULL,
    CONSTRAINT fk_spct_sp FOREIGN KEY (id_san_pham) REFERENCES san_pham(id),
    CONSTRAINT fk_spct_ms FOREIGN KEY (id_mau_sac) REFERENCES mau_sac(id),
    CONSTRAINT fk_spct_kt FOREIGN KEY (id_kich_thuoc) REFERENCES kich_thuoc(id)
);

CREATE TABLE khuyen_mai_san_pham (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_khuyen_mai INT NOT NULL,
    id_san_pham INT NULL,
    id_san_pham_chi_tiet INT NULL,
    CONSTRAINT fk_kmsp_km FOREIGN KEY (id_khuyen_mai) REFERENCES khuyen_mai(id),
    CONSTRAINT fk_kmsp_sp FOREIGN KEY (id_san_pham) REFERENCES san_pham(id),
    CONSTRAINT fk_kmsp_spct FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id)
);

CREATE TABLE san_pham_yeu_thich (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_tai_khoan INT NULL,
    id_san_pham INT NULL,
    ngay_tao DATETIME DEFAULT GETDATE(),
    CONSTRAINT fk_spyt_tk FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan(id),
    CONSTRAINT fk_spyt_sp FOREIGN KEY (id_san_pham) REFERENCES san_pham(id)
);

-- ==========================================
-- PHẦN 4: CÁC BẢNG NGHIỆP VỤ HÓA ĐƠN & GIAO DỊCH
-- ==========================================

CREATE TABLE hoa_don (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(50) UNIQUE,
    gia_tam_thoi DECIMAL(18,2) DEFAULT 0,
    phi_van_chuyen DECIMAL(18,2) DEFAULT 0,
    gia_tri_khuyen_mai DECIMAL(18,2) DEFAULT 0,
    gia_tong DECIMAL(18,2) DEFAULT 0,
    
    -- CỘT MỚI: Gộp toàn bộ tên, sđt, địa chỉ, mã tỉnh/huyện/xã vào 1 cục JSON
    thong_tin_giao_hang NVARCHAR(MAX),
    
    ghi_chu NVARCHAR(MAX),
    loai_hoa_don INT DEFAULT 0,
    
    hinh_thuc_giao_hang NVARCHAR(50) DEFAULT 'GIAO_TAN_NOI', 
    ngay_tao DATETIME DEFAULT GETDATE(),
    ngay_thanh_toan DATETIME,
    
    ngay_nhan_hang_du_kien DATETIME, -- Dùng cho GHN báo ngày dự kiến
    ngay_hen_lay_hang DATETIME,      -- Dùng khi khách đến cửa hàng
    ngay_hang_san_sang DATETIME,     -- Dùng khi cửa hàng báo đã nhặt xong đồ
    
    id_nhan_vien INT,
    id_khach_hang INT,
    id_ma_giam_gia INT,
    id_trang_thai_hoa_don INT,
    CONSTRAINT fk_hd_nv FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id),
    CONSTRAINT fk_hd_kh FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    CONSTRAINT fk_hd_mgg FOREIGN KEY (id_ma_giam_gia) REFERENCES ma_giam_gia(id),
    CONSTRAINT fk_hd_tthd FOREIGN KEY (id_trang_thai_hoa_don) REFERENCES trang_thai_hoa_don(id)
);

CREATE TABLE hoa_don_chi_tiet (
    id INT IDENTITY(1,1) PRIMARY KEY,
    gia_tien DECIMAL(18,2),
    so_luong INT NOT NULL,
    id_hoa_don INT,
    id_san_pham_chi_tiet INT,
    so_luong_tra INT DEFAULT 0,
    CONSTRAINT fk_hdct_hd FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    CONSTRAINT fk_hdct_spct FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id)
);

CREATE TABLE lich_su_hoa_don (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don INT NOT NULL,
    id_trang_thai_hoa_don INT NOT NULL,
    id_nhan_vien INT,
    ghi_chu NVARCHAR(MAX),
    ngay_tao DATETIME DEFAULT GETDATE(),
    CONSTRAINT fk_lshd_hd FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    CONSTRAINT fk_lshd_tthd FOREIGN KEY (id_trang_thai_hoa_don) REFERENCES trang_thai_hoa_don(id),
    CONSTRAINT fk_lshd_nv FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id)
);

CREATE TABLE thanh_toan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don INT NOT NULL,
    so_tien DECIMAL(18,2) NOT NULL,
    phuong_thuc NVARCHAR(50) NOT NULL,
    loai_thanh_toan VARCHAR(20) DEFAULT 'THANH_TOAN',
    trang_thai VARCHAR(20) DEFAULT 'THANH_CONG',
    ma_giao_dich VARCHAR(100),
    ngay_thanh_toan DATETIME DEFAULT GETDATE(),
    ghi_chu NVARCHAR(MAX),
    id_nhan_vien INT,
    CONSTRAINT fk_tt_hd FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    CONSTRAINT fk_tt_nv FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id)
);

-- BẢNG ĐÁNH GIÁ (ĐÃ BỔ SUNG CHỨC NĂNG TRẢ LỜI CHO NHÂN VIÊN)
CREATE TABLE danh_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tai_khoan_id INT,
    san_pham_id INT,
    so_sao INT,
    noi_dung NVARCHAR(MAX),
    phan_loai NVARCHAR(100),
    danh_sach_hinh_anh NVARCHAR(MAX),
    ngay_tao DATETIME DEFAULT GETDATE(),
    hoa_don_chi_tiet_id INT,
    da_sua BIT DEFAULT 0,
    -- Các trường bổ sung cho nhân viên
    nhan_vien_tra_loi_id INT,
    noi_dung_tra_loi NVARCHAR(MAX),
    ngay_tra_loi DATETIME,
    CONSTRAINT fk_dg_tk FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id),
    CONSTRAINT fk_dg_sp FOREIGN KEY (san_pham_id) REFERENCES san_pham(id),
    CONSTRAINT fk_dg_hdct FOREIGN KEY (hoa_don_chi_tiet_id) REFERENCES hoa_don_chi_tiet(id),
    CONSTRAINT fk_dg_nv FOREIGN KEY (nhan_vien_tra_loi_id) REFERENCES nhan_vien(id)
);

-- ==========================================
-- PHẦN 5: CÁC BẢNG NGHIỆP VỤ ĐỔI TRẢ HÀNG (MỚI BỔ SUNG)
-- ==========================================

CREATE TABLE yeu_cau_doi_tra (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma VARCHAR(50) UNIQUE NOT NULL,
    id_hoa_don INT NOT NULL,
    id_nhan_vien INT, 
    loai_yeu_cau NVARCHAR(50) NOT NULL, -- Ví dụ: 'ĐỔI HÀNG', 'TRẢ HÀNG'
    ly_do NVARCHAR(MAX),
    ghi_chu NVARCHAR(MAX),
    so_tien_hoan DECIMAL(18,2) DEFAULT 0,
    trang_thai NVARCHAR(50) DEFAULT N'Chờ xử lý',
    ngay_tao DATETIME DEFAULT GETDATE(),
    ngay_xu_ly DATETIME,
    CONSTRAINT fk_dt_hd FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    CONSTRAINT fk_dt_nv FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id)
);

CREATE TABLE chi_tiet_doi_tra (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_yeu_cau_doi_tra INT NOT NULL,
    id_hoa_don_chi_tiet INT NOT NULL,
    so_luong INT NOT NULL,
    tinh_trang_san_pham NVARCHAR(MAX),
    CONSTRAINT fk_ctdt_dt FOREIGN KEY (id_yeu_cau_doi_tra) REFERENCES yeu_cau_doi_tra(id),
    CONSTRAINT fk_ctdt_hdct FOREIGN KEY (id_hoa_don_chi_tiet) REFERENCES hoa_don_chi_tiet(id)
);
GO

CREATE TABLE gio_hang_chi_tiet (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_gio_hang INT NOT NULL,
    id_san_pham_chi_tiet INT NOT NULL,
    so_luong INT NOT NULL DEFAULT 1,
    ngay_them DATETIME DEFAULT GETDATE(),
    CONSTRAINT fk_ghct_gh FOREIGN KEY (id_gio_hang) REFERENCES gio_hang(id),
    CONSTRAINT fk_ghct_spct FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id)
);
GO