package com.example.bee.entities.purchase_receipts;

import com.example.bee.entities.pos_offline.NhanVien;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "phieu_nhap",
        uniqueConstraints = @UniqueConstraint(name = "uk_phieu_nhap_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhieuNhap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    // Nhân viên lập phiếu nhập
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_nhan_vien",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pn_nv")
    )
    private NhanVien nhanVien;

    @Column(name = "ngay", nullable = false)
    private LocalDateTime ngay = LocalDateTime.now();

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}
