package com.example.bee.entities.account;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vai_tro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaiTro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    @Column(name = "ten", length = 100, nullable = false)
    private String ten;
}