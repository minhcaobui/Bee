package com.example.bee.entities.product;

import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.entities.catalog.DanhMuc;
import com.example.bee.entities.catalog.Hang;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "san_pham")
public class SanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = false, unique = true)
    private String ma;

    @Column(length = 100, nullable = false)
    private String ten;

    @Column(name = "mo_ta", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_sua")
    private LocalDateTime ngaySua;

//    @Column(name = "nguoi_sua")
//    private Integer nguoiSua;

    @Column(name = "trang_thai")
    private Boolean trangThai = true;

    // --- KHÓA NGOẠI ---
    @ManyToOne
    @JoinColumn(name = "id_hang", nullable = false)
    private Hang hang;

    @ManyToOne
    @JoinColumn(name = "id_chat_lieu", nullable = false)
    private ChatLieu chatLieu;

    @ManyToOne
    @JoinColumn(name = "id_danh_muc", nullable = false)
    private DanhMuc danhMuc;
}