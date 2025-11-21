package com.example.bee.entities.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "san_pham",
        uniqueConstraints = @UniqueConstraint(name = "uk_san_pham_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    @Column(name = "ten", length = 200, nullable = false)
    private String ten;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "hinh_anh_dai_dien", length = 255)
    private String hinhAnhDaiDien;

    @ManyToOne(fetch = FetchType.EAGER) // <--- THÊM fetch = FetchType.EAGER
    @JoinColumn(name = "id_danh_muc")
    private DanhMuc danhMuc;

    @ManyToOne(fetch = FetchType.EAGER) // <--- THÊM fetch = FetchType.EAGER
    @JoinColumn(name = "id_hang")
    private Hang hang;

    @ManyToOne(fetch = FetchType.EAGER) // <--- THÊM fetch = FetchType.EAGER
    @JoinColumn(name = "id_chat_lieu")
    private ChatLieu chatLieu;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @OneToMany(mappedBy = "sanPham")
    @JsonIgnore // <--- PHẢI CÓ CÁI NÀY!
    private List<SanPhamBienThe> bienThes; // Tên biến của mày

    // CHẶN VÒNG LẶP SANG ẢNH (Images)
    @OneToMany(mappedBy = "sanPham")
    @JsonIgnore // <--- PHẢI CÓ CÁI NÀY NỮA!
    private List<HinhAnhSanPham> hinhAnhs; // Tên biến của mày
}
