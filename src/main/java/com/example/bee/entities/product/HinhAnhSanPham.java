package com.example.bee.entities.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hinh_anh_san_pham")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HinhAnhSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // Cái URL ảnh đại diện hay ảnh phụ gì thì nó nằm ở đây hết
    @Column(name = "url", columnDefinition = "VARCHAR(MAX)")
    private String url;

    // Nối về bảng cha SanPham
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham", referencedColumnName = "id")
    private SanPham sanPham;
}