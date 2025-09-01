package com.example.bee.entities.catalog;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "mau_sac",
        uniqueConstraints = @UniqueConstraint(name = "uk_mau_sac_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MauSac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Có thể lưu mã HEX (#FFFFFF) hoặc ký hiệu riêng
    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    @Column(name = "ten", length = 50, nullable = false)
    private String ten;
}
