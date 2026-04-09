package com.example.bee.entities.staff;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chuc_vu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChucVu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", unique = true, nullable = false, length = 50)
    private String ma;

    @Column(name = "ten", nullable = false, length = 150)
    private String ten;

    @Column(name = "mo_ta", length = 500)
    private String moTa;
}