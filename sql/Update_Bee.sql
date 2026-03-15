-- 1. Cập nhật ảnh SKU trong san_pham_chi_tiet theo màu thực tế
UPDATE san_pham_chi_tiet
SET hinh_anh = CASE 
    WHEN id_mau_sac = 1 THEN 'https://placehold.jp/24/000000/ffffff/150x150.png?text=DEN'      -- Đen
    WHEN id_mau_sac = 2 THEN 'https://placehold.jp/24/ffffff/000000/150x150.png?text=TRANG'    -- Trắng
    WHEN id_mau_sac = 3 THEN 'https://placehold.jp/24/000080/ffffff/150x150.png?text=NAVY'     -- Xanh than
    WHEN id_mau_sac = 4 THEN 'https://placehold.jp/24/808080/ffffff/150x150.png?text=XAM'      -- Ghi xám
    WHEN id_mau_sac = 5 THEN 'https://placehold.jp/24/f5f5dc/333333/150x150.png?text=BEIGE'    -- Màu Be
    ELSE 'https://placehold.jp/24/eeeeee/333333/150x150.png?text=ANH'
END;

-- 2. Cập nhật ảnh Album của Sản phẩm (Ảnh lớn ở trên)
UPDATE hinh_anh_san_pham
SET url = 'https://placehold.jp/40/333333/ffffff/800x600.png?text=HINH+ANH+SAN+PHAM';