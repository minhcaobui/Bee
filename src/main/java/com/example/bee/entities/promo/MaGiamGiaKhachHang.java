package com.example.bee.entities.promo;

import com.example.bee.entities.account.TaiKhoan;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "ma_giam_gia_khach_hang",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_mggkh",
                columnNames = {"id_ma_giam_gia", "id_tai_khoan"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaGiamGiaKhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Quan hệ nhiều KH có thể được cấp cùng 1 mã
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_ma_giam_gia",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_mggkh_mgg")
    )
    private MaGiamGia maGiamGia;

    // Một KH có thể có nhiều mã giảm giá khác nhau
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_tai_khoan",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_mggkh_tk")
    )
    private TaiKhoan taiKhoan;
}
