package com.example.bee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SanPhamResponse {
    private Integer id;
    private String ma;
    private String ten;
    private String moTa;
    private String tenDanhMuc;
    private String tenHang;
    private String tenChatLieu;
    private Integer tongSoLuong;
    private Boolean trangThai;
    private String anhDaiDien;
}
