package com.example.bee.entities.order;

import com.example.bee.entities.staff.NhanVien;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "yeu_cau_doi_tra")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class YeuCauDoiTra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String ma;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don")
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    private String loaiYeuCau;
    private String lyDo;
    private String ghiChu;
    private BigDecimal soTienHoan;
    private String trangThai;
    private Date ngayTao;
    private Date ngayXuLy;
}