package com.example.bee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SanPhamRequest {

    private String ma;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 50, message = "Tên sản phẩm tối đa 50 ký tự")
    private String ten;

    private String moTa;

    @Size(max = 50, message = "URL hình ảnh tối đa 50 ký tự")
    private String hinhAnhDaiDien;

    @NotNull(message = "Danh mục không được để trống")
    private Integer idDanhMuc;

    @NotNull(message = "Hãng không được để trống")
    private Integer idHang;

    @NotNull(message = "Chất liệu không được để trống")
    private Integer idChatLieu;

    private Boolean trangThai;
}
