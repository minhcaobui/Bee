package com.example.bee.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class KhachHangRequest {
    // --- 1. Info Khách Hàng ---
    private String hoTen;
    private Boolean gioiTinh; // Frontend gửi true/false (Nam/Nữ)
    private LocalDate ngaySinh;
    private String soDienThoai;
    private String email;
    private Integer idTaiKhoan;
    private Boolean trangThai;
    private String hinhAnh; //  THÊM FIELD NÀY

    // --- 2. Info Địa Chỉ Mặc Định (Để tạo luôn khi thêm mới) ---
    private String tinhThanhPho;
    private String quanHuyen;
    private String phuongXa;
    private String diaChiChiTiet;
}