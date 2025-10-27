package com.example.bee.entities.catalog;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "san_pham",
        uniqueConstraints = @UniqueConstraint(name = "uk_san_pham_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    @Column(name = "ten", length = 200, nullable = false)
    private String ten;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "hinh_anh_dai_dien", length = 255)
    private String hinhAnhDaiDien;

    // Many-to-one: sản phẩm thuộc 1 danh mục
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_danh_muc", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sp_dm"))
    private DanhMuc danhMuc;

    // Many-to-one: sản phẩm thuộc 1 hãng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hang", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sp_hang"))
    private Hang hang;

    // Many-to-one: sản phẩm có 1 chất liệu
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_chat_lieu", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sp_cl"))
    private ChatLieu chatLieu;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
