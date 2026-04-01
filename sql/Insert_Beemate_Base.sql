INSERT INTO trang_thai_hoa_don (ma, ten) VALUES
('CHO_THANH_TOAN', N'Chờ thanh toán'),
('CHO_XAC_NHAN', N'Chờ xác nhận'),
('DA_XAC_NHAN', N'Đã xác nhận'),
('CHO_GIAO', N'Chờ giao hàng'),
('DANG_GIAO', N'Đang giao hàng'),
('HOAN_THANH', N'Hoàn thành'),
('DA_HUY', N'Đã hủy'),
('GIAO_THAT_BAI', N'Giao thất bại / Hoàn hàng'),
('YEU_CAU_TRA_HANG', N'Yêu cầu trả hàng'),
('TU_CHOI_TRA_HANG', N'Từ chối trả hàng'),
('DA_HOAN_TIEN', N'Đã hoàn tiền');
GO


INSERT INTO vai_tro (ma, ten)
VALUES 
('ROLE_ADMIN', N'Quản trị viên'),
('ROLE_STAFF', N'Nhân viên'),
('ROLE_CUSTOMER', N'Khách hàng');
GO

INSERT INTO chuc_vu (ma, ten)
VALUES 
('QUAN_LY', N'Quản lý'),
('NHAN_VIEN_BAN_HANG', N'Nhân viên bán hàng');
GO

INSERT INTO tai_khoan (ten_dang_nhap, mat_khau, id_vai_tro, trang_thai)
VALUES 
('admin', '$2a$10$XXXXXXXXXXXXXXXXXXXXXuokona1LX1DXTJY2.0f7XC.y2NDQmXR6', 1, 1),   
('nhanvien', '$2a$10$YYYYYYYYYYYYYYYYYYYYYuxf9iotiao2onb9GSBlp02HclF5Sg6Oy', 2, 1)
GO

INSERT INTO nhan_vien (ma, ho_ten, gioi_tinh, ngay_sinh, dia_chi, so_dien_thoai, email, id_chuc_vu, id_tai_khoan)
VALUES 
('NV001', N'Nguyễn Quản Trị', N'Nam', '1990-01-01', N'Hà Nội', '0988123456', 'admin@shop.com', 1, 1);

INSERT INTO nhan_vien (ma, ho_ten, gioi_tinh, ngay_sinh, dia_chi, so_dien_thoai, email, id_chuc_vu, id_tai_khoan)
VALUES 
('NV002', N'Trần Thị Bán Hàng', N'Nữ', '1995-05-20', N'Phúc Yên, Hà Nội', '0977654321', 'staff1@shop.com', 2, 2);