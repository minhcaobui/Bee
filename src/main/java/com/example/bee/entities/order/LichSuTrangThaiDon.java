package com.example.bee.entities.order;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lich_su_trang_thai_don")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuTrangThaiDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Quan hệ N-1: nhiều bản ghi lịch sử thuộc 1 đơn hàng
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_don_hang",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ls_dh")
    )
    private DonHang donHang;

    @Column(name = "tu_trang_thai", length = 30)
    private String tuTrangThai;

    @Column(name = "den_trang_thai", length = 30, nullable = false)
    private String denTrangThai;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian = LocalDateTime.now();
}
