package com.example.bee.entities.product;

import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.entities.catalog.DanhMuc;
import com.example.bee.entities.catalog.Hang;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(
        name = "san_pham",
        uniqueConstraints = @UniqueConstraint(name = "uk_san_pham_ma", columnNames = "ma")
)
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Thêm dòng này
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String ma;

    @Column(nullable = false, length = 100)
    private String ten;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngay_tao", updatable = false)
    private Date ngayTao = new Date();

    private Date ngaySua;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hang")
    private Hang hang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chat_lieu")
    private ChatLieu chatLieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_danh_muc")
    private DanhMuc danhMuc;

    private Boolean trangThai = true;

    // Tìm đến list hinhAnhs
    @OneToMany(mappedBy = "sanPham", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // THÊM DÒNG NÀY
    private List<HinhAnhSanPham> hinhAnhs = new ArrayList<>();

    // Helper method để thêm hình ảnh đúng cách
    public void addHinhAnh(HinhAnhSanPham hinhAnh) {
        hinhAnhs.add(hinhAnh);
        hinhAnh.setSanPham(this);
    }
}
