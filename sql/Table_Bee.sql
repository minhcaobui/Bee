CREATE DATABASE beemate
GO

USE beemate
GO

-- =========================================================================
-- PHẦN 1: TẠO CẤU TRÚC BẢNG (CHỈ CHỨA CỘT, KHÓA CHÍNH, UNIQUE VÀ CHECK)
-- =========================================================================

CREATE TABLE hang(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(50) NOT NULL,
ten NVARCHAR(100) NOT NULL,
mo_ta NVARCHAR(MAX) NULL,
ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
ngay_sua DATETIME NULL,
trang_thai BIT NOT NULL DEFAULT 1
)
GO

CREATE TABLE chat_lieu(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(50) NOT NULL,
ten NVARCHAR(100) NOT NULL,
mo_ta NVARCHAR(MAX) NULL,
ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
ngay_sua DATETIME NULL,
trang_thai BIT NOT NULL DEFAULT 1
)
GO

CREATE TABLE danh_muc(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(50) NOT NULL,
ten NVARCHAR(100) NOT NULL,
mo_ta NVARCHAR(MAX) NULL,
ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
ngay_sua DATETIME NULL,
trang_thai BIT NOT NULL DEFAULT 1
)
GO

CREATE TABLE san_pham(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(50) NOT NULL,
ten NVARCHAR(100) NOT NULL,
mo_ta NVARCHAR(MAX) NULL,
ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
ngay_sua DATETIME NULL,
id_hang INT NOT NULL,
id_chat_lieu INT NOT NULL,
id_danh_muc INT NOT NULL,
trang_thai BIT NOT NULL DEFAULT 1
)
GO

CREATE TABLE khuyen_mai(
  id INT IDENTITY(1,1) PRIMARY KEY,
  ma VARCHAR(10) NOT NULL,
  ten NVARCHAR(150) NOT NULL,
  loai NVARCHAR(20) NOT NULL, -- PERCENT/AMOUNT
  gia_tri DECIMAL(12,2) NOT NULL,
  ngay_bat_dau DATETIME2(0) NOT NULL DEFAULT GETDATE(),
  ngay_ket_thuc DATETIME2(0) NOT NULL,
  cho_phep_cong_don BIT DEFAULT 0,
  trang_thai BIT NOT NULL DEFAULT 1
)
GO

CREATE TABLE khuyen_mai_san_pham(
  id INT IDENTITY(1,1) PRIMARY KEY,
  id_khuyen_mai INT NOT NULL,
  id_san_pham INT NOT NULL,
  UNIQUE(id_khuyen_mai, id_san_pham)
)
GO

CREATE TABLE hinh_anh_san_pham(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
id_san_pham INT NOT NULL,
url VARCHAR(MAX) NOT NULL
)
GO

CREATE TABLE mau_sac(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(50) NOT NULL,
ten NVARCHAR(100) NOT NULL,
trang_thai BIT NOT NULL DEFAULT 1
)
GO

CREATE TABLE kich_thuoc(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(50) NOT NULL,
ten NVARCHAR(100) NOT NULL,
trang_thai BIT NOT NULL DEFAULT 1
)
GO

CREATE TABLE san_pham_chi_tiet(
id INT IDENTITY (1,1) NOT NULL PRIMARY KEY,
sku VARCHAR(100) NOT NULL,
gia_ban DECIMAL(18,2) NOT NULL,
so_luong INT NOT NULL,
hinh_anh VARCHAR(MAX),
id_mau_sac INT NOT NULL,
id_kich_thuoc INT NOT NULL,
id_san_pham INT NOT NULL,
trang_thai BIT NOT NULL DEFAULT 1
)
GO

CREATE TABLE san_pham_yeu_thich (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tai_khoan_id INT NOT NULL,
    san_pham_id INT NOT NULL,
    ngay_tao DATETIME DEFAULT GETDATE()
)
GO

CREATE TABLE danh_gia (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tai_khoan_id INT NOT NULL,
    san_pham_id INT NOT NULL,
    so_sao INT NOT NULL,
    noi_dung NVARCHAR(MAX),
    phan_loai NVARCHAR(255),
    danh_sach_hinh_anh NVARCHAR(MAX), 
    ngay_tao DATETIME DEFAULT GETDATE()
)
GO

CREATE TABLE chuc_vu(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(50) NOT NULL UNIQUE,
ten NVARCHAR(150) NOT NULL
)
GO

CREATE TABLE vai_tro (
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(50) NOT NULL UNIQUE,       
ten NVARCHAR(150) NOT NULL,           
mo_ta NVARCHAR(MAX) NULL,
trang_thai BIT NOT NULL DEFAULT 1,
ngay_tao DATETIME NOT NULL DEFAULT GETDATE()
)
GO

CREATE TABLE tai_khoan(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ten_dang_nhap VARCHAR(150) NOT NULL UNIQUE,
mat_khau VARCHAR(255) NOT NULL, 
id_vai_tro INT NOT NULL, 
trang_thai BIT DEFAULT 1
)
GO

CREATE TABLE nhan_vien(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(50) NOT NULL UNIQUE,
ho_ten NVARCHAR(150) NOT NULL,
gioi_tinh NVARCHAR(50) NULL,
ngay_sinh DATE NULL,
dia_chi NVARCHAR(MAX) NULL,
so_dien_thoai NVARCHAR(12) NULL,
email VARCHAR(150) NULL,
hinh_anh VARCHAR(MAX) NULL,
id_chuc_vu INT NOT NULL,
id_tai_khoan INT NULL, 
trang_thai BIT DEFAULT 1
)
GO

CREATE TABLE khach_hang(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(50) NOT NULL UNIQUE,
ho_ten NVARCHAR(150) NOT NULL DEFAULT N'Khách vãng lai',
gioi_tinh NVARCHAR(50) NULL,
ngay_sinh DATE NULL,
dia_chi NVARCHAR(MAX) NULL,
so_dien_thoai NVARCHAR(12) NULL,
email VARCHAR(150) NULL,
hinh_anh VARCHAR(MAX) NULL,
id_tai_khoan INT NULL, 
trang_thai BIT DEFAULT 1
)
GO

CREATE TABLE dia_chi_khach_hang (
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
id_khach_hang INT NOT NULL,
ho_ten_nhan NVARCHAR(150) NULL, 
sdt_nhan VARCHAR(15) NULL,      
dia_chi_chi_tiet NVARCHAR(MAX) NOT NULL, 
phuong_xa NVARCHAR(100) NULL,
quan_huyen NVARCHAR(100) NULL,
tinh_thanh_pho NVARCHAR(100) NULL,
ma_tinh INT NULL,
ma_huyen INT NULL,
ma_xa VARCHAR(50) NULL,
loai_dia_chi NVARCHAR(50) DEFAULT N'Nhà riêng', 
la_mac_dinh BIT DEFAULT 0, 
trang_thai BIT DEFAULT 1  
)
GO

CREATE TABLE thong_bao (
    id INT IDENTITY(1,1) PRIMARY KEY,
    tai_khoan_id INT NULL,  
    tieu_de NVARCHAR(255) NOT NULL,
    noi_dung NVARCHAR(MAX) NOT NULL,
    loai_thong_bao VARCHAR(50) NULL,
    da_doc BIT DEFAULT 0,
    da_xoa BIT DEFAULT 0,
    ngay_tao DATETIME DEFAULT GETDATE()
)
GO

CREATE TABLE ma_giam_gia(
id INT IDENTITY (1,1) NOT NULL PRIMARY KEY,
ma_code VARCHAR(100) NOT NULL,
ten NVARCHAR(100) NOT NULL,
loai_giam_gia VARCHAR(50) NOT NULL, 
gia_tri_giam_gia_toi_da DECIMAL(18,2) NULL,
gia_tri_giam_gia DECIMAL(18,2) NOT NULL,
dieu_kien DECIMAL(18,2) NOT NULL,
so_luong INT NOT NULL,
luot_su_dung INT NOT NULL DEFAULT 0,
ngay_bat_dau DATETIME NOT NULL DEFAULT GETDATE(),
ngay_ket_thuc DATETIME NOT NULL,
cho_phep_cong_don BIT DEFAULT 0,
trang_thai BIT NOT NULL DEFAULT 1,
CONSTRAINT CHK_ThoiGian CHECK (ngay_ket_thuc >= ngay_bat_dau)
)
GO

CREATE TABLE trang_thai_hoa_don (
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(20) NOT NULL,
ten NVARCHAR(50) NOT NULL
)
GO

CREATE TABLE hoa_don(
id INT IDENTITY (1,1) NOT NULL PRIMARY KEY,
ma VARCHAR(50) NOT NULL UNIQUE,
gia_tam_thoi DECIMAL(18,2) NOT NULL,
phi_van_chuyen DECIMAL(18,2) DEFAULT 0,
gia_tri_khuyen_mai DECIMAL(18,2) DEFAULT 0,
gia_tong DECIMAL(18,2) NOT NULL,
ten_nguoi_nhan NVARCHAR(100) NULL,
sdt_nhan VARCHAR(15) NULL,
dia_chi_giao_hang NVARCHAR(MAX) NULL,
phuong_thuc_thanh_toan NVARCHAR(225) NULL,
ghi_chu NVARCHAR(MAX) NULL,
id_nhan_vien INT  NULL,
id_khach_hang INT NULL,
id_ma_giam_gia INT NULL,
id_trang_thai_hoa_don INT NOT NULL,
loai_hoa_don INT DEFAULT 0, 
ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
ngay_thanh_toan DATETIME NULL
)
GO

CREATE TABLE hoa_don_chi_tiet(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
gia_tien DECIMAL(18,2) NOT NULL,
so_luong INT NOT NULL CHECK (so_luong > 0),
id_hoa_don INT NOT NULL,
id_san_pham_chi_tiet INT NOT NULL
)
GO

CREATE TABLE lich_su_hoa_don (
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
id_hoa_don INT NOT NULL,
id_trang_thai_hoa_don INT NOT NULL,
id_nhan_vien INT NULL, 
ghi_chu NVARCHAR(MAX) NULL, 
ngay_tao DATETIME NOT NULL DEFAULT GETDATE()
)
GO

CREATE TABLE thanh_toan (
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
id_hoa_don INT NOT NULL, 
so_tien DECIMAL(18,2) NOT NULL CHECK (so_tien > 0),
phuong_thuc NVARCHAR(50) NOT NULL, 
loai_thanh_toan NVARCHAR(20) NOT NULL DEFAULT N'THANH_TOAN',
trang_thai NVARCHAR(20) NOT NULL DEFAULT N'THANH_CONG',
ma_giao_dich NVARCHAR(100) NULL,
ngay_thanh_toan DATETIME NOT NULL DEFAULT GETDATE(),	
ghi_chu NVARCHAR(MAX) NULL,
id_nhan_vien INT NULL
)
GO

CREATE TABLE gio_hang(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
id_khach_hang INT NOT NULL
)
GO

CREATE TABLE gio_hang_chi_tiet(
id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
id_gio_hang INT NOT NULL,
id_san_pham_chi_tiet INT NOT NULL,
so_luong INT NOT NULL
)
GO


-- =========================================================================
-- PHẦN 2: THIẾT LẬP CÁC MỐI QUAN HỆ KHÓA NGOẠI (FOREIGN KEYS)
-- =========================================================================

-- Bảng san_pham
ALTER TABLE san_pham ADD CONSTRAINT FK_SanPham_Hang FOREIGN KEY (id_hang) REFERENCES hang(id);
ALTER TABLE san_pham ADD CONSTRAINT FK_SanPham_ChatLieu FOREIGN KEY (id_chat_lieu) REFERENCES chat_lieu(id);
ALTER TABLE san_pham ADD CONSTRAINT FK_SanPham_DanhMuc FOREIGN KEY (id_danh_muc) REFERENCES danh_muc(id);
GO

-- Bảng khuyen_mai_san_pham
ALTER TABLE khuyen_mai_san_pham ADD CONSTRAINT FK_KMSP_KhuyenMai FOREIGN KEY(id_khuyen_mai) REFERENCES khuyen_mai(id);
ALTER TABLE khuyen_mai_san_pham ADD CONSTRAINT FK_KMSP_SanPham FOREIGN KEY(id_san_pham) REFERENCES san_pham(id);
GO

-- Bảng hinh_anh_san_pham
ALTER TABLE hinh_anh_san_pham ADD CONSTRAINT FK_HinhAnhSP_SanPham FOREIGN KEY (id_san_pham) REFERENCES san_pham(id);
GO

-- Bảng san_pham_chi_tiet
ALTER TABLE san_pham_chi_tiet ADD CONSTRAINT FK_SPCT_MauSac FOREIGN KEY (id_mau_sac) REFERENCES mau_sac(id);
ALTER TABLE san_pham_chi_tiet ADD CONSTRAINT FK_SPCT_KichThuoc FOREIGN KEY (id_kich_thuoc) REFERENCES kich_thuoc(id);
ALTER TABLE san_pham_chi_tiet ADD CONSTRAINT FK_SPCT_SanPham FOREIGN KEY (id_san_pham) REFERENCES san_pham(id);
GO

-- Bảng san_pham_yeu_thich (Có ON DELETE CASCADE để tự dọn dẹp)
ALTER TABLE san_pham_yeu_thich ADD CONSTRAINT FK_SPYeuThich_TaiKhoan FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id) ON DELETE CASCADE;
ALTER TABLE san_pham_yeu_thich ADD CONSTRAINT FK_SPYeuThich_SanPham FOREIGN KEY (san_pham_id) REFERENCES san_pham(id) ON DELETE CASCADE;
GO

-- Bảng danh_gia (Có ON DELETE CASCADE để tự dọn dẹp)
ALTER TABLE danh_gia ADD CONSTRAINT FK_DanhGia_TaiKhoan FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id) ON DELETE CASCADE;
ALTER TABLE danh_gia ADD CONSTRAINT FK_DanhGia_SanPham FOREIGN KEY (san_pham_id) REFERENCES san_pham(id) ON DELETE CASCADE;
GO

-- Bảng tai_khoan
ALTER TABLE tai_khoan ADD CONSTRAINT FK_TaiKhoan_VaiTro FOREIGN KEY (id_vai_tro) REFERENCES vai_tro(id);
GO

-- Bảng nhan_vien
ALTER TABLE nhan_vien ADD CONSTRAINT FK_NhanVien_ChucVu FOREIGN KEY (id_chuc_vu) REFERENCES chuc_vu(id);
ALTER TABLE nhan_vien ADD CONSTRAINT FK_NhanVien_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan(id);
GO

-- Bảng khach_hang
ALTER TABLE khach_hang ADD CONSTRAINT FK_KhachHang_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan(id);
GO

-- Bảng dia_chi_khach_hang
ALTER TABLE dia_chi_khach_hang ADD CONSTRAINT FK_DCKH_KhachHang FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id);
GO

-- Bảng thong_bao
ALTER TABLE thong_bao ADD CONSTRAINT FK_ThongBao_TaiKhoan FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id);
GO

-- Bảng hoa_don
ALTER TABLE hoa_don ADD CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id);
ALTER TABLE hoa_don ADD CONSTRAINT FK_HoaDon_KhachHang FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id);
ALTER TABLE hoa_don ADD CONSTRAINT FK_HoaDon_MaGiamGia FOREIGN KEY (id_ma_giam_gia) REFERENCES ma_giam_gia(id);
ALTER TABLE hoa_don ADD CONSTRAINT FK_HoaDon_TrangThai FOREIGN KEY (id_trang_thai_hoa_don) REFERENCES trang_thai_hoa_don(id);
GO

-- Bảng hoa_don_chi_tiet
ALTER TABLE hoa_don_chi_tiet ADD CONSTRAINT FK_HDCT_HoaDon FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id);
ALTER TABLE hoa_don_chi_tiet ADD CONSTRAINT FK_HDCT_SPCT FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id);
GO

-- Bảng lich_su_hoa_don
ALTER TABLE lich_su_hoa_don ADD CONSTRAINT FK_LichSuHD_HoaDon FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id);
ALTER TABLE lich_su_hoa_don ADD CONSTRAINT FK_LichSuHD_TrangThai FOREIGN KEY (id_trang_thai_hoa_don) REFERENCES trang_thai_hoa_don(id);
ALTER TABLE lich_su_hoa_don ADD CONSTRAINT FK_LichSuHD_NhanVien FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id);
GO

-- Bảng thanh_toan
ALTER TABLE thanh_toan ADD CONSTRAINT FK_ThanhToan_HoaDon FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id);
ALTER TABLE thanh_toan ADD CONSTRAINT FK_ThanhToan_NhanVien FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id);
GO

-- Bảng gio_hang
ALTER TABLE gio_hang ADD CONSTRAINT FK_GioHang_KhachHang FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id);
GO

-- Bảng gio_hang_chi_tiet
ALTER TABLE gio_hang_chi_tiet ADD CONSTRAINT FK_GHCT_GioHang FOREIGN KEY (id_gio_hang) REFERENCES gio_hang(id);
ALTER TABLE gio_hang_chi_tiet ADD CONSTRAINT FK_GHCT_SPCT FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id);
GO

-- =========================================================================
-- PHẦN 3: DỮ LIỆU MẪU BAN ĐẦU
-- =========================================================================

INSERT INTO trang_thai_hoa_don (ma, ten) VALUES
('CHO_THANH_TOAN', N'Chờ thanh toán'),
('CHO_XAC_NHAN', N'Chờ xác nhận'),
('CHO_GIAO', N'Đang chuẩn bị'),
('DANG_GIAO', N'Đang giao hàng'),
('HOAN_THANH', N'Hoàn thành'),
('DA_HUY', N'Đã hủy');
GO