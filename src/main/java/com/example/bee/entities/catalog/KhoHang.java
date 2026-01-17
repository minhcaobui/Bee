package com.example.bee.entities.catalog;

import com.example.bee.entities.product.SanPhamBienThe;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kho_hang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhoHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mỗi dòng kho_hang gắn với 1 biến thể sản phẩm
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_san_pham_bien_the",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_kho_spbt")
    )
    private SanPhamBienThe sanPhamBienThe;

    @Column(name = "vi_tri", length = 100)
    private String viTri;

    @Column(name = "so_luong_ton", nullable = false)
    private Integer soLuongTon = 0;
}
