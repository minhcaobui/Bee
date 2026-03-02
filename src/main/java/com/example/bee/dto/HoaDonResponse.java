package com.example.bee.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class HoaDonResponse {
    private Integer id;
    private String ma;
    private String tenKhachHang;
    private String tenNhanVien;
    private String sdtNhan;
    private String diaChiGiaoHang;
    private String trangThaiMa; // CHO_XAC_NHAN...
    private String trangThaiTen; // Chờ xác nhận
    private Integer loaiHoaDon; // 0: Tại quầy, 1: Online
    private String ngayTao;
    private String phuongThucThanhToan;

    // Tiền nong (Tách bóc rõ ràng)
    private BigDecimal tienHang; // giaTamThoi
    private BigDecimal phiVanChuyen;
    private BigDecimal tienGiamVoucher; // giaTriKhuyenMai
    private BigDecimal tienGiamSale; // Nếu anh có lưu tiền giảm Sale thì map vào, tạm thời để 0
    private BigDecimal tongTien; // giaTong

    // Danh sách sản phẩm (Cái mà JS đang khát khao)
    private List<HoaDonChiTietResponse> chiTiets;

}