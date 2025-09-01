package com.example.bee.entities.pos_offline;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "phieu_tra_offline")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhieuTraOffline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Gắn với hóa đơn offline gốc
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_hoa_don_offline",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ptof_hdof")
    )
    private HoaDonOffline hoaDonOffline;

    // Nhân viên lập phiếu trả/đổi
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_nhan_vien",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ptof_nv")
    )
    private NhanVien nhanVien;

    @Column(name = "ly_do", length = 255)
    private String lyDo;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
