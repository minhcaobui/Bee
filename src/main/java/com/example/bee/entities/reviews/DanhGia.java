package com.example.bee.entities.reviews;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.order.HoaDonChiTiet;
import com.example.bee.entities.product.SanPham;
import com.example.bee.entities.staff.NhanVien;
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

    @ManyToOne
    @JoinColumn(name = "id_tai_khoan")
    private TaiKhoan taiKhoan;

    @ManyToOne
    @JoinColumn(name = "id_san_pham")
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

    @ManyToOne
    @JoinColumn(name = "id_hoa_don_chi_tiet")
    private HoaDonChiTiet hoaDonChiTiet;

    @Column(name = "da_sua")
    private Boolean daSua = false;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien_tra_loi")
    private NhanVien nhanVienTraLoi;

    @Column(name = "noi_dung_tra_loi", columnDefinition = "NVARCHAR(MAX)")
    private String noiDungTraLoi;

    @Column(name = "ngay_tra_loi")
    private Date ngayTraLoi;

    @Transient
    private String tenKhachHang;

    @Transient
    public List<String> getDanhSachHinhAnhList() {
        if (this.danhSachHinhAnh == null || this.danhSachHinhAnh.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(this.danhSachHinhAnh.split(","));
    }
}