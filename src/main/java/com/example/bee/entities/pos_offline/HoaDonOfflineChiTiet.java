package com.example.bee.entities.pos_offline;

import com.example.bee.entities.product.SanPhamBienThe;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "hoa_don_offline_chi_tiet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoaDonOfflineChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mỗi chi tiết gắn với 1 hóa đơn offline
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_hoa_don_offline",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hdofct_hd")
    )
    private HoaDonOffline hoaDonOffline;

    // Mỗi chi tiết gắn với 1 biến thể sản phẩm
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_san_pham_bien_the",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hdofct_spbt")
    )
    private SanPhamBienThe sanPhamBienThe;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "don_gia", precision = 12, scale = 2, nullable = false)
    private BigDecimal donGia;

    @Column(name = "giam_gia", precision = 12, scale = 2, nullable = false)
    private BigDecimal giamGia = BigDecimal.ZERO;
}
