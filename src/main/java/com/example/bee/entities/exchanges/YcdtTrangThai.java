package com.example.bee.entities.exchanges;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "ycdt_trang_thai",
        uniqueConstraints = @UniqueConstraint(name = "uk_ycdt_trang_thai_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YcdtTrangThai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 30, nullable = false, unique = true)
    private String ma; // PENDING_REVIEW / APPROVED / RECEIVED / REJECTED / COMPLETED / CANCELLED

    @Column(name = "mo_ta", length = 100)
    private String moTa;
}
