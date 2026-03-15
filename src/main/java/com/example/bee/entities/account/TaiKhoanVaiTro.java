package com.example.bee.entities.account;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "tai_khoan_vai_tro",
        uniqueConstraints = @UniqueConstraint(name = "uq_tkvt", columnNames = {"id_tai_khoan", "id_vai_tro"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaiKhoanVaiTro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tai_khoan", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tkvt_tk"))
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vai_tro", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tkvt_vt"))
    private VaiTro vaiTro;
}