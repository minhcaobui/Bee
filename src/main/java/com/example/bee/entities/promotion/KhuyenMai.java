package com.example.bee.entities.promotion;

import com.example.bee.entities.product.SanPham;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "khuyen_mai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KhuyenMai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 10, nullable = false)
    private String ma;

    @Column(name = "ten", nullable = false, length = 150)
    private String ten;

    @Column(name = "loai", nullable = false, length = 20)
    private String loai;

    @Column(name = "gia_tri", nullable = false)
    private BigDecimal giaTri;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "cho_phep_cong_don")
    private Boolean choPhepCongDon = false;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "khuyen_mai_san_pham",
            joinColumns = @JoinColumn(name = "id_khuyen_mai"),
            inverseJoinColumns = @JoinColumn(name = "id_san_pham")
    )
    @JsonIgnore
    private List<SanPham> sanPhams;
}