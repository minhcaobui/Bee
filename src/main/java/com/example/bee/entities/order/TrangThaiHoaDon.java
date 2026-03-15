package com.example.bee.entities.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trang_thai_hoa_don")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrangThaiHoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String ma;

    private String ten;
}