Table_Bee.sql
﻿CREATE DATABASE beemate
GO

USE beemate
GO

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
                         trang_thai BIT NOT NULL DEFAULT 1,
                         FOREIGN KEY (id_hang) REFERENCES hang(id),
                         FOREIGN KEY (id_chat_lieu) REFERENCES chat_lieu(id),
                         FOREIGN KEY (id_danh_muc) REFERENCES danh_muc(id)
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
                                    FOREIGN KEY(id_khuyen_mai) REFERENCES khuyen_mai(id),
                                    FOREIGN KEY(id_san_pham) REFERENCES san_pham(id),
                                    UNIQUE(id_khuyen_mai,id_san_pham)
)
    GO

CREATE TABLE hinh_anh_san_pham(
                                  id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                  id_san_pham INT NOT NULL,
                                  url VARCHAR(MAX) NOT NULL,
FOREIGN KEY (id_san_pham) REFERENCES san_pham(id)
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
trang_thai BIT NOT NULL DEFAULT 1,
FOREIGN KEY (id_mau_sac) REFERENCES mau_sac(id),
FOREIGN KEY (id_kich_thuoc) REFERENCES kich_thuoc(id),
FOREIGN KEY (id_san_pham) REFERENCES san_pham(id)
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
                         ma VARCHAR(50) NOT NULL UNIQUE,       -- ROLE_ADMIN, ROLE_STAFF, ROLE_CUSTOMER
                         ten NVARCHAR(150) NOT NULL,           -- Quản trị, Nhân viên, Khách hàng
                         mo_ta NVARCHAR(MAX) NULL,
                         trang_thai BIT NOT NULL DEFAULT 1,
                         ngay_tao DATETIME NOT NULL DEFAULT GETDATE()
)
    GO

CREATE TABLE tai_khoan(
                          id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                          ten_dang_nhap VARCHAR(150) NOT NULL UNIQUE,
                          mat_khau VARCHAR(255) NOT NULL, -- Độ dài 255 để lưu mật khẩu đã mã hóa BCrypt
                          id_vai_tro INT NOT NULL, -- Lưu: 'ROLE_STAFF', 'ROLE_CUSTOMER', 'ROLE_ADMIN'
                          trang_thai BIT DEFAULT 1,
                          FOREIGN KEY (id_vai_tro) REFERENCES vai_tro(id)
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
id_tai_khoan INT NULL, -- Liên kết để đăng nhập
trang_thai BIT DEFAULT 1,
FOREIGN KEY (id_chuc_vu) REFERENCES chuc_vu(id),
FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan(id)
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
id_tai_khoan INT NULL, -- Liên kết để khách đăng nhập online
trang_thai BIT DEFAULT 1,
FOREIGN KEY (id_tai_khoan) REFERENCES tai_khoan(id)
)
    GO

CREATE TABLE dia_chi_khach_hang (
                                    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                    id_khach_hang INT NOT NULL,
                                    ho_ten_nhan NVARCHAR(150) NULL, -- Tên người nhận (có thể khác tên chủ tài khoản)
                                    sdt_nhan VARCHAR(15) NULL,      -- Số điện thoại người nhận
                                    dia_chi_chi_tiet NVARCHAR(MAX) NOT NULL, -- Số nhà, tên đường...
                                    phuong_xa NVARCHAR(100) NULL,
                                    quan_huyen NVARCHAR(100) NULL,
                                    tinh_thanh_pho NVARCHAR(100) NULL,
                                    loai_dia_chi NVARCHAR(50) DEFAULT N'Nhà riêng', -- Nhà riêng hoặc Văn phòng
                                    la_mac_dinh BIT DEFAULT 0, -- 1 nếu là địa chỉ mặc định
                                    trang_thai BIT DEFAULT 1,  -- 0 nếu đã xóa (soft delete)
                                    FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id)
)
    GO

CREATE TABLE ma_giam_gia(
                            id INT IDENTITY (1,1) NOT NULL PRIMARY KEY,
                            ma_code VARCHAR(100) NOT NULL,
                            ten NVARCHAR(100) NOT NULL,
                            loai_giam_gia VARCHAR(50) NOT NULL, --Lưu giá trị: 'fixed' hoặc 'percentage'
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
                        loai_hoa_don INT DEFAULT 0, -- 0 TẠI QUẦY, 1 ONLINE
                        ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
                        ngay_thanh_toan DATETIME NULL,
                        FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id),
                        FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
                        FOREIGN KEY (id_ma_giam_gia) REFERENCES ma_giam_gia(id),
                        FOREIGN KEY (id_trang_thai_hoa_don) REFERENCES trang_thai_hoa_don(id)
)
    GO

CREATE TABLE hoa_don_chi_tiet(
                                 id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                 gia_tien DECIMAL(18,2) NOT NULL,
                                 so_luong INT NOT NULL CHECK (so_luong > 0),
                                 id_hoa_don INT NOT NULL,
                                 id_san_pham_chi_tiet INT NOT NULL,
                                 FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
                                 FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id)
)
    GO

CREATE TABLE lich_su_hoa_don (
                                 id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                 id_hoa_don INT NOT NULL,
                                 id_trang_thai_hoa_don INT NOT NULL,
                                 id_nhan_vien INT NULL, -- Người thực hiện thay đổi (null nếu là hệ thống hoặc khách hủy)
                                 ghi_chu NVARCHAR(MAX) NULL, -- Lý do đổi trạng thái (vídụ: Khách không nghe máy, Hoàn hàng...)
                                 ngay_tao DATETIME NOT NULL DEFAULT GETDATE(),
                                 FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
                                 FOREIGN KEY (id_trang_thai_hoa_don) REFERENCES trang_thai_hoa_don(id),
                                 FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id)
)
    GO

CREATE TABLE thanh_toan (
                            id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                            id_hoa_don INT NOT NULL, -- Hóa đơn được thanh toán
                            so_tien DECIMAL(18,2) NOT NULL CHECK (so_tien > 0),
                            phuong_thuc NVARCHAR(50) NOT NULL,
-- 'TIEN_MAT', 'CHUYEN_KHOAN', 'VNPAY', 'MOMO', 'COD'
                            loai_thanh_toan NVARCHAR(20) NOT NULL DEFAULT N'THANH_TOAN',
-- 'THANH_TOAN', 'HOAN_TIEN'
                            trang_thai NVARCHAR(20) NOT NULL DEFAULT N'THANH_CONG',
-- 'CHO_XU_LY', 'THANH_CONG', 'THAT_BAI'
                            ma_giao_dich NVARCHAR(100) NULL,
                            ngay_thanh_toan DATETIME NOT NULL DEFAULT GETDATE(),
                            ghi_chu NVARCHAR(MAX) NULL,
                            id_nhan_vien INT NULL,
-- Nhân viên thu tiền (null nếu online)
                            FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
                            FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id)
)
    GO

CREATE TABLE gio_hang(
                         id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                         id_khach_hang INT NOT NULL,
                         FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id)
)
    GO

CREATE TABLE gio_hang_chi_tiet(
                                  id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                  id_gio_hang INT NOT NULL,
                                  id_san_pham_chi_tiet INT NOT NULL,
                                  so_luong INT NOT NULL,
                                  FOREIGN KEY (id_gio_hang) REFERENCES gio_hang(id),
                                  FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id)
)



    INSERT INTO trang_thai_hoa_don (ma, ten) VALUES
('CHO_THANH_TOAN', 'Chờ thanh toán'),
('HOAN_THANH',     'Hoàn thành'),
('DA_HUY',         'Đã hủy');

INSERT INTO trang_thai_hoa_don (ma, ten) VALUES
                                             ('CHO_XAC_NHAN', 'Chờ xác nhận'),
                                             ('CHO_GIAO', 'Chờ giao hàng'),
                                             ('DANG_GIAO', 'Đang giao hàng');

UPDATE trang_thai_hoa_don SET ten = N'Chờ thanh toán' WHERE ma = 'CHO_THANH_TOAN';
UPDATE trang_thai_hoa_don SET ten = N'Chờ xác nhận' WHERE ma = 'CHO_XAC_NHAN';
UPDATE trang_thai_hoa_don SET ten = N'Đang chuẩn bị' WHERE ma = 'CHO_GIAO';
UPDATE trang_thai_hoa_don SET ten = N'Đang giao hàng' WHERE ma = 'DANG_GIAO';
UPDATE trang_thai_hoa_don SET ten = N'Hoàn thành' WHERE ma = 'HOAN_THANH';
UPDATE trang_thai_hoa_don SET ten = N'Đã hủy' WHERE ma = 'DA_HUY';