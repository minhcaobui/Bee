package com.example.bee.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoaDonResponse {
    private Integer id;
    private String ma;
    private String tenKhachHang;
    private String tenNhanVien;
    private String sdtNhan;
    private String diaChiGiaoHang;
    private String trangThaiMa;
    private String trangThaiTen;
    private Integer loaiHoaDon;
    private String ngayTao;
    private String phuongThucThanhToan;
    private BigDecimal tienHang;
    private BigDecimal phiVanChuyen;
    private BigDecimal tienGiamVoucher;
    private BigDecimal tienGiamSale;
    private BigDecimal tongTien;
    private List<HoaDonChiTietResponse> chiTiets;

}