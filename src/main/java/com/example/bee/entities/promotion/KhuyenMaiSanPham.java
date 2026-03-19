package com.example.bee.entities.promotion;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "khuyen_mai_san_pham")
public class KhuyenMaiSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_khuyen_mai", nullable = false)
    private Integer idKhuyenMai;

    @Column(name = "id_san_pham")
    private Integer idSanPham;
}