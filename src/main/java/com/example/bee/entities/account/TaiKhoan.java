package com.example.bee.entities.account;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tai_khoan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_dang_nhap", unique = true)
    private String tenDangNhap;

    @Column(name = "mat_khau")
    private String matKhau;

    @Column(name = "trang_thai")
    private Boolean trangThai;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_vai_tro")
    private VaiTro vaiTro;

    @Column(name = "da_doi_ten_dang_nhap")
    private Boolean daDoiTenDangNhap = false;
}