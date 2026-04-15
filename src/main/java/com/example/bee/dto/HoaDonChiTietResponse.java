package com.example.bee.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class HoaDonChiTietResponse {
    private Integer id;
    private String tenSanPham;
    private String sku;
    private String thuocTinh;
    private String hinhAnh;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal giaBan;
    private Integer idSanPhamChiTiet;
    private Integer idSanPham;
}
