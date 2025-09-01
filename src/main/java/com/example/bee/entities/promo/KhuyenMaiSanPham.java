package com.example.bee.entities.promo;

import com.example.bee.entities.catalog.SanPham;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "khuyen_mai_san_pham",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_kmsp",
                columnNames = {"id_khuyen_mai", "id_san_pham"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhuyenMaiSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nhiều sp có thể thuộc 1 khuyến mãi
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_khuyen_mai",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_kmsp_km")
    )
    private KhuyenMai khuyenMai;

    // 1 sản phẩm có thể tham gia nhiều khuyến mãi
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_san_pham",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_kmsp_sp")
    )
    private SanPham sanPham;
}
