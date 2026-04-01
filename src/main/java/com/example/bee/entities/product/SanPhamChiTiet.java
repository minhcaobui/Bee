package com.example.bee.entities.product;

import com.example.bee.entities.catalog.KichThuoc;
import com.example.bee.entities.catalog.MauSac;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "san_pham_chi_tiet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPhamChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(name = "gia_ban", nullable = false, precision = 18, scale = 2)
    private BigDecimal giaBan;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong = 0;

    @Column(name = "so_luong_tam_giu", nullable = false)
    @Builder.Default
    private Integer soLuongTamGiu = 0;

    @Column(name = "hinh_anh", length = 2048)
    private String hinhAnh;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mau_sac", nullable = false)
    private MauSac mauSac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_kich_thuoc", nullable = false)
    private KichThuoc kichThuoc;

    @Transient
    private BigDecimal giaSauKhuyenMai;

    @Transient
    public Integer getSoLuongKhaDung() {
        int khaDung = this.soLuong - (this.soLuongTamGiu != null ? this.soLuongTamGiu : 0);
        return Math.max(0, khaDung);
    }
}