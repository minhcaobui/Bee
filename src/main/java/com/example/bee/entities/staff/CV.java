package com.example.bee.entities.staff;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
@Table(name = "chuc_vu")
public class CV {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    @Column(name = "ten", length = 150, nullable = false)
    private String ten;

}
