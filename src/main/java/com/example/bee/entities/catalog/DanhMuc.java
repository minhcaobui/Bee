package com.example.bee.entities.catalog;

import com.example.bee.entities.product.SanPham;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
        name = "danh_muc",
        uniqueConstraints = @UniqueConstraint(name = "uk_danh_muc_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhMuc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    @Column(name = "ten", length = 150, nullable = false)
    private String ten;

    @Column(name = "mo_ta", length = 255)
    private String moTa;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @OneToMany(mappedBy = "danhMuc")
    @JsonIgnore // <--- THÊM CÁI NÀY VÀO! QUAN TRỌNG VÃI LỒN!
    private List<SanPham> sanPhams;
}
