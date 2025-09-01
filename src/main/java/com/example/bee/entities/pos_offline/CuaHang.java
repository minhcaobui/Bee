package com.example.bee.entities.pos_offline;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "cua_hang",
        uniqueConstraints = @UniqueConstraint(name = "uk_cua_hang_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuaHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    @Column(name = "ten", length = 150, nullable = false)
    private String ten;

    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}
