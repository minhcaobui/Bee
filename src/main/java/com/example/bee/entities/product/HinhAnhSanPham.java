package com.example.bee.entities.product;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hinh_anh_san_pham")
@Getter // CHỈ DÙNG GETTER & SETTER, BỎ @Data ĐI
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HinhAnhSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham")
    @JsonBackReference
    private SanPham sanPham;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String url;
}