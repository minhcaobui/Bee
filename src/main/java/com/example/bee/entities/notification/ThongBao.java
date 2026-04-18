package com.example.bee.entities.notification;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "thong_bao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThongBao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_tai_khoan")
    private Integer taiKhoanId;

    @Column(name = "tieu_de")
    private String tieuDe;

    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    @Column(name = "loai_thong_bao")
    private String loaiThongBao;

    @Column(name = "da_doc")
    private Boolean daDoc = false;

    // Thêm trường này để làm tính năng Xóa mềm (Soft Delete)
    @Column(name = "da_xoa")
    private Boolean daXoa = false;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao = LocalDateTime.now();
}