package com.example.bee.entities.catalog;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "san_pham_bien_the",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_spbt",
                columnNames = {"id_san_pham", "id_kich_thuoc", "id_mau_sac"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPhamBienThe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mối quan hệ nhiều biến thể thuộc 1 sản phẩm
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_san_pham",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_spbt_sp")
    )
    private SanPham sanPham;

    @Column(name = "sku", length = 100, nullable = false, unique = true)
    private String sku;

    @Column(name = "gia", precision = 12, scale = 2, nullable = false)
    private BigDecimal gia;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong = 0;

    // Kích thước
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_kich_thuoc",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_spbt_size")
    )
    private KichThuoc kichThuoc;

    // Màu sắc
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_mau_sac",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_spbt_color")
    )
    private MauSac mauSac;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}