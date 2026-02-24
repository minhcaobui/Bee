package com.example.bee.entities.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dia_chi_khach_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaChiKhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang", nullable = false)
    @JsonIgnore // Chặn vòng lặp khi gọi API lấy địa chỉ
    private KhachHang khachHang;

    @Column(name = "ho_ten_nhan")
    private String hoTenNhan;

    @Column(name = "sdt_nhan", length = 15)
    private String sdtNhan;

    @Column(name = "dia_chi_chi_tiet", nullable = false)
    private String diaChiChiTiet;

    @Column(name = "phuong_xa")
    private String phuongXa;

    @Column(name = "quan_huyen")
    private String quanHuyen;

    @Column(name = "tinh_thanh_pho")
    private String tinhThanhPho;

    @Column(name = "loai_dia_chi")
    private String loaiDiaChi = "Nhà riêng";

    @Column(name = "la_mac_dinh")
    private Boolean laMacDinh = false;

    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}