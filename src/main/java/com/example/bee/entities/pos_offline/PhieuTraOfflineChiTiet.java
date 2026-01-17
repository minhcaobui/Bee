package com.example.bee.entities.pos_offline;

import com.example.bee.entities.product.SanPhamBienThe;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "phieu_tra_offline_chi_tiet")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhieuTraOfflineChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Phiếu trả offline gốc
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_phieu_tra_offline",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ptofct_pt")
    )
    private PhieuTraOffline phieuTraOffline;

    // Sản phẩm biến thể được trả
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_san_pham_bien_the",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ptofct_spbt")
    )
    private SanPhamBienThe sanPhamBienThe;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "don_gia", precision = 12, scale = 2, nullable = false)
    private BigDecimal donGia;
}

