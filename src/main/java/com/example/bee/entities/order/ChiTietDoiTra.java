package com.example.bee.entities.order;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chi_tiet_doi_tra")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ChiTietDoiTra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_yeu_cau_doi_tra")
    private YeuCauDoiTra yeuCauDoiTra;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don_chi_tiet")
    private HoaDonChiTiet hoaDonChiTiet;

    private Integer soLuong;
    private String tinhTrangSanPham;
}