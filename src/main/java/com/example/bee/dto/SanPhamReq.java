package com.example.bee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SanPhamReq {
    private String ma;

    @NotBlank(message = "Tên sản phẩm không được để trống") // <--- THÊM
    private String ten;

    @NotNull(message = "Danh mục là bắt buộc") // <--- THÊM
    private Integer idDanhMuc;

    // ... (idHang, idChatLieu có thể là @NotNull nếu DB là NOT NULL)
    private Integer idHang;
    private Integer idChatLieu;

    private String moTa;
    private Boolean trangThai;
}
