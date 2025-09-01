package com.example.bee.entities.catalog;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "hang",
        uniqueConstraints = @UniqueConstraint(name = "uk_hang_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hang {

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
}
