package com.example.bee.entities.customer;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

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
    private KhachHangChiTiet khachHangChiTiet;  // ← Đổi từ KhachHang thành KhachHangChiTiet

    @Size(max = 150, message = "Họ tên người nhận tối đa 150 ký tự")
    @Column(name = "ho_ten_nhan")
    private String hoTenNhan;

    @Size(max = 15, message = "SĐT người nhận tối đa 15 ký tự")
    @Pattern(regexp = "^0[0-9]{9}$|^\\+84[0-9]{9}$", message = "Số điện thoại không đúng định dạng")
    @Column(name = "sdt_nhan", length = 15)
    private String sdtNhan;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    @Size(max = 500, message = "Địa chỉ chi tiết tối đa 500 ký tự")
    @Column(name = "dia_chi_chi_tiet", nullable = false)
    private String diaChiChiTiet;

    @Size(max = 100, message = "Phường/xã tối đa 100 ký tự")
    private String phuongXa;

    @Size(max = 100, message = "Quận/huyện tối đa 100 ký tự")
    private String quanHuyen;

    @Size(max = 100, message = "Tỉnh/thành phố tối đa 100 ký tự")
    private String tinhThanhPho;

    @Size(max = 50, message = "Loại địa chỉ tối đa 50 ký tự")
    @Column(name = "loai_dia_chi")
    private String loaiDiaChi = "Nhà riêng";

    @Column(name = "la_mac_dinh")
    private Boolean laMacDinh = false;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_sua")
    private LocalDateTime ngaySua;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}