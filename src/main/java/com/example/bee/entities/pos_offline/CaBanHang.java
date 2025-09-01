package com.example.bee.entities.pos_offline;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ca_ban_hang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaBanHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Cửa hàng mở ca
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_cua_hang",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cbh_ch")
    )
    private CuaHang cuaHang;

    // Nhân viên mở ca
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_nhan_vien_mo",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cbh_nvmo")
    )
    private NhanVien nhanVienMo;

    @Column(name = "thoi_gian_mo", nullable = false)
    private LocalDateTime thoiGianMo = LocalDateTime.now();

    // Nhân viên đóng ca
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_nhan_vien_dong",
            foreignKey = @ForeignKey(name = "fk_cbh_nvdong")
    )
    private NhanVien nhanVienDong;

    @Column(name = "thoi_gian_dong")
    private LocalDateTime thoiGianDong;

    @Column(name = "tien_mat_ban_dau", precision = 12, scale = 2, nullable = false)
    private BigDecimal tienMatBanDau = BigDecimal.ZERO;

    @Column(name = "tien_mat_thuc_te_cuoi_ca", precision = 12, scale = 2)
    private BigDecimal tienMatThucTeCuoiCa;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}