package com.example.bee.entities.product;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "san_pham_yeu_thich")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPhamYeuThich {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tai_khoan_id")
    private Integer taiKhoanId;

    @Column(name = "san_pham_id")
    private Integer sanPhamId;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();
}