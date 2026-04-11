USE [beemate];
GO

SET NOCOUNT ON;
PRINT N'Đang tạo dữ liệu giả lập... Vui lòng đợi!';

-- =========================================================================
-- PHẦN 1: TẠO 100 TÀI KHOẢN, KHÁCH HÀNG VÀ GIỎ HÀNG
-- =========================================================================
DECLARE @i INT = 1;
DECLARE @MaxKhachHang INT = 100;
DECLARE @tk_id INT;
DECLARE @ngay_sinh DATE;
DECLARE @ho_ten NVARCHAR(150);
DECLARE @sdt VARCHAR(12);
DECLARE @gioi_tinh NVARCHAR(20);
DECLARE @ho_arr NVARCHAR(50), @dem_arr NVARCHAR(50), @ten_arr NVARCHAR(50);
DECLARE @tinh_thanh NVARCHAR(100);

WHILE @i <= @MaxKhachHang
BEGIN
    -- 1. Sinh dữ liệu khách hàng ngẫu nhiên, tự nhiên nhất có thể
    SET @ho_arr = CHOOSE((@i % 7) + 1, N'Nguyễn', N'Trần', N'Lê', N'Phạm', N'Hoàng', N'Bùi', N'Đặng');
    SET @dem_arr = CHOOSE((@i % 5) + 1, N'Văn', N'Thị', N'Minh', N'Ngọc', N'Xuân');
    SET @ten_arr = CHOOSE((@i % 8) + 1, N'An', N'Bình', N'Cường', N'Dung', N'Hải', N'Linh', N'Sơn', N'Trang');
    SET @ho_ten = @ho_arr + ' ' + @dem_arr + ' ' + @ten_arr;
    
    SET @sdt = '09' + CAST((10000000 + ABS(CHECKSUM(NEWID())) % 89999999) AS VARCHAR(8));
    SET @gioi_tinh = CHOOSE((@i % 2) + 1, N'Nam', N'Nữ');
    SET @ngay_sinh = DATEADD(DAY, - (ROUND(RAND() * 5000, 0) + 6000), GETDATE()); -- Tuổi từ 16 - 30
    SET @tinh_thanh = CHOOSE((@i % 5) + 1, N'Hà Nội', N'Hồ Chí Minh', N'Đà Nẵng', N'Hải Phòng', N'Cần Thơ');

    -- 2. Insert Tài Khoản (Vai trò khách hàng = 3)
    INSERT INTO [dbo].[tai_khoan] ([ten_dang_nhap], [mat_khau], [trang_thai], [id_vai_tro], [da_doi_ten_dang_nhap])
    VALUES ('khachhang' + CAST(@i AS VARCHAR(10)), '$2a$10$dummyHashPass1234567890', 1, 3, 0);
    
    SET @tk_id = SCOPE_IDENTITY(); -- Lấy ID tài khoản vừa tạo

    -- 3. Insert Khách Hàng (Gắn với tài khoản vừa tạo)
    INSERT INTO [dbo].[khach_hang] ([ma], [ho_ten], [gioi_tinh], [ngay_sinh], [dia_chi], [so_dien_thoai], [email], [trang_thai], [id_tai_khoan])
    VALUES ('KH2026' + RIGHT('000' + CAST(@i AS VARCHAR(10)), 3), @ho_ten, @gioi_tinh, @ngay_sinh, @tinh_thanh, @sdt, 'khachhang' + CAST(@i AS VARCHAR(10)) + '@gmail.com', 1, @tk_id);

    -- 4. Insert Giỏ Hàng (Gắn với tài khoản)
    INSERT INTO [dbo].[gio_hang] ([id_tai_khoan], [cap_nhat_cuoi])
    VALUES (@tk_id, GETDATE());

    SET @i = @i + 1;
END
PRINT N'Đã tạo thành công 100 Tài khoản, Khách hàng và Giỏ hàng!';


-- =========================================================================
-- PHẦN 2: TẠO 100 HÓA ĐƠN THỰC TẾ (Kèm Chi Tiết, Lịch Sử, Thanh Toán)
-- Giao dịch được thực hiện bởi 2 nhân viên (ID: 1 hoặc 2)
-- =========================================================================
DECLARE @j INT = 1;
DECLARE @MaxHoaDon INT = 100;
DECLARE @hd_id INT;
DECLARE @nv_id INT;
DECLARE @kh_id INT;
DECLARE @trang_thai_hd INT;
DECLARE @spct_id INT;
DECLARE @gia_tien DECIMAL(18,2);
DECLARE @ngay_tao DATETIME;

-- Đưa danh sách Khách Hàng và Sản Phẩm Chi Tiết vào bảng tạm để thao tác ngẫu nhiên
IF OBJECT_ID('tempdb..#TempKhachHang') IS NOT NULL DROP TABLE #TempKhachHang;
SELECT id, ho_ten, so_dien_thoai, dia_chi INTO #TempKhachHang FROM khach_hang;

IF OBJECT_ID('tempdb..#TempSPCT') IS NOT NULL DROP TABLE #TempSPCT;
SELECT id, gia_ban INTO #TempSPCT FROM san_pham_chi_tiet WHERE trang_thai = 1;

WHILE @j <= @MaxHoaDon
BEGIN
    -- 1. Random dữ liệu nền cho Hóa đơn
    SET @nv_id = CHOOSE((@j % 2) + 1, 1, 2); -- Random NV001 hoặc NV002
    SELECT TOP 1 @kh_id = id FROM #TempKhachHang ORDER BY NEWID(); -- Random Khách Hàng
    
    DECLARE @ten_nguoi_nhan NVARCHAR(150), @sdt_nhan VARCHAR(15), @dia_chi_giao NVARCHAR(MAX);
    SELECT @ten_nguoi_nhan = ho_ten, @sdt_nhan = so_dien_thoai, @dia_chi_giao = dia_chi FROM #TempKhachHang WHERE id = @kh_id;

    -- Đơn hàng diễn ra ngẫu nhiên trong 60 ngày gần nhất
    SET @ngay_tao = DATEADD(DAY, -ROUND(RAND() * 60, 0), GETDATE());
    
    -- Đa số đơn là Hoàn thành (6), một số Đang giao (5), Đã xác nhận (3)
    SET @trang_thai_hd = CHOOSE((@j % 5) + 1, 3, 6, 6, 6, 5); 

    -- 2. Khởi tạo Hóa Đơn (Giá trị tiền ban đầu là 0, sẽ update sau khi có chi tiết)
    INSERT INTO [dbo].[hoa_don] ([ma], [gia_tam_thoi], [phi_van_chuyen], [gia_tri_khuyen_mai], [gia_tong], 
                                 [ten_nguoi_nhan], [sdt_nhan], [dia_chi_giao_hang], [ghi_chu], [loai_hoa_don], 
                                 [ngay_tao], [id_nhan_vien], [id_khach_hang], [id_trang_thai_hoa_don])
    VALUES ('HD2026' + RIGHT('0000' + CAST(@j AS VARCHAR), 4), 0, 0, 0, 0,
            @ten_nguoi_nhan, @sdt_nhan, @dia_chi_giao, N'Đơn hàng tự động sinh', 1, 
            @ngay_tao, @nv_id, @kh_id, @trang_thai_hd);
            
    SET @hd_id = SCOPE_IDENTITY();
    
    -- 3. Tạo ngẫu nhiên 1 - 3 Sản phẩm chi tiết cho mỗi hóa đơn
    DECLARE @k INT = 1;
    DECLARE @MaxSP INT = (ROUND(RAND() * 2, 0) + 1); 
    DECLARE @tong_tien DECIMAL(18,2) = 0;
    
    WHILE @k <= @MaxSP
    BEGIN
        SELECT TOP 1 @spct_id = id, @gia_tien = gia_ban FROM #TempSPCT ORDER BY NEWID();
        DECLARE @sl INT = (ROUND(RAND() * 2, 0) + 1); -- Random mua 1-3 cái mỗi loại
        
        INSERT INTO [dbo].[hoa_don_chi_tiet] ([gia_tien], [so_luong], [id_hoa_don], [id_san_pham_chi_tiet], [so_luong_tra])
        VALUES (@gia_tien, @sl, @hd_id, @spct_id, 0);
        
        SET @tong_tien = @tong_tien + (@gia_tien * @sl);
        SET @k = @k + 1;
    END

    -- 4. Tính toán phí Ship và Cập nhật lại tổng tiền Hóa Đơn
    DECLARE @phi_ship DECIMAL(18,2) = CASE WHEN @tong_tien < 500000 THEN 30000 ELSE 0 END;
    DECLARE @ngay_thanh_toan DATETIME = DATEADD(MINUTE, 30, @ngay_tao);

    UPDATE [dbo].[hoa_don] 
    SET gia_tam_thoi = @tong_tien, 
        phi_van_chuyen = @phi_ship, 
        gia_tong = @tong_tien + @phi_ship,
        ngay_thanh_toan = @ngay_thanh_toan
    WHERE id = @hd_id;

    -- 5. Ghi nhận Lịch Sử Hóa Đơn (Logic theo dòng thời gian thực tế)
    -- Trạng thái 1: Chờ thanh toán
    INSERT INTO [dbo].[lich_su_hoa_don] ([id_hoa_don], [id_trang_thai_hoa_don], [id_nhan_vien], [ghi_chu], [ngay_tao])
    VALUES (@hd_id, 1, @nv_id, N'Khách hàng vừa đặt đơn', @ngay_tao);
    
    -- Trạng thái 3: Đã xác nhận (10 phút sau)
    INSERT INTO [dbo].[lich_su_hoa_don] ([id_hoa_don], [id_trang_thai_hoa_don], [id_nhan_vien], [ghi_chu], [ngay_tao])
    VALUES (@hd_id, 3, @nv_id, N'Nhân viên đã gọi điện xác nhận', DATEADD(MINUTE, 10, @ngay_tao));
    
    -- Nếu đơn hàng Đang giao hoặc Hoàn thành, phải qua bước 5 (Đang giao)
    IF @trang_thai_hd >= 5
    BEGIN
        INSERT INTO [dbo].[lich_su_hoa_don] ([id_hoa_don], [id_trang_thai_hoa_don], [id_nhan_vien], [ghi_chu], [ngay_tao])
        VALUES (@hd_id, 5, @nv_id, N'Giao cho đơn vị vận chuyển', DATEADD(HOUR, 5, @ngay_tao));
    END

    -- Nếu đơn hàng là Hoàn thành, ghi nhận bước 6
    IF @trang_thai_hd = 6
    BEGIN
        INSERT INTO [dbo].[lich_su_hoa_don] ([id_hoa_don], [id_trang_thai_hoa_don], [id_nhan_vien], [ghi_chu], [ngay_tao])
        VALUES (@hd_id, 6, @nv_id, N'Đơn hàng giao thành công', DATEADD(HOUR, 48, @ngay_tao));
    END

    -- 6. Ghi nhận thanh toán
    INSERT INTO [dbo].[thanh_toan] ([id_hoa_don], [so_tien], [phuong_thuc], [loai_thanh_toan], [trang_thai], [ma_giao_dich], [ngay_thanh_toan], [id_nhan_vien])
    VALUES (@hd_id, @tong_tien + @phi_ship, N'TIEN_MAT', 'THANH_TOAN', 'THANH_CONG', 'GD' + CAST(@hd_id AS VARCHAR), @ngay_thanh_toan, @nv_id);

    SET @j = @j + 1;
END

-- Dọn dẹp bảng tạm
DROP TABLE #TempKhachHang;
DROP TABLE #TempSPCT;
PRINT N'Đã hoàn thành tạo 100 luồng Giao dịch logic!';
GO