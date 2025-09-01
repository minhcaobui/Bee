package com.example.bee.entities.account;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dia_chi")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaChi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Quan hệ N-1: nhiều địa chỉ thuộc 1 tài khoản
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_tai_khoan",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dc_tk")
    )
    private TaiKhoan taiKhoan;

    @Column(name = "ho_ten", length = 150, nullable = false)
    private String hoTen;

    @Column(name = "so_dien_thoai", length = 20, nullable = false)
    private String soDienThoai;

    @Column(name = "tinh", length = 100, nullable = false)
    private String tinh;

    @Column(name = "quan", length = 100, nullable = false)
    private String quan;

    @Column(name = "phuong", length = 100, nullable = false)
    private String phuong;

    @Column(name = "dia_chi_chi_tiet", length = 255, nullable = false)
    private String diaChiChiTiet;

    @Column(name = "mac_dinh", nullable = false)
    private Boolean macDinh = false;
}