package com.example.bee.entities.cart;

import com.example.bee.entities.product.SanPhamChiTiet;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gio_hang_chi_tiet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GioHangChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_gio_hang", nullable = false, foreignKey = @ForeignKey(name = "fk_ghct_gh"))
    private GioHang gioHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham_chi_tiet", nullable = false, foreignKey = @ForeignKey(name = "fk_ghct_spct"))
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "ngay_them", nullable = false)
    private LocalDateTime ngayThem;

    @PrePersist
    public void prePersist() {
        if (ngayThem == null) ngayThem = LocalDateTime.now();
        if (soLuong == null) soLuong = 1;
    }
}