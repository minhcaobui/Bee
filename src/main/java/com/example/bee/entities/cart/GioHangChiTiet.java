package com.example.bee.entities.cart;

import com.example.bee.entities.product.SanPhamBienThe;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "gio_hang_chi_tiet",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_ghct",
                columnNames = {"id_gio_hang", "id_san_pham_bien_the"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GioHangChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Quan hệ nhiều dòng chi tiết thuộc 1 giỏ hàng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_gio_hang",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ghct_gh")
    )
    private GioHang gioHang;

    // Mỗi dòng chi tiết gắn với 1 biến thể sản phẩm
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_san_pham_bien_the",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ghct_spbt")
    )
    private SanPhamBienThe sanPhamBienThe;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;
}
