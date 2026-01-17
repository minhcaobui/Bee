package com.example.bee.entities.reviews;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.product.SanPham;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "danh_gia",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_dg",
                columnNames = {"id_tai_khoan", "id_san_pham"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Người dùng đánh giá
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_tai_khoan",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dg_tk")
    )
    private TaiKhoan taiKhoan;

    // Sản phẩm được đánh giá
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_san_pham",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dg_sp")
    )
    private SanPham sanPham;

    @Column(name = "so_sao", nullable = false)
    private Integer soSao;

    @Column(name = "noi_dung", length = 1000)
    private String noiDung;

    @Column(name = "ngay", nullable = false)
    private LocalDateTime ngay = LocalDateTime.now();
}
