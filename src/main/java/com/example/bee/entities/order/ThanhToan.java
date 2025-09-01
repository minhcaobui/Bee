package com.example.bee.entities.order;

import com.example.bee.entities.exchanges.YeuCauDoiTra;
import com.example.bee.entities.pos_offline.HoaDonOffline;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "thanh_toan",
        indexes = {
                @Index(name = "ix_tt_dh", columnList = "id_don_hang"),
                @Index(name = "ix_tt_hdof", columnList = "id_hoa_don_offline"),
                @Index(name = "ix_tt_ycdt", columnList = "id_yeu_cau_doi_tra")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // --- 1) Thanh toán cho ĐƠN HÀNG online ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_don_hang", foreignKey = @ForeignKey(name = "fk_tt_dh"))
    private DonHang donHang;

    // --- 2) Thanh toán cho HÓA ĐƠN OFFLINE (POS) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoa_don_offline", foreignKey = @ForeignKey(name = "fk_tt_hdof"))
    private HoaDonOffline hoaDonOffline;

    // --- 3) Thanh toán cho YÊU CẦU ĐỔI/TRẢ ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_yeu_cau_doi_tra", foreignKey = @ForeignKey(name = "fk_tt_ycdt"))
    private YeuCauDoiTra yeuCauDoiTra;

    @Column(name = "phuong_thuc", length = 30, nullable = false)
    private String phuongThuc; // COD / VNPAY / MOMO / CASH / CARD / QR

    @Column(name = "so_tien", precision = 12, scale = 2, nullable = false)
    private BigDecimal soTien;

    @Column(name = "trang_thai", length = 20, nullable = false)
    private String trangThai; // PENDING / PAID / FAILED / REFUNDED

    @Column(name = "ma_giao_dich", length = 100)
    private String maGiaoDich;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    /* Optional: đảm bảo 1 bản ghi chỉ gắn với MỘT trong ba đối tượng trên */
    @PrePersist @PreUpdate
    private void validateSingleTarget() {
        int cnt = 0;
        if (donHang != null) cnt++;
        if (hoaDonOffline != null) cnt++;
        if (yeuCauDoiTra != null) cnt++;
        if (cnt != 1) {
            throw new IllegalStateException("ThanhToan phải gắn đúng 1 trong các đối tượng: DonHang, HoaDonOffline, hoặc YeuCauDoiTra.");
        }
    }
}