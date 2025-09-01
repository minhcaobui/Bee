package com.example.bee.entities.account;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tai_khoan",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tai_khoan_ten_dang_nhap", columnNames = "ten_dang_nhap"),
                @UniqueConstraint(name = "uk_tai_khoan_email", columnNames = "email")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_dang_nhap", length = 100, nullable = false, unique = true)
    private String tenDangNhap;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    @Lob
    @Column(name = "mat_khau", nullable = false)
    private byte[] matKhau;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
