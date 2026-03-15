package com.example.bee.entities.promotion;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ma_giam_gia")
public class MaGiamGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_code", nullable = false, length = 100, unique = true)
    private String maCode;

    @Column(name = "ten", nullable = false, length = 100)
    private String ten;

    @Column(name = "loai_giam_gia", nullable = false)
    private String loaiGiamGia;

    @Column(name = "gia_tri_giam_gia", nullable = false)
    private BigDecimal giaTriGiamGia;

    @Column(name = "gia_tri_giam_gia_toi_da")
    private BigDecimal giaTriGiamGiaToiDa;

    @Column(name = "dieu_kien", nullable = false)
    private BigDecimal dieuKien;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "luot_su_dung", nullable = false)
    private Integer luotSuDung = 0;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "cho_phep_cong_don")
    private Boolean choPhepCongDon = false;

    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}