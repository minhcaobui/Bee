package com.example.bee.entities.promotion;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "khuyen_mai")
public class KhuyenMai {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = false, unique = true)
    private String ma;

    @Column(length = 150, nullable = false)
    private String ten;

    @Column(length = 20, nullable = false)
    private String loai; // PERCENT hoặc AMOUNT

    @Column(name = "gia_tri", nullable = false)
    private BigDecimal giaTri;

    // --- CÁC CỘT MỚI THÊM ---
    @Column(name = "giam_toi_da")
    private BigDecimal giamToiDa; // Dùng cho loại PERCENT

    @Column(name = "dieu_kien_toi_thieu")
    private BigDecimal dieuKienToiThieu = BigDecimal.ZERO;

    @Column(name = "da_su_dung")
    private Integer daSuDung = 0;
    // ------------------------

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "hinh_thuc", nullable = false)
    private Boolean hinhThuc; // false: Hóa đơn, true: Sản phẩm

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "cho_phep_cong_don")
    private Boolean choPhepCongDon = false;

    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}