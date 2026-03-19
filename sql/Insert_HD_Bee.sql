USE beemate;
GO

-- Xóa dữ liệu cũ nếu muốn reset (bỏ comment để dùng)
-- DELETE FROM thanh_toan;
-- DELETE FROM lich_su_hoa_don;
-- DELETE FROM hoa_don_chi_tiet;
-- DELETE FROM hoa_don;

SET NOCOUNT ON;

DECLARE @counter INT = 1;
DECLARE @trang_thai_id INT;
DECLARE @rand_pct INT;
DECLARE @khach_hang_id INT;
DECLARE @nhan_vien_id INT;
DECLARE @loai_hd INT;
DECLARE @gia_tam DECIMAL(18,2);
DECLARE @phi_ship DECIMAL(18,2);
DECLARE @tong_tien DECIMAL(18,2);
DECLARE @ngay_tao DATETIME;
DECLARE @ngay_thanh_toan DATETIME;
DECLARE @ma_hd VARCHAR(50);
DECLARE @phuong_thuc NVARCHAR(50);
DECLARE @id_hd_new INT;
DECLARE @spct_id INT;

-- Giả định có 40 sản phẩm chi tiết đã tồn tại để lấy làm hóa đơn chi tiết
DECLARE @max_spct INT = (SELECT ISNULL(MAX(id), 40) FROM san_pham_chi_tiet);
IF @max_spct = 0 SET @max_spct = 40;

WHILE @counter <= 1000
BEGIN
    -- Sinh số ngẫu nhiên từ 1 đến 100 để chia tỷ lệ
    SET @rand_pct = ABS(CHECKSUM(NEWID())) % 100 + 1;
    
    -- Chia tỷ lệ 95% trạng thái Hoàn thành. ID tương ứng trong bảng trang_thai_hoa_don
    -- ID = 1 (Chờ thanh toán), 2 (Hoàn thành), 3 (Đã hủy), 4 (Chờ xác nhận), 5 (Chờ giao), 6 (Đang giao)
    IF @rand_pct <= 95 
        SET @trang_thai_id = 2; -- 95% Hoàn thành
    ELSE IF @rand_pct = 96 
        SET @trang_thai_id = 1; -- 1% Chờ thanh toán
    ELSE IF @rand_pct = 97 
        SET @trang_thai_id = 3; -- 1% Đã hủy
    ELSE IF @rand_pct = 98 
        SET @trang_thai_id = 4; -- 1% Chờ xác nhận
    ELSE IF @rand_pct = 99 
        SET @trang_thai_id = 5; -- 1% Chờ giao hàng
    ELSE 
        SET @trang_thai_id = 6; -- 1% Đang giao hàng

    -- Lấy ngẫu nhiên KH (giả sử có 100 KH) và NV (giả sử có 2 NV)
    SET @khach_hang_id = ABS(CHECKSUM(NEWID())) % 100 + 1; 
    SET @nhan_vien_id = ABS(CHECKSUM(NEWID())) % 2 + 1;    
    
    -- 0: Tại quầy, 1: Online
    SET @loai_hd = ABS(CHECKSUM(NEWID())) % 2;             
    
    -- Xử lý thông tin ship và phương thức thanh toán dựa theo loại hóa đơn
    IF @loai_hd = 0 
    BEGIN
        SET @phi_ship = 0;
        SET @phuong_thuc = N'TIEN_MAT';
    END
    ELSE 
    BEGIN
        SET @phi_ship = 30000;
        IF ABS(CHECKSUM(NEWID())) % 2 = 0 
            SET @phuong_thuc = N'COD';
        ELSE 
            SET @phuong_thuc = N'CHUYEN_KHOAN';
    END
    
    -- Random giá trị đơn hàng (từ 100.000đ đến 2.000.000đ)
    SET @gia_tam = (ABS(CHECKSUM(NEWID())) % 20 + 1) * 100000;
    SET @tong_tien = @gia_tam + @phi_ship;
    
    -- Random ngày tạo rải rác đều trong 3 tháng qua (90 ngày)
    -- 90 ngày * 24 giờ * 60 phút = 129600 phút
    SET @ngay_tao = DATEADD(MINUTE, -(ABS(CHECKSUM(NEWID())) % 129600), GETDATE());
    
    -- Nếu hoàn thành thì có ngày thanh toán (sau lúc tạo khoảng ngẫu nhiên từ 1 phút đến 24h = 1440 phút)
    IF @trang_thai_id = 2 
        SET @ngay_thanh_toan = DATEADD(MINUTE, ABS(CHECKSUM(NEWID())) % 1440, @ngay_tao);
    ELSE
        SET @ngay_thanh_toan = NULL;

    -- Format Mã hóa đơn (Ví dụ: HD00123)
    SET @ma_hd = 'HD' + RIGHT('00000' + CAST(@counter AS VARCHAR), 5);

    -- 1. Chèn dữ liệu vào bảng hoa_don
    INSERT INTO hoa_don (
        ma, gia_tam_thoi, phi_van_chuyen, gia_tri_khuyen_mai, gia_tong, 
        ten_nguoi_nhan, sdt_nhan, dia_chi_giao_hang, phuong_thuc_thanh_toan, 
        ghi_chu, id_nhan_vien, id_khach_hang, id_ma_giam_gia, 
        id_trang_thai_hoa_don, loai_hoa_don, ngay_tao, ngay_thanh_toan
    )
    VALUES (
        @ma_hd, @gia_tam, @phi_ship, 0, @tong_tien,
        N'Khách hàng ' + CAST(@khach_hang_id AS NVARCHAR), 
        '090' + RIGHT('0000000' + CAST(ABS(CHECKSUM(NEWID())) % 10000000 AS VARCHAR), 7), -- SĐT ngẫu nhiên
        CASE WHEN @loai_hd = 1 THEN N'Địa chỉ giao hàng ngẫu nhiên số ' + CAST((ABS(CHECKSUM(NEWID())) % 500) AS NVARCHAR) ELSE NULL END,
        @phuong_thuc,
        N'Hóa đơn sinh tự động',
        @nhan_vien_id, @khach_hang_id, NULL,
        @trang_thai_id, @loai_hd, @ngay_tao, @ngay_thanh_toan
    );
    
    -- Lấy ID hóa đơn vừa được sinh ra
    SET @id_hd_new = SCOPE_IDENTITY();

    -- 2. Chèn dữ liệu vào bảng hoa_don_chi_tiet (1 sản phẩm mỗi HĐ)
    SET @spct_id = ABS(CHECKSUM(NEWID())) % @max_spct + 1;
    INSERT INTO hoa_don_chi_tiet (gia_tien, so_luong, id_hoa_don, id_san_pham_chi_tiet)
    VALUES (@gia_tam, 1, @id_hd_new, @spct_id);

    -- 3. Chèn lịch sử thay đổi trạng thái hóa đơn
    INSERT INTO lich_su_hoa_don (id_hoa_don, id_trang_thai_hoa_don, id_nhan_vien, ghi_chu, ngay_tao)
    VALUES (@id_hd_new, @trang_thai_id, @nhan_vien_id, N'Chuyển trạng thái hóa đơn sang ' + CAST(@trang_thai_id AS NVARCHAR), @ngay_tao);

    -- 4. Chèn thông tin vào bảng thanh_toan (Chỉ lưu thanh toán khi hóa đơn hoàn thành)
    IF @trang_thai_id = 2
    BEGIN
        INSERT INTO thanh_toan (
            id_hoa_don, so_tien, phuong_thuc, loai_thanh_toan, 
            trang_thai, ma_giao_dich, ngay_thanh_toan, ghi_chu, id_nhan_vien
        )
        VALUES (
            @id_hd_new, @tong_tien, @phuong_thuc, N'THANH_TOAN', 
            N'THANH_CONG', 'VNPTX' + CAST((ABS(CHECKSUM(NEWID())) % 999999) AS VARCHAR), @ngay_thanh_toan, N'Thanh toán hoàn tất', @nhan_vien_id
        );
    END

    SET @counter = @counter + 1;
END

PRINT N'Đã thêm thành công 1000 Hóa Đơn (chia đều 3 tháng) với 95% trạng thái HOÀN THÀNH!'
GO