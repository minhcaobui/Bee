package com.example.bee.entities.order;

import com.example.bee.entities.product.SanPhamChiTiet;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "don_hang_chi_tiet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonHangChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mỗi chi tiết thuộc về 1 đơn hàng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_don_hang",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dhct_dh")
    )
    private DonHang donHang;

    // Chi tiết gắn với 1 biến thể sản phẩm
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_san_pham_bien_the",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dhct_spbt")
    )
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "don_gia", precision = 12, scale = 2, nullable = false)
    private BigDecimal donGia;

    @Column(name = "giam_gia", precision = 12, scale = 2, nullable = false)
    private BigDecimal giamGia = BigDecimal.ZERO;
}