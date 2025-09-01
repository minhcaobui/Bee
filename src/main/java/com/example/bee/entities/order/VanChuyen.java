package com.example.bee.entities.order;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "van_chuyen")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VanChuyen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mỗi vận chuyển gắn với một đơn hàng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_don_hang",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_vc_dh")
    )
    private DonHang donHang;

    @Column(name = "dvvc", length = 50, nullable = false)
    private String dvvc; // Đơn vị vận chuyển

    @Column(name = "ma_van_don", length = 100)
    private String maVanDon;

    @Column(name = "trang_thai", length = 50)
    private String trangThai; // CREATED / PICKED / TRANSIT / DELIVERED / FAILED

    @Column(name = "phi_van_chuyen", precision = 12, scale = 2)
    private BigDecimal phiVanChuyen;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
