package com.example.bee.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class HoaDonChiTietResponse {
    private Integer id;
    private String tenSanPham;
    private String sku;
    private String thuocTinh; // Gộp chung "Màu sắc - Kích thước" cho gọn
    private String hinhAnh;
    private Integer soLuong;
    private BigDecimal donGia; // Giá gốc
    private BigDecimal giaBan; // Giá sau giảm
}
