package com.example.bee.entities.wishlist;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.product.SanPham;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "yeu_thich",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_yt",
                columnNames = {"id_tai_khoan", "id_san_pham"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YeuThich {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_tai_khoan",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_yt_tk")
    )
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_san_pham",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_yt_sp")
    )
    private SanPham sanPham;

    @Column(name = "ngay", nullable = false)
    private LocalDateTime ngay = LocalDateTime.now();
}
