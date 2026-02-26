package com.example.bee.entities.order;

import com.example.bee.entities.product.SanPhamChiTiet;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hoa_don_chi_tiet")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class HoaDonChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Double giaTien;

    @Column(nullable = false)
    private Integer soLuong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoa_don")
    private HoaDon hoaDon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham_chi_tiet")
    private SanPhamChiTiet sanPhamChiTiet;
}