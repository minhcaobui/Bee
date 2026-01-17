package com.example.bee.entities.exchanges;

import com.example.bee.entities.product.SanPhamBienThe;
import com.example.bee.entities.order.DonHangChiTiet;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "yeu_cau_doi_tra_ct",
        indexes = {
                @Index(name = "ix_ycdtct_yc", columnList = "id_yeu_cau"),
                @Index(name = "ix_ycdtct_dhct", columnList = "id_don_hang_chi_tiet")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YeuCauDoiTraChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Gắn với yêu cầu đổi/trả
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_yeu_cau",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ycdtct_yc")
    )
    private YeuCauDoiTra yeuCau;

    // Gắn với dòng chi tiết trong đơn hàng gốc
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_don_hang_chi_tiet",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ycdtct_dhct")
    )
    private DonHangChiTiet donHangChiTiet;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    // Biến thể mới nếu khách đổi sang sản phẩm khác
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_bien_the_doi_moi",
            foreignKey = @ForeignKey(name = "fk_ycdtct_bt")
    )
    private SanPhamBienThe bienTheDoiMoi;

    // Chênh lệch giá: + khách bù thêm, - hoàn lại khách
    @Column(name = "chenh_lech", precision = 12, scale = 2)
    private BigDecimal chenhLech;
}
