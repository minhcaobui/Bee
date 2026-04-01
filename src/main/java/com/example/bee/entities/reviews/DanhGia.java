package com.example.bee.entities.reviews;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "danh_gia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tai_khoan_id")
    private Integer taiKhoanId;

    @Column(name = "san_pham_id")
    private Integer sanPhamId;

    @Column(name = "so_sao")
    private Integer soSao;

    // ĐÃ SỬA THÀNH NVARCHAR(MAX) CHO SQL SERVER
    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    @Column(name = "phan_loai")
    private String phanLoai;

    // ĐÃ SỬA THÀNH NVARCHAR(MAX) CHO SQL SERVER
    @Column(name = "danh_sach_hinh_anh", columnDefinition = "NVARCHAR(MAX)")
    private String danhSachHinhAnh;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Transient
    private String tenKhachHang;

    // Tự động cắt chuỗi link ảnh thành mảng cho Frontend dễ dùng
    @Transient
    public List<String> getDanhSachHinhAnhList() {
        if (this.danhSachHinhAnh == null || this.danhSachHinhAnh.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(this.danhSachHinhAnh.split(","));
    }

    // Trong Entity DanhGia.java
    @Column(name = "hoa_don_chi_tiet_id")
    private Integer hoaDonChiTietId;

    @Column(name = "da_sua")
    private Boolean daSua = false;
}