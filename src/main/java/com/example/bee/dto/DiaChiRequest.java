package com.example.bee.dto;

import lombok.Data;

@Data
public class DiaChiRequest {
    private String hoTenNhan;
    private String sdtNhan;
    private String diaChiChiTiet;
    private String tinhThanhPho;
    private Integer maTinh; // Thêm mã tỉnh của GHN
    private String quanHuyen;
    private Integer maHuyen; // Thêm mã huyện của GHN
    private String phuongXa;
    private String maXa;     // Thêm mã xã của GHN
    private Boolean laMacDinh;
}