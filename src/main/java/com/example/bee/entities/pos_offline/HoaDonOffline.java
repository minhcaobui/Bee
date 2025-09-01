package com.example.bee.entities.pos_offline;

import com.example.bee.entities.account.TaiKhoan;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "hoa_don_offline",
        uniqueConstraints = @UniqueConstraint(name = "uk_hdof_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoaDonOffline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    // Cửa hàng xuất hóa đơn
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_cua_hang",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hdof_ch")
    )
    private CuaHang cuaHang;

    // Ca bán hàng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_ca_ban_hang",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hdof_cabh")
    )
    private CaBanHang caBanHang;

    // Nhân viên lập hóa đơn
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_nhan_vien",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hdof_nv")
    )
    private NhanVien nhanVien;

    // Khách hàng (tài khoản) nếu có thành viên
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_tai_khoan_khach",
            foreignKey = @ForeignKey(name = "fk_hdof_tk")
    )
    private TaiKhoan taiKhoanKhach;

    @Column(name = "tong_tien", precision = 12, scale = 2, nullable = false)
    private BigDecimal tongTien;

    @Column(name = "thue_vat", precision = 12, scale = 2)
    private BigDecimal thueVat;

    @Column(name = "giam_gia", precision = 12, scale = 2, nullable = false)
    private BigDecimal giamGia = BigDecimal.ZERO;

    // = tong_tien + thue_vat - giam_gia
    @Column(name = "thanh_tien", precision = 12, scale = 2, nullable = false)
    private BigDecimal thanhTien;

    // CASH | CARD | QR | MIXED
    @Column(name = "hinh_thuc", length = 20, nullable = false)
    private String hinhThuc;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
