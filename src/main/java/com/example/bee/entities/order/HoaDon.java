package com.example.bee.entities.order;

import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.user.NhanVien; // Check lại package nhan vien của mày
import com.example.bee.entities.promotion.MaGiamGia; // Check lại package ma giam gia
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "hoa_don")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
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

    @Column(name = "ten_nguoi_nhan")
    private String tenNguoiNhan;

    @Column(name = "sdt_nhan")
    private String sdtNhan;

    @Column(name = "dia_chi_giao_hang")
    private String diaChiGiaoHang;

    private String phuongThucThanhToan;
    private String ghiChu;

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

    private Integer loaiHoaDon; // 0: Tại quầy, 1: Online

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    private Date ngayTao;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    private Date ngayThanhToan;

    @PrePersist
    public void prePersist() {
        this.ngayTao = new Date();
        if (this.loaiHoaDon == null) this.loaiHoaDon = 0;
        if (this.phiVanChuyen == null) this.phiVanChuyen = BigDecimal.ZERO;
        if (this.giaTriKhuyenMai == null) this.giaTriKhuyenMai = BigDecimal.ZERO;

        if (this.giaTamThoi == null) this.giaTamThoi = BigDecimal.ZERO;
        if (this.giaTong == null) this.giaTong = BigDecimal.ZERO;
    }
}