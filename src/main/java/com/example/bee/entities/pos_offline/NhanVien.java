package com.example.bee.entities.pos_offline;

import com.example.bee.entities.account.TaiKhoan;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "nhan_vien",
        uniqueConstraints = @UniqueConstraint(name = "uk_nhan_vien_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhanVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    @Column(name = "ho_ten", length = 150, nullable = false)
    private String hoTen;

    // Nếu nhân viên có tài khoản đăng nhập POS
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_tai_khoan",
            foreignKey = @ForeignKey(name = "fk_nv_tk")
    )
    private TaiKhoan taiKhoan;
}
