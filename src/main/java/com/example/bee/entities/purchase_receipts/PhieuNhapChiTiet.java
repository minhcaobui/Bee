package com.example.bee.entities.purchase_receipts;

import com.example.bee.entities.catalog.SanPhamBienThe;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "phieu_nhap_chi_tiet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhieuNhapChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Mỗi chi tiết gắn với 1 phiếu nhập
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_phieu_nhap",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pnct_pn")
    )
    private PhieuNhap phieuNhap;

    // Mỗi chi tiết gắn với 1 biến thể sản phẩm
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_san_pham_bien_the",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pnct_spbt")
    )
    private SanPhamBienThe sanPhamBienThe;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "don_gia", precision = 12, scale = 2, nullable = false)
    private BigDecimal donGia;
}
