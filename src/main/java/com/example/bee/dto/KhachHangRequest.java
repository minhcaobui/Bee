package com.example.bee.dto;

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
    private String diaChiChiTiet;
}