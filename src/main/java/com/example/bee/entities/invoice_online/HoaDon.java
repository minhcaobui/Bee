package com.example.bee.entities.invoice_online;

import com.example.bee.entities.order.DonHang;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "hoa_don",
        uniqueConstraints = @UniqueConstraint(name = "uk_hoa_don_so_hoa_don", columnNames = "so_hoa_don")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "so_hoa_don", length = 50, nullable = false, unique = true)
    private String soHoaDon;

    // Mỗi hóa đơn gắn với một đơn hàng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_don_hang",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hd_dh")
    )
    private DonHang donHang;

    @Column(name = "tong_tien", precision = 12, scale = 2, nullable = false)
    private BigDecimal tongTien;

    @Column(name = "thue_vat", precision = 12, scale = 2)
    private BigDecimal thueVat;

    @Column(name = "file_pdf", length = 255)
    private String filePdf;

    @Column(name = "ngay_xuat", nullable = false)
    private LocalDateTime ngayXuat = LocalDateTime.now();
}
