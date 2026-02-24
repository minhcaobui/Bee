package com.example.bee.entities.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "khach_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, unique = true, nullable = false)
    private String ma;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "gioi_tinh")
    private String gioiTinh;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    // Giữ trường này để hiển thị nhanh (dù DB hơi thừa)
    @Column(name = "dia_chi")
    private String diaChi;

    @Column(name = "so_dien_thoai", length = 12)
    private String soDienThoai;

    @Column(name = "email")
    private String email;

    @Column(name = "hinh_anh")
    private String hinhAnh;

    @Column(name = "id_tai_khoan")
    private Integer idTaiKhoan;

    @Column(name = "trang_thai")
    private Boolean trangThai = true;

    // QUAN TRỌNG: mappedBy phải trỏ đúng tên biến "khachHang" ở file dưới
    @OneToMany(mappedBy = "khachHang", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DiaChiKhachHang> diaChiList = new ArrayList<>();
}