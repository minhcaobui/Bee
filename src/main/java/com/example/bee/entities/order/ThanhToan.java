package com.example.bee.entities.order;

import com.example.bee.entities.user.NhanVien;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Table(name = "thanh_toan")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ThanhToan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoa_don", nullable = false)
    private HoaDon hoaDon;

    @Column(nullable = false)
    private Double soTien;

    @Column(nullable = false, length = 50)
    private String phuongThuc; // TIEN_MAT, CHUYEN_KHOAN, VNPAY...

    @Builder.Default // THÊM DÒNG NÀY VÀO
    @Column(length = 20)
    private String loaiThanhToan = "THANH_TOAN"; // THANH_TOAN, HOAN_TIEN

    @Builder.Default // THÊM CẢ VÀO ĐÂY NỮA CHO CHẮC CÚ
    @Column(length = 20)
    private String trangThai = "THANH_CONG";

    private String maGiaoDich;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date ngayThanhToan;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @PrePersist
    public void prePersist() {
        if (this.ngayThanhToan == null) this.ngayThanhToan = new Date();
    }
}