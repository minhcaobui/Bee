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

    @Column(name = "noi_dung", columnDefinition = "LONGTEXT")
    private String noiDung;

    @Column(name = "phan_loai")
    private String phanLoai;

    @Column(name = "danh_sach_hinh_anh", columnDefinition = "LONGTEXT")
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
}