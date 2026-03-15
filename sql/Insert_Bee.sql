INSERT INTO hang (ma, ten, mo_ta, ngay_tao, trang_thai)
VALUES 
('NIKE', N'Nike', N'Thương hiệu thể thao hàng đầu thế giới, nổi tiếng với các dòng áo thun và áo khoác thể thao.', GETDATE(), 1),
('ADIDAS', N'Adidas', N'Hãng thời trang thể thao Đức với biểu tượng 3 sọc đặc trưng.', GETDATE(), 1),
('UNIQLO', N'Uniqlo', N'Thương hiệu bán lẻ thời trang tối giản đến từ Nhật Bản, nổi tiếng với dòng áo giữ nhiệt Heattech và AIRism.', GETDATE(), 1),
('ZARA', N'Zara', N'Thương hiệu thời trang nhanh (Fast Fashion) nổi tiếng từ Tây Ban Nha.', GETDATE(), 1),
('HM', N'H&M', N'Hãng thời trang đa quốc gia Thụy Điển, chuyên cung cấp áo thun và sơ mi thời thượng giá tốt.', GETDATE(), 1),
('LV', N'Louis Vuitton', N'Phân khúc thời trang xa xỉ với các mẫu áo thiết kế cao cấp.', GETDATE(), 1),
('GUCCI', N'Gucci', N'Thương hiệu thời trang cao cấp từ Ý, nổi bật với các mẫu áo họa tiết độc bản.', GETDATE(), 1),
('LACOSTE', N'Lacoste', N'Nổi tiếng với dòng áo thun Polo có biểu tượng hình con cá sấu.', GETDATE(), 1),
('VIETTEN', N'Việt Tiến', N'Thương hiệu quốc gia Việt Nam, chuyên về áo sơ mi công sở nam.', GETDATE(), 1),
('CANIFA', N'Canifa', N'Thương hiệu thời trang Việt Nam phổ biến với các dòng áo len và đồ cotton gia đình.', GETDATE(), 1);

INSERT INTO chat_lieu (ma, ten, mo_ta, ngay_tao, trang_thai)
VALUES 
('COTTON', N'Cotton 100%', N'Vải bông tự nhiên, thấm hút mồ hôi cực tốt, thường dùng cho áo thun.', GETDATE(), 1),
('LINEN', N'Vải Lanh (Linen)', N'Chất liệu tự nhiên, thoáng mát, có độ nhăn tự nhiên đặc trưng, phù hợp mùa hè.', GETDATE(), 1),
('POLY', N'Polyester', N'Vải sợi tổng hợp, ít nhăn, bền màu, thường dùng cho áo thể thao.', GETDATE(), 1),
('SILK', N'Lụa Tơ Tằm', N'Bề mặt bóng mịn, sang trọng, mang lại cảm giác mát mẻ và nhẹ nhàng.', GETDATE(), 1),
('FLANNEL', N'Nỉ/Dạ (Flannel)', N'Vải dày dặn, giữ ấm tốt, thường dùng cho các loại áo sơ mi kẻ caro mùa đông.', GETDATE(), 1),
('CHIFFON', N'Voan (Chiffon)', N'Mỏng, nhẹ, có độ rủ cao, thường dùng cho áo kiểu hoặc sơ mi nữ.', GETDATE(), 1),
('DENIM', N'Vải Bò (Denim)', N'Bền bỉ, cá tính, chuyên dùng cho áo khoác bò hoặc sơ mi denim.', GETDATE(), 1),
('KHAKI', N'Kaki', N'Vải có độ cứng cáp, phù hợp cho áo khoác hoặc các loại áo bảo hộ lao động.', GETDATE(), 1),
('VISCOSE', N'Vải Viscose', N'Sợi nhân tạo từ bột gỗ, mềm mại như lụa nhưng giá thành rẻ hơn.', GETDATE(), 1),
('SPANDEX', N'Spandex/Lycra', N'Khả năng co giãn cực cao, thường được pha với cotton để làm áo ôm sát cơ thể.', GETDATE(), 1);

INSERT INTO danh_muc (ma, ten, mo_ta, ngay_tao, trang_thai)
VALUES 
('AO_THUN', N'Áo Thun (T-Shirt)', N'Các loại áo cổ tròn, cổ tim chất liệu cotton co giãn.', GETDATE(), 1),
('AO_POLO', N'Áo Polo', N'Áo thun có cổ, phong cách lịch sự nhưng vẫn năng động.', GETDATE(), 1),
('AO_SO_MI', N'Áo Sơ Mi', N'Bao gồm sơ mi công sở, sơ mi kiểu và sơ mi flannel.', GETDATE(), 1),
('AO_KHOAC', N'Áo Khoác', N'Các loại áo bảo vệ bên ngoài như Bomber, Jacket, áo gió.', GETDATE(), 1),
('AO_HOODIE', N'Áo Hoodie', N'Áo nỉ có mũ, phong cách thời trang đường phố (Streetwear).', GETDATE(), 1),
('AO_LEN', N'Áo Len', N'Các sản phẩm dệt kim, giữ ấm tốt cho mùa đông.', GETDATE(), 1),
('AO_VEST', N'Áo Vest/Blazer', N'Dòng áo khoác trang trọng cho các sự kiện hoặc đi làm công sở.', GETDATE(), 1),
('AO_TANKTOP', N'Áo Ba Lỗ (Tanktop)', N'Áo không tay, phù hợp mặc lót hoặc tập thể thao.', GETDATE(), 1),
('AO_TRE_EM', N'Áo Trẻ Em', N'Các loại áo dành riêng cho lứa tuổi thiếu nhi.', GETDATE(), 1),
('AO_CACH_TAN', N'Áo Cách Tân/Dài', N'Các loại áo mang hơi hướng truyền thống nhưng thiết kế hiện đại.', GETDATE(), 1);

INSERT INTO san_pham (ma, ten, mo_ta, id_hang, id_chat_lieu, id_danh_muc, ngay_tao, trang_thai)
VALUES 
-- NHÓM THỂ THAO NĂNG ĐỘNG (NIKE, ADIDAS) - Ưu tiên Poly, Spandex, Cotton
('SP016', N'Áo Khoác Gió Nike Windrunner', N'Thiết kế chữ V kinh điển, chống gió tốt.', 1, 3, 4, GETDATE(), 1),
('SP017', N'Áo Thun Chạy Bộ Adidas OwnTheRun', N'Chất liệu tái chế bảo vệ môi trường.', 2, 3, 1, GETDATE(), 1),
('SP018', N'Áo Polo Nike Golf Tiger Woods', N'Dòng áo Polo cao cấp cho golfer.', 1, 10, 2, GETDATE(), 1),
('SP019', N'Áo Tanktop Nike Pro Tight', N'Áo ba lỗ ôm sát hỗ trợ cơ bắp.', 1, 10, 8, GETDATE(), 1),
('SP020', N'Áo Hoodie Adidas Essentials', N'Áo nỉ phong cách thể thao dạo phố.', 2, 5, 5, GETDATE(), 1),
('SP021', N'Áo Khoác Adidas Track Jacket', N'Áo khoác 3 sọc huyền thoại.', 2, 3, 4, GETDATE(), 1),
('SP022', N'Áo Thun Nike SB Logo', N'Dòng áo thun dành cho dân trượt ván.', 1, 1, 1, GETDATE(), 1),
('SP023', N'Áo Tanktop Adidas Basketball', N'Áo bóng rổ lỗ thoáng khí.', 2, 3, 8, GETDATE(), 1),
('SP024', N'Áo Khoác Nike Jordan Flight', N'Áo khoác bóng rổ phong cách Retro.', 1, 3, 4, GETDATE(), 1),
('SP025', N'Áo Thun Adidas Techfit', N'Lớp áo lót nén cơ cực tốt.', 2, 10, 1, GETDATE(), 1),

-- NHÓM CÔNG SỞ & LỊCH LÃM (VIỆT TIẾN, LACOSTE) - Ưu tiên Cotton, Linen, Lụa
('SP026', N'Áo Sơ Mi Việt Tiến Bamboo', N'Vải sợi tre kháng khuẩn, ít nhăn.', 9, 1, 3, GETDATE(), 1),
('SP027', N'Áo Polo Lacoste Slim Fit', N'Dáng ôm trẻ trung, vải cá sấu mịn.', 8, 1, 2, GETDATE(), 1),
('SP028', N'Áo Sơ Mi Việt Tiến Sọc Caro', N'Họa tiết trẻ trung cho dân văn phòng.', 9, 1, 3, GETDATE(), 1),
('SP029', N'Áo Polo Lacoste L.12.12', N'Mẫu Polo nguyên bản từ năm 1933.', 8, 1, 2, GETDATE(), 1),
('SP030', N'Áo Sơ Mi Việt Tiến Linen', N'Sơ mi đũi nhẹ nhàng cho mùa hè.', 9, 2, 3, GETDATE(), 1),
('SP031', N'Áo Vest Việt Tiến Smart Casual', N'Vest nhẹ không lót, mặc hàng ngày.', 9, 8, 7, GETDATE(), 1),
('SP032', N'Áo Polo Lacoste Sport', N'Dòng polo chuyên dụng cho Tennis.', 8, 3, 2, GETDATE(), 1),
('SP033', N'Áo Sơ Mi Việt Tiến Cổ Trụ', N'Thiết kế hiện đại, phá cách.', 9, 1, 3, GETDATE(), 1),
('SP034', N'Áo Polo Lacoste Long Sleeve', N'Áo polo dài tay cho mùa thu.', 8, 1, 2, GETDATE(), 1),
('SP035', N'Áo Vest Việt Tiến Premium', N'Vest cao cấp cho quý ông thượng lưu.', 9, 8, 7, GETDATE(), 1),

-- NHÓM THỜI TRANG CƠ BẢN (UNIQLO, CANIFA) - Ưu tiên Viscose, Cotton, Nỉ
('SP036', N'Áo Thun Uniqlo U Cổ Tròn', N'Dáng Boxy hiện đại, vải dày dặn.', 3, 1, 1, GETDATE(), 1),
('SP037', N'Áo Len Canifa Cashmere-like', N'Sợi len mềm như lông cừu Cashmere.', 10, 5, 6, GETDATE(), 1),
('SP038', N'Áo Hoodie Canifa Basic', N'Nỉ bông mềm mại, nhiều màu sắc.', 10, 5, 5, GETDATE(), 1),
('SP039', N'Áo Sơ Mi Uniqlo Oxford', N'Vải Oxford bền bỉ, càng mặc càng mềm.', 3, 1, 3, GETDATE(), 1),
('SP040', N'Áo Len Uniqlo Cổ Lọ', N'Giữ ấm cổ tuyệt đối cho mùa đông.', 3, 5, 6, GETDATE(), 1),
('SP041', N'Áo Khoác Siêu Nhẹ Uniqlo Ultra Light Down', N'Áo phao lông vũ gấp gọn bỏ túi.', 3, 3, 4, GETDATE(), 1),
('SP042', N'Áo Thun Trẻ Em Canifa Mickey', N'Họa tiết hoạt hình ngộ nghĩnh.', 10, 1, 9, GETDATE(), 1),
('SP043', N'Áo Polo Canifa Cotton Pink', N'Polo màu hồng nhạt trẻ trung.', 10, 1, 2, GETDATE(), 1),
('SP044', N'Áo Cardigan Uniqlo UV Protection', N'Áo khoác len mỏng chống tia UV.', 3, 9, 6, GETDATE(), 1),
('SP045', N'Áo Thun Canifa Marvel', N'Bản quyền hình ảnh siêu anh hùng.', 10, 1, 1, GETDATE(), 1),

-- NHÓM THỜI TRANG NHANH (ZARA, H&M) - Đa dạng chất liệu
('SP046', N'Áo Sơ Mi Zara Floral Print', N'Họa tiết hoa lá nhiệt đới sặc sỡ.', 4, 9, 3, GETDATE(), 1),
('SP047', N'Áo Khoác Bomber H&M Satin', N'Vải bóng, phong cách thời trang trẻ.', 5, 3, 4, GETDATE(), 1),
('SP048', N'Áo Thun Zara Oversized', N'Form rộng thùng thình cực chất.', 4, 1, 1, GETDATE(), 1),
('SP049', N'Áo Blazer H&M Slim Fit', N'Áo giả vest cho phong cách trẻ trung.', 5, 8, 7, GETDATE(), 1),
('SP050', N'Áo Sơ Mi Denim H&M', N'Chất denim wash nhẹ bụi bặm.', 5, 7, 3, GETDATE(), 1),
('SP051', N'Áo Hoodie Zara Graffiti', N'Họa tiết vẽ tay độc đáo.', 4, 5, 5, GETDATE(), 1),
('SP052', N'Áo Len H&M Knitwear', N'Kiểu đan vặn thừng cổ điển.', 5, 5, 6, GETDATE(), 1),
('SP053', N'Áo Tanktop Zara Ribbed', N'Vải gân co giãn, tôn dáng.', 4, 10, 8, GETDATE(), 1),
('SP054', N'Áo Khoác Da Zara Faux Leather', N'Da nhân tạo, phong cách Biker.', 4, 3, 4, GETDATE(), 1),
('SP055', N'Áo Cách Tân H&M Lunar New Year', N'Phiên bản giới hạn dịp Tết.', 5, 4, 10, GETDATE(), 1),

-- NHÓM XA XỈ (GUCCI, LV) - Ưu tiên Lụa, Da, Kaki cao cấp
('SP056', N'Áo Thun Gucci Web Detail', N'Có dải sọc xanh đỏ đặc trưng ở cổ.', 7, 1, 1, GETDATE(), 1),
('SP057', N'Áo Sơ Mi LV Monogram Silk', N'Lụa tự nhiên 100%, in logo chìm.', 6, 4, 3, GETDATE(), 1),
('SP058', N'Áo Khoác Gucci Bomber Tiger', N'Thêu hình hổ thủ công sau lưng.', 7, 3, 4, GETDATE(), 1),
('SP059', N'Áo Vest LV Tuxedo', N'Lễ phục dạ tiệc cao cấp.', 6, 8, 7, GETDATE(), 1),
('SP060', N'Áo Sơ Mi Gucci Silk Shirt', N'Họa tiết baroque cổ điển.', 7, 4, 3, GETDATE(), 1),
('SP061', N'Áo Hoodie LV Damier', N'Họa tiết bàn cờ biểu tượng.', 6, 5, 5, GETDATE(), 1),
('SP062', N'Áo Polo Gucci Double G', N'Cúc áo mạ vàng sang trọng.', 7, 1, 2, GETDATE(), 1),
('SP063', N'Áo Khoác LV Leather Sleeve', N'Phối tay da cừu thật.', 6, 8, 4, GETDATE(), 1),
('SP064', N'Áo Cách Tân Gucci Silk Red', N'Lụa đỏ thêu họa tiết rồng.', 7, 4, 10, GETDATE(), 1),
('SP065', N'Áo Blazer LV Tailored', N'Cắt may thủ công tại Ý.', 6, 8, 7, GETDATE(), 1);

INSERT INTO khuyen_mai (ma, ten, loai, gia_tri, ngay_bat_dau, ngay_ket_thuc, cho_phep_cong_don, trang_thai)
VALUES 
('SUMMER26', N'Xả kho quần áo hè 2026', 'PERCENT', 20.00, '2026-03-01', '2026-06-30', 1, 1),
('SOMI_PRO', N'Tuần lễ Sơ mi công sở', 'AMOUNT', 30000.00, '2026-03-10', '2026-03-20', 1, 1),
('LUX_SALE', N'Giảm giá đồ hiệu cao cấp', 'PERCENT', 10.00, '2026-03-15', '2026-03-25', 0, 1);

-- Gắn 20% giảm giá SUMMER26 cho các sản phẩm Áo thun (Nike, Adidas, Uniqlo)
INSERT INTO khuyen_mai_san_pham (id_khuyen_mai, id_san_pham)
VALUES 
(1, 1), (1, 5), (1, 9), (1, 17), (1, 22), (1, 36);

-- Gắn giảm 30k SOMI_PRO cho các loại áo sơ mi (Việt Tiến, Zara)
INSERT INTO khuyen_mai_san_pham (id_khuyen_mai, id_san_pham)
VALUES 
(2, 4), (2, 8), (2, 26), (2, 28), (2, 30), (2, 33);

-- Gắn 10% giảm LUX_SALE cho các sản phẩm hàng hiệu (Gucci, LV)
INSERT INTO khuyen_mai_san_pham (id_khuyen_mai, id_san_pham)
VALUES 
(3, 12), (3, 13), (3, 56), (3, 57), (3, 59), (3, 65);

INSERT INTO ma_giam_gia (ma_code, ten, loai_giam_gia, gia_tri_giam_gia_toi_da, gia_tri_giam_gia, dieu_kien, so_luong, luot_su_dung, ngay_bat_dau, ngay_ket_thuc, cho_phep_cong_don, trang_thai)
VALUES 
(
  'FREE50K', N'Voucher giảm 50k cho đơn từ 500k', 
  'fixed', NULL, 50000.00, 500000.00, 100, 5, '2026-01-01', '2026-12-31', 1, 1
),
(
  'CHUC_MUNG', N'Mã mừng ngày 15/3', 
  'percentage', 100000.00, 15.00, 200000.00, 50, 0, '2026-03-15', '2026-03-20', 1, 1
),
(
  'VIP_ONLY', N'Mã đặc biệt cho khách VIP', 
  'percentage', 500000.00, 30.00, 1000000.00, 10, 2, '2026-03-01', '2026-12-31', 0, 1
);

WITH Ten_Anh AS (
    -- Tạo 15 số thứ tự
    SELECT 1 AS STT UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
    UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
)
INSERT INTO hinh_anh_san_pham (id_san_pham, url)
SELECT 
    sp.id,
    -- Cấu trúc URL Cloudinary mẫu
    'https://res.cloudinary.com/gemini_fashion/image/upload/products/' + sp.ma + '_' + CAST(ta.STT AS VARCHAR) + '.jpg'
FROM san_pham sp
CROSS JOIN Ten_Anh ta;

INSERT INTO mau_sac (ma, ten, trang_thai)
VALUES 
('BLACK', N'Đen', 1),
('WHITE', N'Trắng', 1),
('NAVY', N'Xanh Than', 1),
('GREY', N'Ghi Xám', 1),
('BEIGE', N'Màu Be', 1);

INSERT INTO kich_thuoc (ma, ten, trang_thai)
VALUES 
('S', N'Size S', 1),
('M', N'Size M', 1),
('L', N'Size L', 1),
('XL', N'Size XL', 1),
('XXL', N'Size XXL', 1),
('FREESIZE', N'Freesize', 1);

WITH BienThe_Random AS (
    -- Chọn ngẫu nhiên 4 biến thể cho mỗi sản phẩm từ các bảng màu và size đã có
    SELECT 
        sp.id AS id_san_pham,
        sp.ma AS ma_sp,
        m.id AS id_mau,
        m.ma AS ma_mau,
        k.id AS id_size,
        k.ma AS ma_size,
        -- Tạo giá bán logic: Áo hiệu (Gucci/LV) thì đắt, áo thường thì rẻ
        CASE 
            WHEN sp.id_hang IN (6, 7) THEN 15000000 -- Hàng xa xỉ
            WHEN sp.id_hang IN (1, 2, 8) THEN 1200000 -- Hàng hiệu thể thao/Polo
            ELSE 350000 -- Hàng phổ thông
        END AS gia_co_ban,
        ROW_NUMBER() OVER(PARTITION BY sp.id ORDER BY NEWID()) as Rank_STT
    FROM san_pham sp
    CROSS JOIN mau_sac m
    CROSS JOIN kich_thuoc k
    -- Loại bỏ Freesize cho các dòng Sơ mi/Polo để tăng tính thực tế
    WHERE NOT (sp.id_danh_muc IN (2, 3) AND k.ma = 'FREESIZE')
)
INSERT INTO san_pham_chi_tiet (sku, gia_ban, so_luong, hinh_anh, id_mau_sac, id_kich_thuoc, id_san_pham, trang_thai)
SELECT 
    ma_sp + '-' + ma_mau + '-' + ma_size AS sku,
    gia_co_ban,
    100 AS so_luong, -- Số lượng tồn kho mặc định
    'https://res.cloudinary.com/gemini_fashion/image/upload/products/' + ma_sp + '_1.jpg' AS hinh_anh,
    id_mau,
    id_size,
    id_san_pham,
    1
FROM BienThe_Random
WHERE Rank_STT <= 4; -- Mỗi sản phẩm lấy tối đa 4 biến thể ngẫu nhiên

-- Chèn Vai trò hệ thống
INSERT INTO vai_tro (ma, ten, mo_ta)
VALUES 
('ROLE_ADMIN', N'Quản trị viên', N'Toàn quyền hệ thống'),
('ROLE_STAFF', N'Nhân viên', N'Quản lý bán hàng và kho'),
('ROLE_CUSTOMER', N'Khách hàng', N'Người mua hàng online');

-- Chèn Chức vụ (Dành cho bảng nhân viên)
INSERT INTO chuc_vu (ma, ten)
VALUES 
('GD', N'Giám đốc'),
('NVBH', N'Nhân viên bán hàng');

INSERT INTO tai_khoan (ten_dang_nhap, mat_khau, id_vai_tro, trang_thai)
VALUES 
('admin', '$2a$10$XXXXXXXXXXXXXXXXXXXXXuokona1LX1DXTJY2.0f7XC.y2NDQmXR6', 1, 1),   -- Mật khẩu: admin123
('nhanvien', '$2a$10$YYYYYYYYYYYYYYYYYYYYYuxf9iotiao2onb9GSBlp02HclF5Sg6Oy', 2, 1), -- Mật khẩu: staff123
('khachhang', '$2a$10$N9X1X2X3X4X5X6X7X8X9Xub7teIlRWY52AdtRc421AKIb402q.UOi', 3, 1); -- Mật khẩu: 123456
GO

-- 1. Chèn Admin (Gắn với tài khoản admin_boss)
INSERT INTO nhan_vien (ma, ho_ten, gioi_tinh, ngay_sinh, dia_chi, so_dien_thoai, email, id_chuc_vu, id_tai_khoan)
VALUES 
('NV001', N'Nguyễn Quản Trị', N'Nam', '1990-01-01', N'Hà Nội', '0988123456', 'admin@shop.com', 1, 1);

-- 2. Chèn Nhân viên (Gắn với tài khoản staff_01)
INSERT INTO nhan_vien (ma, ho_ten, gioi_tinh, ngay_sinh, dia_chi, so_dien_thoai, email, id_chuc_vu, id_tai_khoan)
VALUES 
('NV002', N'Trần Thị Bán Hàng', N'Nữ', '1995-05-20', N'Vĩnh Phúc', '0977654321', 'staff1@shop.com', 2, 2);

-- 3. Chèn Khách hàng (Gắn với tài khoản customer_vip)
INSERT INTO khach_hang (ma, ho_ten, gioi_tinh, ngay_sinh, dia_chi, so_dien_thoai, email, id_tai_khoan)
VALUES 
('KH001', N'Lê Văn Khách', N'Nam', '2000-12-12', N'TP. Hồ Chí Minh', '0911222333', 'customer@gmail.com', 3);

-- Chèn địa chỉ cho khách hàng trên
INSERT INTO dia_chi_khach_hang (id_khach_hang, ho_ten_nhan, sdt_nhan, dia_chi_chi_tiet, quan_huyen, tinh_thanh_pho, la_mac_dinh)
VALUES 
(1, N'Lê Văn Khách', '0911222333', N'Số 10, Đường Hoa Hồng', N'Quận 1', N'TP. Hồ Chí Minh', 1);