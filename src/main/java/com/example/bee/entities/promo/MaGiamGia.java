package com.example.bee.entities.promo;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ma_giam_gia",
        uniqueConstraints = @UniqueConstraint(name = "uk_ma_giam_gia_code", columnNames = "code")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "loai", length = 20, nullable = false)
    private String loai; // PERCENT / AMOUNT

    @Column(name = "gia_tri", precision = 12, scale = 2, nullable = false)
    private BigDecimal giaTri;

    @Column(name = "gia_tri_toi_da", precision = 12, scale = 2)
    private BigDecimal giaTriToiDa;

    @Column(name = "don_toi_thieu", precision = 12, scale = 2)
    private BigDecimal donToiThieu;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong = 0;

    @Column(name = "so_lan_su_dung", nullable = false)
    private Integer soLanSuDung = 0;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}
