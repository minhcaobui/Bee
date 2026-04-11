package com.example.bee.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class KhachHangRequest {
    private String hoTen;
    private Boolean gioiTinh;
    private LocalDate ngaySinh;
    private String soDienThoai;
    private String email;
    private Integer idTaiKhoan;
    private Boolean trangThai;
    private String hinhAnh;
    private String tinhThanhPho;
    private String quanHuyen;
    private String phuongXa;
    private Integer maTinh;
    private Integer maHuyen;
    private String maXa;

    private String diaChiChiTiet;
}