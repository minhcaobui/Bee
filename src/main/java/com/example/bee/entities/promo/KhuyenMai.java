package com.example.bee.entities.promo;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "khuyen_mai")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhuyenMai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten", length = 150, nullable = false)
    private String ten;

    @Column(name = "loai", length = 20, nullable = false)
    private String loai; // PERCENT / AMOUNT

    @Column(name = "gia_tri", precision = 12, scale = 2, nullable = false)
    private BigDecimal giaTri;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}