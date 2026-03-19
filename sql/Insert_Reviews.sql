-- =========================================================================
-- TỰ ĐỘNG SINH 650 ĐÁNH GIÁ (10 ĐÁNH GIÁ MỖI SẢN PHẨM) CHIA ĐỀU CHO KHÁCH HÀNG
-- =========================================================================

-- Xóa dữ liệu cũ (nếu có) để tránh rác trước khi chạy
DELETE FROM danh_gia;
GO

-- Chèn dữ liệu tự động
INSERT INTO danh_gia (tai_khoan_id, san_pham_id, so_sao, noi_dung, phan_loai, danh_sach_hinh_anh, ngay_tao)
SELECT 
    -- 1. Random Tài khoản khách hàng (Từ ID 3 đến ID 102)
    ABS(CHECKSUM(NEWID())) % 100 + 3 AS tai_khoan_id,
    
    -- 2. ID Sản phẩm
    sp.id AS san_pham_id,
    
    -- 3. Random Số sao (80% là 5 sao, 10% 4 sao, 10% 3 sao cho chân thực)
    CASE ABS(CHECKSUM(NEWID())) % 10 
        WHEN 0 THEN 3 
        WHEN 1 THEN 4 
        ELSE 5 
    END AS so_sao,
    
    -- 4. Random Nội dung đánh giá
    CHOOSE(ABS(CHECKSUM(NEWID())) % 10 + 1, 
        N'Áo rất đẹp, chất vải mát, mặc rất thoải mái. Giao hàng nhanh!',
        N'Form chuẩn, đóng gói cẩn thận. Rất ưng ý với sản phẩm này.',
        N'Đúng như mô tả, màu sắc bên ngoài đẹp hơn trong ảnh. Sẽ ủng hộ shop tiếp.',
        N'Chất lượng tuyệt vời so với giá tiền. Mọi người nên mua nhé.',
        N'Hàng đẹp, shop tư vấn nhiệt tình. Mặc vừa in luôn.',
        N'Áo mặc êm, giặt máy không bị xù lông hay phai màu. Rất đáng tiền.',
        N'Giao hàng cực kỳ nhanh, shipper thân thiện. Áo bọc trong túi zip rất sang xịn mịn.',
        N'Chất vải sờ vào rất thích, form dáng basic dễ phối đồ. Vợ mình khen mặc đẹp.',
        N'Hơi rộng một xíu so với mình nhưng đổi lại mặc rất thoải mái, thoáng mát.',
        N'Beemate làm đồ bao giờ cũng chỉnh chu từ đường kim mũi chỉ. Mua lần thứ 3 rồi.'
    ) AS noi_dung,
    
    -- 5. Random Phân loại (Màu sắc & Kích cỡ)
    CHOOSE(ABS(CHECKSUM(NEWID())) % 6 + 1, 
        N'Màu: Đen | Size: M', 
        N'Màu: Trắng | Size: L', 
        N'Màu: Ghi Xám | Size: XL', 
        N'Màu: Xanh Than | Size: S',
        N'Màu: Màu Be | Size: XXL',
        N'Màu: Trắng | Size: M'
    ) AS phan_loai,
    
    -- 6. Random Hình ảnh feedback (Có người up ảnh, có người không)
    CASE ABS(CHECKSUM(NEWID())) % 4 
        WHEN 0 THEN 'https://res.cloudinary.com/gemini_fashion/image/upload/v1710600000/feedback_1.jpg'
        WHEN 1 THEN 'https://res.cloudinary.com/gemini_fashion/image/upload/v1710600000/feedback_2.jpg,https://res.cloudinary.com/gemini_fashion/image/upload/v1710600000/feedback_3.jpg'
        WHEN 2 THEN 'https://res.cloudinary.com/gemini_fashion/image/upload/v1710600000/feedback_4.jpg'
        ELSE '' -- 25% khách hàng không up ảnh
    END AS danh_sach_hinh_anh,
    
    -- 7. Random Ngày đánh giá (Trải đều trong 30 ngày gần nhất)
    DATEADD(day, - (ABS(CHECKSUM(NEWID())) % 30), GETDATE()) AS ngay_tao
    
FROM san_pham sp
-- Vòng lặp nhân bản: Tạo 10 dòng cho mỗi 1 sản phẩm
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) AS SoLuongDanhGia;
GO