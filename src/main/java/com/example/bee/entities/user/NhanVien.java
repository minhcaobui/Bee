package com.example.bee.entities.user; // Kiểm tra lại package cho đúng folder của mày

import com.example.bee.entities.role.ChucVu;
import com.example.bee.entities.account.TaiKhoan; // Hoặc package chứa bảng Tài Khoản của mày
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Table(name = "nhan_vien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhanVien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 50)
    private String ma;

    @Column(name = "ho_ten", nullable = false, length = 150)
    private String hoTen;

    @Column(length = 50)
    private String gioiTinh;

    @Temporal(TemporalType.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Ho_Chi_Minh")
    private Date ngaySinh;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String diaChi;

    @Column(length = 12)
    private String soDienThoai;

    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String hinhAnh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chuc_vu")
    private ChucVu chucVu;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan")
    private TaiKhoan taiKhoan;

    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}