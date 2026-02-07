package com.example.bee.entities.product;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hinh_anh_san_pham")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HinhAnhSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Tìm đến đối tượng sanPham
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham")
    @JsonBackReference // THÊM DÒNG NÀY
    private SanPham sanPham;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)") // Dùng MAX cho SQL Server
    private String url;
}
