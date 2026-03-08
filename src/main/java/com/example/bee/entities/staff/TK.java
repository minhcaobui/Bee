package com.example.bee.entities.staff;


import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
@Table(name = "tai_khoan")
public class TK {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_dang_nhap", length = 150, nullable = false, unique = true)
    private String tenDangNhap;

    @Column(name = "mat_khau", nullable = false)
    private String matKhau;

    @Column(name = "trang_thai")
    private Boolean trangThai = true;

    @ManyToOne
    @JoinColumn(name = "id_vai_tro")
    private VT vaiTro;


}

