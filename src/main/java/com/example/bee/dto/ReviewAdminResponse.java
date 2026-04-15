package com.example.bee.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewAdminResponse {
    private Long id;
    private String tenKhachHang;
    private Integer sanPhamId;
    private String tenSanPham;
    private Integer soSao;
    private String noiDung;
    private String phanLoai;
    private String danhSachHinhAnh;
    private String ngayTao;
    private String tenNhanVienTraLoi;
    private String noiDungTraLoi;
    private String ngayTraLoi;
}