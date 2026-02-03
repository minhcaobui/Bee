package com.example.bee.entities.product;

import com.example.bee.entities.catalog.KichThuoc;
import com.example.bee.entities.catalog.MauSac;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "san_pham_chi_tiet")
public class SanPhamChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100, nullable = false, unique = true)
    private String sku; // Mã SKU (VD: SP001-DO-L)

    @Column(name = "gia_ban", nullable = false)
    private BigDecimal giaBan;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong = 0;

    @Column(name = "hinh_anh", columnDefinition = "VARCHAR(MAX)")
    private String hinhAnh;

    @Column(name = "trang_thai")
    private Boolean trangThai = true;

    // --- KHÓA NGOẠI ---
    @ManyToOne
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @ManyToOne
    @JoinColumn(name = "id_mau_sac", nullable = false)
    private MauSac mauSac;

    @ManyToOne
    @JoinColumn(name = "id_kich_thuoc", nullable = false)
    private KichThuoc kichThuoc;
}