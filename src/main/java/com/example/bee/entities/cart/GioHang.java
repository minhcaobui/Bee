package com.example.bee.entities.cart;

import com.example.bee.entities.account.TaiKhoan;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "gio_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_tai_khoan",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_gh_tk")
    )
    private TaiKhoan taiKhoan;

    @Column(name = "cap_nhat_cuoi", nullable = false)
    private LocalDateTime capNhatCuoi;

    @OneToMany(mappedBy = "gioHang", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GioHangChiTiet> chiTiets;

    @PrePersist
    public void prePersist() {
        if (capNhatCuoi == null) capNhatCuoi = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        capNhatCuoi = LocalDateTime.now();
    }
}