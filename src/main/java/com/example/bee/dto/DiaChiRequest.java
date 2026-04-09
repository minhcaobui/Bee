package com.example.bee.dto;

import lombok.Data;

@Data
public class DiaChiRequest {
    private String hoTenNhan;
    private String sdtNhan;
    private String diaChiChiTiet;
    private String tinhThanhPho;
    private Integer maTinh;
    private String quanHuyen;
    private Integer maHuyen;
    private String phuongXa;
    private String maXa;
    private Boolean laMacDinh;
}