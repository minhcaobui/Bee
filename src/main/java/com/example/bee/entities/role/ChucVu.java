package com.example.bee.entities.role;

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

    @Column(unique = true, nullable = false, length = 50)
    private String ma;

    @Column(nullable = false, length = 150)
    private String ten;
}