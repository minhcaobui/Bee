package com.example.bee.entities.exchanges;

import com.example.bee.entities.order.DonHang;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "yeu_cau_doi_tra",
        uniqueConstraints = @UniqueConstraint(name = "uk_ycdt_so_yc", columnNames = "so_yc"),
        indexes = {
                @Index(name = "ix_ycdt_dh", columnList = "id_don_hang")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YeuCauDoiTra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "so_yc", length = 50, nullable = false, unique = true)
    private String soYc; // ví dụ: RT2025-00001

    // Liên kết đơn hàng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_don_hang",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ycdt_dh")
    )
    private DonHang donHang;

    // Loại yêu cầu: DOI / TRA
    @Column(name = "loai", length = 10, nullable = false)
    private String loai;

    // Trạng thái yêu cầu
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_trang_thai",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ycdt_tt")
    )
    private YcdtTrangThai trangThai;

    @Column(name = "ly_do", length = 255)
    private String lyDo;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;

    @Column(name = "phi_xu_ly", precision = 12, scale = 2, nullable = false)
    private BigDecimal phiXuLy = BigDecimal.ZERO;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime ngayCapNhat = LocalDateTime.now();
}
