-- Thêm dữ liệu vào bảng vai_tro
INSERT INTO vai_tro (ma, ten)
VALUES 
    ('ADMIN', N'Quản trị viên'),
    ('STAFF', N'Nhân viên'),
    ('CUSTOMER', N'Khách hàng');

-- Kiểm tra lại dữ liệu sau khi thêm
SELECT * FROM vai_tro;

INSERT INTO tai_khoan (ten_dang_nhap, email, mat_khau, trang_thai)
VALUES 
    -- Tài khoản Admin
    ('admin_bee', 'admin@beeshop.com', HASHBYTES('SHA2_256', '123456'), 1),
    
    -- Tài khoản Staff
    ('staff_01', 'staff01@beeshop.com', HASHBYTES('SHA2_256', '123456'), 1);

-- Kiểm tra lại dữ liệu
SELECT id, ten_dang_nhap, email, mat_khau FROM tai_khoan;

INSERT INTO tai_khoan_vai_tro (id_tai_khoan, id_vai_tro)
VALUES 
    -- 1. Gán quyền ADMIN cho tài khoản 'admin_bee'
    (
        (SELECT id FROM tai_khoan WHERE ten_dang_nhap = 'admin_bee'), -- Lấy ID tài khoản
        (SELECT id FROM vai_tro WHERE ma = 'ADMIN')                   -- Lấy ID vai trò
    ),
    
    -- 2. Gán quyền STAFF cho tài khoản 'staff_01'
    (
        (SELECT id FROM tai_khoan WHERE ten_dang_nhap = 'staff_01'),
        (SELECT id FROM vai_tro WHERE ma = 'STAFF')
    );

-- Kiểm tra kết quả liên kết (JOIN 3 bảng để xem tên rõ ràng)
SELECT 
    tk.ten_dang_nhap, 
    tk.email, 
    vt.ten AS chuc_vu
FROM tai_khoan tk
JOIN tai_khoan_vai_tro tkvt ON tk.id = tkvt.id_tai_khoan
JOIN vai_tro vt ON vt.id = tkvt.id_vai_tro;

BEGIN TRANSACTION; -- Dùng Transaction để nếu lỗi thì rollback sạch sẽ

BEGIN TRY
    -- 1. Khai báo biến
    DECLARE @i INT = 1;
    DECLARE @max INT = 1000; -- Số lượng tài khoản muốn tạo
    DECLARE @roleId INT;
    DECLARE @newAccountId INT;
    DECLARE @hashedPassword VARBINARY(MAX);

    -- 2. Lấy ID của vai trò CUSTOMER và mã hóa mật khẩu trước (để tối ưu tốc độ)
    SELECT @roleId = id FROM vai_tro WHERE ma = 'CUSTOMER';
    SET @hashedPassword = HASHBYTES('SHA2_256', '123456');

    -- Kiểm tra nếu chưa có role CUSTOMER thì báo lỗi
    IF @roleId IS NULL
    BEGIN
        PRINT 'Lỗi: Không tìm thấy vai trò CUSTOMER trong bảng vai_tro';
        ROLLBACK TRANSACTION;
        RETURN;
    END

    -- 3. Bắt đầu vòng lặp
    WHILE @i <= @max
    BEGIN
        -- Tạo tên đăng nhập và email duy nhất (customer_1, customer_2...)
        DECLARE @username VARCHAR(100) = 'customer_' + CAST(@i AS VARCHAR);
        DECLARE @email VARCHAR(150) = 'customer' + CAST(@i AS VARCHAR) + '@example.com';

        -- Insert vào bảng tai_khoan
        INSERT INTO tai_khoan (ten_dang_nhap, email, mat_khau, trang_thai)
        VALUES (@username, @email, @hashedPassword, 1);

        -- Lấy ID của tài khoản vừa tạo (SCOPE_IDENTITY lấy ID vừa sinh ra trong scope này)
        SET @newAccountId = SCOPE_IDENTITY();

        -- Insert vào bảng liên kết tai_khoan_vai_tro
        INSERT INTO tai_khoan_vai_tro (id_tai_khoan, id_vai_tro)
        VALUES (@newAccountId, @roleId);

        -- Tăng biến đếm
        SET @i = @i + 1;
    END

    COMMIT TRANSACTION;
    PRINT N'Đã tạo thành công 1000 tài khoản khách hàng!';
END TRY
BEGIN CATCH
    ROLLBACK TRANSACTION;
    PRINT N'Có lỗi xảy ra: ' + ERROR_MESSAGE();
END CATCH;

INSERT INTO nhan_vien (ma, ho_ten, id_tai_khoan)
VALUES 
    -- 1. Tạo hồ sơ cho ông Admin (liên kết với tk admin_bee)
    (
        'NV001', 
        N'Nguyễn Quản Trị', 
        (SELECT id FROM tai_khoan WHERE ten_dang_nhap = 'admin_bee')
    ),
    
    -- 2. Tạo hồ sơ cho nhân viên bán hàng (liên kết với tk staff_01)
    (
        'NV002', 
        N'Trần Nhân Viên', 
        (SELECT id FROM tai_khoan WHERE ten_dang_nhap = 'staff_01')
    );

INSERT INTO khach_hang (id_tai_khoan, ho_ten, so_dien_thoai, gioi_tinh, ngay_sinh)
SELECT 
    tk.id,                                          -- Lấy ID từ bảng tai_khoan
    N'Khách hàng ' + CAST(tk.id AS NVARCHAR),       -- Tự bịa tên: Khách hàng 5, Khách hàng 6...
    '09' + RIGHT('00000000' + CAST(tk.id AS VARCHAR), 8), -- Tự bịa SĐT theo ID
    CASE WHEN tk.id % 2 = 0 THEN N'Nữ' ELSE N'Nam' END,   -- Chẵn là Nữ, Lẻ là Nam
    DATEADD(YEAR, -20, GETDATE())                   -- Mặc định 20 tuổi
FROM tai_khoan tk
JOIN tai_khoan_vai_tro tkvt ON tk.id = tkvt.id_tai_khoan
JOIN vai_tro vt ON tkvt.id_vai_tro = vt.id
WHERE vt.ma = 'CUSTOMER' -- Chỉ lấy những nick là Khách hàng
AND NOT EXISTS (SELECT 1 FROM khach_hang kh WHERE kh.id_tai_khoan = tk.id); -- Tránh thêm trùng nếu đã có