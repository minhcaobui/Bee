package com.example.bee.entities.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hinh_anh_san_pham")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HinhAnhSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Quan hệ N-1: nhiều hình ảnh thuộc 1 sản phẩm
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_san_pham",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_has_sp")
    )
    private SanPham sanPham;

    @Column(name = "url", length = 255, nullable = false)
    private String url;

    @Column(name = "thu_tu", nullable = false)
    private Integer thuTu = 0;
}
