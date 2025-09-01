package com.example.bee.entities.order;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "trang_thai_don_hang",
        uniqueConstraints = @UniqueConstraint(name = "uk_trang_thai_don_hang_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrangThaiDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 30, nullable = false, unique = true)
    private String ma; // ví dụ: NEW, CONFIRMED, PACKED, SHIPPING, COMPLETED, CANCELLED

    @Column(name = "mo_ta", length = 100)
    private String moTa;
}