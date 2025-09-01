package com.example.bee.entities.order;

import com.example.bee.entities.account.DiaChi;
import com.example.bee.entities.account.TaiKhoan;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "don_hang",
        uniqueConstraints = @UniqueConstraint(name = "uk_don_hang_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    // Tài khoản đặt hàng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_tai_khoan",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dh_kh")
    )
    private TaiKhoan taiKhoan;

    // Địa chỉ giao hàng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_dia_chi",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dh_dc")
    )
    private DiaChi diaChi;

    // Trạng thái đơn hàng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_trang_thai",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dh_tt")
    )
    private TrangThaiDonHang trangThai;

    @Column(name = "gia_tam", precision = 12, scale = 2, nullable = false)
    private BigDecimal giaTam;

    @Column(name = "gia_giam", precision = 12, scale = 2, nullable = false)
    private BigDecimal giaGiam = BigDecimal.ZERO;

    @Column(name = "phi_van_chuyen", precision = 12, scale = 2, nullable = false)
    private BigDecimal phiVanChuyen = BigDecimal.ZERO;

    @Column(name = "gia_tong", precision = 12, scale = 2, nullable = false)
    private BigDecimal giaTong;

    @Column(name = "code_ma_giam_gia", length = 50)
    private String codeMaGiamGia;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
