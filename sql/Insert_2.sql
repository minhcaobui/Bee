INSERT INTO danh_muc (ma, ten, mo_ta, trang_thai)
VALUES 
    ('DM_TSHIRT', N'Áo Thun (T-Shirt)', N'Áo thun cotton thoáng mát, thấm hút mồ hôi tốt, đa dạng hình in.', 1),
    ('DM_POLO', N'Áo Polo', N'Áo thun có cổ, phong cách lịch sự nhưng vẫn trẻ trung, năng động.', 1),
    ('DM_SOMI', N'Áo Sơ Mi', N'Thiết kế thanh lịch, phù hợp môi trường công sở hoặc dự tiệc.', 1),
    ('DM_HOODIE', N'Áo Hoodie', N'Áo nỉ dài tay có mũ trùm đầu, phong cách Streetwear cá tính.', 1),
    ('DM_SWEATER', N'Áo Sweater', N'Áo nỉ chui đầu không mũ, ấm áp, dễ phối đồ mùa thu đông.', 1),
    ('DM_JACKET', N'Áo Khoác Gió', N'Chất liệu dù nhẹ, chống thấm nước, cản gió hiệu quả khi đi đường.', 1),
    ('DM_VEST', N'Áo Vest - Blazer', N'Phong cách sang trọng, hiện đại, tôn dáng người mặc.', 1),
    ('DM_LEN', N'Áo Len', N'Giữ ấm tốt vào mùa đông, chất liệu len mềm mại, không gây ngứa.', 1),
    ('DM_TANKTOP', N'Áo Ba Lỗ (Tank Top)', N'Thiết kế sát nách, thoải mái vận động thể thao hoặc mặc ở nhà.', 1),
    ('DM_DENIM', N'Áo Khoác Jean', N'Chất liệu Denim bền bỉ, phong cách bụi bặm và mạnh mẽ.', 1);

-- Kiểm tra kết quả
SELECT * FROM danh_muc;

INSERT INTO hang (ma, ten, mo_ta, trang_thai)
VALUES 
    ('H_NIKE', N'Nike', N'Thành lập năm 1964 tại Oregon, Mỹ bởi Phil Knight và Bill Bowerman, ban đầu tên là Blue Ribbon Sports.', 1),
    ('H_ADIDAS', N'Adidas', N'Ra đời năm 1949 tại Đức bởi Adolf Dassler sau mâu thuẫn với người anh trai (người lập ra Puma).', 1),
    ('H_UNIQLO', N'Uniqlo', N'Thương hiệu Nhật Bản ra mắt năm 1984 tại Hiroshima bởi tỷ phú Tadashi Yanai, tiền thân là Ogori Shoji.', 1),
    ('H_COOLMATE', N'Coolmate', N'Startup công nghệ Việt Nam thành lập năm 2019 bởi Phạm Chí Nhu, tiên phong mô hình D2C.', 1),
    ('H_ZARA', N'Zara', N'Thành lập năm 1975 tại Tây Ban Nha bởi Amancio Ortega, thuộc tập đoàn dệt may khổng lồ Inditex.', 1),
    ('H_LACOSTE', N'Lacoste', N'Thành lập năm 1933 tại Pháp bởi tay vợt huyền thoại René Lacoste, biểu tượng là biệt danh cá sấu của ông.', 1),
    ('H_YODY', N'Yody', N'Thương hiệu Việt khởi nguồn từ Hải Dương năm 2014 bởi Nguyễn Việt Hòa, tiền thân là nhãn hiệu Hi5.', 1),
    ('H_HM', N'H&M', N'Thành lập năm 1947 tại Västerås, Thụy Điển bởi Erling Persson, ban đầu chỉ bán đồ nữ với tên Hennes.', 1),
    ('H_PUMA', N'Puma', N'Thành lập năm 1948 tại Đức bởi Rudolf Dassler, người anh trai tách ra từ xưởng giày gia đình Dassler.', 1),
    ('H_LEVIS', N'Levi''s', N'Ra đời năm 1853 tại San Francisco, Mỹ bởi Levi Strauss trong thời kỳ Cơn sốt vàng California.', 1);

-- Kiểm tra kết quả
SELECT * FROM hang;

INSERT INTO chat_lieu (ma, ten, mo_ta, trang_thai)
VALUES 
    ('CL_COTTON', N'Vải Cotton 100%', N'Dệt từ sợi bông tự nhiên, thấm hút mồ hôi cực tốt, mềm mại và an toàn cho da nhạy cảm.', 1),
    ('CL_POLY', N'Vải Polyester', N'Sợi tổng hợp có độ bền cao, bề mặt trơn bóng, chống nhăn, chống bám bụi và giữ phom tốt.', 1),
    ('CL_KAKI', N'Vải Kaki', N'Chất vải có độ cứng vừa phải, dày dặn, ít nhăn, thường dùng may quần âu hoặc áo khoác.', 1),
    ('CL_LINEN', N'Vải Linen (Lanh)', N'Dệt từ thân cây lanh, đặc tính nhẹ, thoáng mát, bay bổng nhưng có độ nhăn tự nhiên đặc trưng.', 1),
    ('CL_DENIM', N'Vải Denim (Jean)', N'Loại vải thô dệt chéo từ sợi cotton, cực kỳ bền chắc, biểu tượng của phong cách bụi bặm.', 1);

-- Kiểm tra kết quả
SELECT * FROM chat_lieu;

INSERT INTO kich_thuoc (ma, ten)
VALUES 
    ('SIZE_S', 'S'),
    ('SIZE_M', 'M'),
    ('SIZE_L', 'L');

-- Kiểm tra kết quả
SELECT * FROM kich_thuoc;

INSERT INTO mau_sac (ma, ten)
VALUES 
    ('M_TRANG', N'Màu Trắng'),
    ('M_DEN', N'Màu Đen'),
    ('M_DO', N'Màu Đỏ'),
    ('M_XANH_DUONG', N'Màu Xanh Dương');

-- Kiểm tra kết quả
SELECT * FROM mau_sac;

BEGIN TRANSACTION;
BEGIN TRY
    DECLARE @i INT = 1;
    DECLARE @MaxRows INT = 100;

    -- Khai báo các biến để lưu thông tin tạm
    DECLARE @id_hang INT, @ten_hang NVARCHAR(50);
    DECLARE @id_dm INT, @ten_dm NVARCHAR(50), @ma_dm VARCHAR(50);
    DECLARE @id_cl INT;
    DECLARE @ten_sp NVARCHAR(200);
    DECLARE @mo_ta_sp NVARCHAR(MAX);
    DECLARE @ma_sp VARCHAR(50);
    DECLARE @random_suffix INT; -- Số ngẫu nhiên để tên không trùng

    WHILE @i <= @MaxRows
    BEGIN
        -- 1. CHỌN HÃNG NGẪU NHIÊN
        SELECT TOP 1 @id_hang = id, @ten_hang = ten FROM hang ORDER BY NEWID();

        -- 2. CHỌN DANH MỤC LOGIC THEO HÃNG (Business Logic)
        IF @ten_hang IN (N'Nike', N'Adidas', N'Puma') 
        BEGIN
            -- Hãng thể thao: Chỉ chọn Áo thun, Polo, Hoodie, Jacket, Tanktop
            SELECT TOP 1 @id_dm = id, @ten_dm = ten FROM danh_muc 
            WHERE ma IN ('DM_TSHIRT', 'DM_POLO', 'DM_HOODIE', 'DM_JACKET', 'DM_TANKTOP') 
            ORDER BY NEWID();
        END
        ELSE IF @ten_hang IN (N'Levi''s')
        BEGIN
            -- Levi's: Chuyên Denim và Áo thun
            SELECT TOP 1 @id_dm = id, @ten_dm = ten FROM danh_muc 
            WHERE ma IN ('DM_DENIM', 'DM_TSHIRT', 'DM_JACKET') 
            ORDER BY NEWID();
        END
        ELSE IF @ten_hang IN (N'Lacoste', N'Zara', N'H&M', N'Uniqlo', N'Yody', N'Coolmate')
        BEGIN
            -- Hãng thời trang Basic/Casual: Chọn cái gì cũng được TRỪ Denim (ít phổ biến hơn)
            SELECT TOP 1 @id_dm = id, @ten_dm = ten FROM danh_muc 
            WHERE ma NOT IN ('DM_DENIM') 
            ORDER BY NEWID();
        END

        -- 3. CHỌN CHẤT LIỆU LOGIC THEO DANH MỤC
        IF @ten_dm LIKE N'%Jean%' OR @ten_dm LIKE N'%Denim%'
            SELECT TOP 1 @id_cl = id FROM chat_lieu WHERE ma = 'CL_DENIM';
        ELSE IF @ten_dm LIKE N'%Gió%' OR @ten_dm LIKE N'%Polo%' OR @ten_hang IN (N'Nike', N'Adidas')
            SELECT TOP 1 @id_cl = id FROM chat_lieu WHERE ma IN ('CL_POLY', 'CL_COTTON') ORDER BY NEWID();
        ELSE IF @ten_dm LIKE N'%Len%' OR @ten_dm LIKE N'%Sweater%'
            -- Nếu là áo len thì ưu tiên Cotton hoặc chọn chất liệu khác nếu có (tạm dùng Cotton)
            SELECT TOP 1 @id_cl = id FROM chat_lieu WHERE ma = 'CL_COTTON'; 
        ELSE
            -- Còn lại ưu tiên Cotton hoặc Linen
            SELECT TOP 1 @id_cl = id FROM chat_lieu WHERE ma IN ('CL_COTTON', 'CL_LINEN', 'CL_KAKI') ORDER BY NEWID();

        -- 4. SINH TÊN SẢN PHẨM VÀ MÃ (Tạo sự đa dạng)
        SET @random_suffix = CAST(RAND() * 10000 AS INT);
        
        -- Công thức tên: [Tên Danh Mục] + [Tên Hãng] + [Tính từ ngẫu nhiên] + [Mã số]
        -- Ví dụ: Áo Polo Nike Sporty Edition 9921
        DECLARE @tinh_tu NVARCHAR(50);
        SELECT @tinh_tu = CASE CAST(RAND() * 5 AS INT)
            WHEN 0 THEN N'Cao Cấp'
            WHEN 1 THEN N'Chính Hãng'
            WHEN 2 THEN N'Phiên Bản Mới'
            WHEN 3 THEN N'Basic'
            ELSE N'Limited'
        END;

        SET @ten_sp = @ten_dm + N' ' + @ten_hang + N' ' + @tinh_tu + N' - Mẫu ' + CAST(@i AS NVARCHAR);
        
        -- Tạo mã SP: SP001, SP002...
        SET @ma_sp = 'SP' + RIGHT('0000' + CAST(@i AS VARCHAR), 4);

        -- Tạo mô tả giả
        SET @mo_ta_sp = N'Sản phẩm ' + @ten_sp + N' được thiết kế với phong cách hiện đại, phù hợp cho cả nam và nữ.';

        -- 5. INSERT VÀO BẢNG
        INSERT INTO san_pham (ma, ten, mo_ta, id_danh_muc, id_hang, id_chat_lieu, trang_thai, hinh_anh_dai_dien)
        VALUES (
            @ma_sp, 
            @ten_sp, 
            @mo_ta_sp, 
            @id_dm, 
            @id_hang, 
            @id_cl, 
            1,
            'https://example.com/images/product_' + CAST(@i AS VARCHAR) + '.jpg'
        );

        SET @i = @i + 1;
    END

    COMMIT TRANSACTION;
    PRINT N'Đã tạo thành công 100 sản phẩm với dữ liệu logic!';
END TRY
BEGIN CATCH
    ROLLBACK TRANSACTION;
    PRINT N'Lỗi: ' + ERROR_MESSAGE();
END CATCH;

-- Kiểm tra kết quả
SELECT * FROM san_pham;

INSERT INTO hinh_anh_san_pham (id_san_pham, url, thu_tu)
SELECT 
    sp.id,
    -- Tạo link ảnh giả lập (Web placehold.co sẽ tạo ảnh có chữ dựa trên tên sản phẩm)
    'https://placehold.co/600x400?text=' + REPLACE(REPLACE(sp.ten, ' ', '+'), '%', ''), 
    1 -- Ảnh chính (thứ tự 1)
FROM san_pham sp;

-- Thêm ảnh phụ (Góc nghiêng)
INSERT INTO hinh_anh_san_pham (id_san_pham, url, thu_tu)
SELECT 
    sp.id,
    'https://placehold.co/600x400/orange/white?text=Anh+Phu+Mat+Sau', 
    2
FROM san_pham sp;

-- Thêm ảnh phụ (Chi tiết vải)
INSERT INTO hinh_anh_san_pham (id_san_pham, url, thu_tu)
SELECT 
    sp.id,
    'https://placehold.co/600x400/gray/white?text=Chi+Tiet+Vai', 
    3
FROM san_pham sp;

-- Kiểm tra kết quả
SELECT * FROM hinh_anh_san_pham WHERE id_san_pham = 1;  

INSERT INTO san_pham_bien_the (id_san_pham, id_kich_thuoc, id_mau_sac, sku, gia, so_luong, trang_thai)
SELECT TOP 300
    sp.id,
    kt.id,
    ms.id,
    -- Tạo mã SKU tự động: Mã SP + Mã Màu + Tên Size (VD: SP0001-M_DO-L)
    sp.ma + '-' + ms.ma + '-' + kt.ten,
    
    -- Tạo giá ngẫu nhiên: Từ 200.000 đến 1.000.000, làm tròn chẵn nghìn
    CAST((FLOOR(RAND(CHECKSUM(NEWID())) * 800) + 200) * 1000 AS DECIMAL(12,2)),
    
    -- Tạo số lượng tồn kho ngẫu nhiên: Từ 10 đến 100 cái
    ABS(CHECKSUM(NEWID()) % 90) + 10,
    
    1 -- Trạng thái hoạt động
FROM san_pham sp
CROSS JOIN kich_thuoc kt -- Tổ hợp tất cả sản phẩm với tất cả size
CROSS JOIN mau_sac ms    -- Tổ hợp tiếp với tất cả màu
ORDER BY NEWID(); -- Sắp xếp ngẫu nhiên để lấy 300 dòng bất kỳ

-- 3. Kiểm tra kết quả
SELECT TOP 20 
    sp.ten AS ten_san_pham,
    ms.ten AS mau,
    kt.ten AS size,
    spbt.gia,
    spbt.so_luong,
    spbt.sku
FROM san_pham_bien_the spbt
JOIN san_pham sp ON spbt.id_san_pham = sp.id
JOIN mau_sac ms ON spbt.id_mau_sac = ms.id
JOIN kich_thuoc kt ON spbt.id_kich_thuoc = kt.id
ORDER BY sp.ten;

INSERT INTO nha_cung_cap (ma, ten, so_dien_thoai, email, dia_chi)
VALUES 
    ('NCC_NIKE_VN', N'Nike Vietnam Official', '02439998888', 'contact@nike.vn', N'Tòa nhà Landmark 81, TP.HCM'),
    ('NCC_XUONG_MAY_A', N'Xưởng May Anh Tuấn', '0988777666', 'tuan.maymac@gmail.com', N'KCN Ninh Hiệp, Hà Nội'),
    ('NCC_CHINA', N'Quảng Châu Trading', '0912345678', 'order@taobao-vn.com', N'Cửa khẩu Tân Thanh, Lạng Sơn');

-- Kiểm tra lại dữ liệu sau khi thêm
SELECT * FROM nha_cung_cap;

INSERT INTO khuyen_mai (ten, loai, gia_tri, ngay_bat_dau, ngay_ket_thuc, trang_thai)
VALUES 
    -- 1. ĐANG DIỄN RA (Active) - Giảm theo %
    (N'Chào mừng bạn mới 2026', 'PERCENT', 10, '2026-01-01', '2026-12-31', 1),
    (N'Siêu Sale Tháng 1', 'PERCENT', 20, '2026-01-01', '2026-01-31', 1),
    
    -- 2. ĐANG DIỄN RA (Active) - Giảm tiền mặt
    (N'Voucher giảm nóng 50k', 'AMOUNT', 50000, '2026-01-10', '2026-02-10', 1),
    (N'Hỗ trợ phí vận chuyển', 'AMOUNT', 30000, '2026-01-01', '2026-06-30', 1),
    
    -- 3. ĐÃ HẾT HẠN (Expired) - Để test validation
    (N'Flash Sale Black Friday 2025', 'PERCENT', 50, '2025-11-20', '2025-11-30', 0),
    (N'Giáng Sinh An Lành', 'AMOUNT', 100000, '2025-12-20', '2025-12-26', 0),
    
    -- 4. SẮP DIỄN RA (Future) - Test chưa đến ngày
    (N'Mừng ngày Valentine', 'PERCENT', 15, '2026-02-10', '2026-02-15', 1),
    (N'Chào Hè Rực Rỡ 2026', 'PERCENT', 30, '2026-05-01', '2026-08-01', 1),
    
    -- 5. DÀNH CHO KHÁCH VIP (Active)
    (N'Tri ân khách hàng thân thiết', 'PERCENT', 15, '2026-01-01', '2026-12-31', 1),
    (N'Giảm giá đơn hàng lớn', 'AMOUNT', 200000, '2026-01-15', '2026-02-15', 1);

-- Kiểm tra kết quả
SELECT * FROM khuyen_mai;

INSERT INTO ma_giam_gia (code, loai, gia_tri, gia_tri_toi_da, don_toi_thieu, so_luong, so_lan_su_dung, ngay_bat_dau, ngay_ket_thuc, trang_thai)
VALUES 
    -- 1. Mã giảm theo % (Giảm 10%, tối đa 50k cho đơn từ 200k) - Public
    ('SALE10', 'PERCENT', 10, 50000, 200000, 1000, 0, '2026-01-01', '2026-12-31', 1),
    
    -- 2. Mã giảm tiền mặt (Giảm thẳng 30k cho đơn từ 100k) - Public
    ('GIAM30K', 'AMOUNT', 30000, 30000, 100000, 500, 10, '2026-01-01', '2026-06-30', 1),
    
    -- 3. Mã Freeship (Giảm tối đa 30k phí ship)
    ('FREESHIP', 'AMOUNT', 30000, 30000, 0, 2000, 55, '2026-01-01', '2026-12-31', 1),
    
    -- 4. Mã KHÔNG GIỚI HẠN SỐ LƯỢNG (Để số lượng cực lớn)
    ('WELCOME', 'PERCENT', 5, 20000, 0, 999999, 120, '2026-01-01', '2026-12-31', 1),

    -- 5. Mã RIÊNG TƯ (Private) - Chỉ dành cho khách được gán (Quota thấp)
    ('VIP_ONLY', 'PERCENT', 20, 100000, 500000, 50, 0, '2026-02-01', '2026-02-28', 1);

-- Kiểm tra
SELECT * FROM ma_giam_gia;

INSERT INTO ma_giam_gia_khach_hang (id_ma_giam_gia, id_tai_khoan)
VALUES 
    -- Tặng mã VIP cho khách hàng 1001
    (
        (SELECT id FROM ma_giam_gia WHERE code = 'VIP_ONLY'),
        1001 -- Giả sử đây là ID tài khoản khách hàng
    ),
    -- Tặng mã VIP cho khách hàng 1002
    (
        (SELECT id FROM ma_giam_gia WHERE code = 'VIP_ONLY'),
        1002
    );

-- Kiểm tra xem khách hàng 1001 đang có những mã riêng nào
SELECT tk.ten_dang_nhap, mgg.code, mgg.loai, mgg.gia_tri 
FROM ma_giam_gia_khach_hang mggkh
JOIN ma_giam_gia mgg ON mggkh.id_ma_giam_gia = mgg.id
JOIN tai_khoan tk ON mggkh.id_tai_khoan = tk.id;