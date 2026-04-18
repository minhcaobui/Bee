package com.example.bee.entities.order;

import com.example.bee.constants.PhuongThucThanhToan;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.entities.staff.NhanVien;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "hoa_don")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, length = 50)
    private String ma;

    private BigDecimal giaTamThoi;

    private BigDecimal phiVanChuyen;

    private BigDecimal giaTriKhuyenMai;

    private BigDecimal giaTong;

    // Cột JSON lưu trữ thông tin nhận hàng
    @Column(name = "thong_tin_giao_hang", columnDefinition = "NVARCHAR(MAX)")
    @Convert(converter = com.example.bee.converters.ThongTinGiaoHangConverter.class)
    private ThongTinGiaoHang thongTinGiaoHang;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    // Phân loại hình thức: GIAO_TAN_NOI hoặc NHAN_TAI_CUA_HANG
    @Column(name = "hinh_thuc_giao_hang", length = 50)
    private String hinhThucGiaoHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ma_giam_gia")
    private MaGiamGia maGiamGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_trang_thai_hoa_don")
    private TrangThaiHoaDon trangThaiHoaDon;

    private Integer loaiHoaDon;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    private Date ngayTao;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    private Date ngayThanhToan;

    // Các cột thời gian cho API Vận chuyển và Lấy tại cửa hàng
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngay_nhan_hang_du_kien")
    private Date ngayNhanHangDuKien;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngay_hen_lay_hang")
    private Date ngayHenLayHang;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngay_hang_san_sang")
    private Date ngayHangSanSang;

    @PrePersist
    public void prePersist() {
        this.ngayTao = new Date();
        if (this.loaiHoaDon == null) this.loaiHoaDon = 0;
        if (this.phiVanChuyen == null) this.phiVanChuyen = BigDecimal.ZERO;
        if (this.giaTriKhuyenMai == null) this.giaTriKhuyenMai = BigDecimal.ZERO;

        if (this.giaTamThoi == null) this.giaTamThoi = BigDecimal.ZERO;
        if (this.giaTong == null) this.giaTong = BigDecimal.ZERO;
        if (this.hinhThucGiaoHang == null) this.hinhThucGiaoHang = "GIAO_TAN_NOI";
    }

    @OneToMany(mappedBy = "hoaDon")
    @JsonIgnoreProperties("hoaDon")
    private List<ThanhToan> thanhToans;

    @Transient
    public String getPhuongThucThanhToan() {
        if (this.thanhToans != null && !this.thanhToans.isEmpty()) {
            return this.thanhToans.get(0).getPhuongThuc();
        }
        // Dùng class Constants thay vì gõ chữ cứng
        if (this.loaiHoaDon != null && this.loaiHoaDon == 0) {
            return PhuongThucThanhToan.TIEN_MAT;
        }
        return PhuongThucThanhToan.COD;
    }
}