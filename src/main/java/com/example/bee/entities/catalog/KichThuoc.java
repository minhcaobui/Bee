package com.example.bee.entities.catalog;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "kich_thuoc",
        uniqueConstraints = @UniqueConstraint(name = "uk_kich_thuoc_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KichThuoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    @Column(name = "ten", length = 50, nullable = false)
    private String ten;
}
