package com.example.bee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SanPhamRequest {
    @NotBlank(message = "Tên không được để trống")
    private String ten;
    private String ma;
    private String moTa;
    private Integer idDanhMuc;
    private Integer idHang;
    private Integer idChatLieu;
    private Boolean trangThai;
    private List<String> danhSachHinhAnh;
}
