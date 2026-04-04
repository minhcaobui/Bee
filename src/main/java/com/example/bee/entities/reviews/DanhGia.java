package com.example.bee.entities.reviews;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.order.HoaDonChiTiet;
import com.example.bee.entities.product.SanPham;
import com.example.bee.entities.user.NhanVien;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "danh_gia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🌟 Đã map khóa ngoại TaiKhoan
    @ManyToOne
    @JoinColumn(name = "tai_khoan_id")
    private TaiKhoan taiKhoan;

    // 🌟 Đã map khóa ngoại SanPham
    @ManyToOne
    @JoinColumn(name = "san_pham_id")
    private SanPham sanPham;

    @Column(name = "so_sao")
    private Integer soSao;

    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    @Column(name = "phan_loai")
    private String phanLoai;

    @Column(name = "danh_sach_hinh_anh", columnDefinition = "NVARCHAR(MAX)")
    private String danhSachHinhAnh;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();

    // 🌟 Đã map khóa ngoại HoaDonChiTiet
    @ManyToOne
    @JoinColumn(name = "hoa_don_chi_tiet_id")
    private HoaDonChiTiet hoaDonChiTiet;

    @Column(name = "da_sua")
    private Boolean daSua = false;

    // 🌟 Map khóa ngoại NhanVien
    @ManyToOne
    @JoinColumn(name = "nhan_vien_tra_loi_id")
    private NhanVien nhanVienTraLoi;

    @Column(name = "noi_dung_tra_loi", columnDefinition = "NVARCHAR(MAX)")
    private String noiDungTraLoi;

    @Column(name = "ngay_tra_loi")
    private Date ngayTraLoi;

    @Transient
    private String tenKhachHang;

    // Tự động cắt chuỗi link ảnh thành mảng cho Frontend dễ dùng
    @Transient
    public List<String> getDanhSachHinhAnhList() {
        if (this.danhSachHinhAnh == null || this.danhSachHinhAnh.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(this.danhSachHinhAnh.split(","));
    }
}