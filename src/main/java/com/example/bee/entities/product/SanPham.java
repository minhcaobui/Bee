package com.example.bee.entities.product;

import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.entities.catalog.DanhMuc;
import com.example.bee.entities.catalog.Hang;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    // ĐÃ SỬA THÀNH NVARCHAR(MAX) CHO SQL SERVER
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngay_tao", updatable = false)
    private Date ngayTao;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ngay_sua")
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

    @OneToMany(mappedBy = "sanPham", fetch = FetchType.EAGER)
    @JsonIgnoreProperties("sanPham")
    private List<SanPhamChiTiet> chiTietSanPhams;

    @org.hibernate.annotations.BatchSize(size = 20)
    @OneToMany(mappedBy = "sanPham", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<HinhAnhSanPham> hinhAnhs = new ArrayList<>();

    public void addHinhAnh(HinhAnhSanPham hinhAnh) {
        hinhAnhs.add(hinhAnh);
        hinhAnh.setSanPham(this);
    }

    @PrePersist
    public void prePersist() {
        this.ngayTao = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.ngaySua = new Date();
    }

}