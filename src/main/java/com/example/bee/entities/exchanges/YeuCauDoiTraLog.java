package com.example.bee.entities.exchanges;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "yeu_cau_doi_tra_log",
        indexes = {
                @Index(name = "ix_ycdtlog_yc", columnList = "id_yeu_cau")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YeuCauDoiTraLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Gắn với yêu cầu đổi/trả
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_yeu_cau",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ycdtlog_yc")
    )
    private YeuCauDoiTra yeuCau;

    @Column(name = "tu_trang_thai", length = 30)
    private String tuTrangThai;

    @Column(name = "den_trang_thai", length = 30, nullable = false)
    private String denTrangThai;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian = LocalDateTime.now();
}
