package com.example.bee.entities.exchanges;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "yeu_cau_doi_tra_file")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YeuCauDoiTraFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Gắn với yêu cầu đổi/trả
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_yeu_cau",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ycdtf_yc")
    )
    private YeuCauDoiTra yeuCau;

    @Column(name = "url", length = 255, nullable = false)
    private String url;

    @Column(name = "mo_ta", length = 150)
    private String moTa;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();
}
