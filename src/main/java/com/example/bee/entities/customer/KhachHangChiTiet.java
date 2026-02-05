package com.example.bee.entities.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "khach_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhachHangChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Mã khách hàng không được để trống")
    @Size(max = 50, message = "Mã tối đa 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Mã chỉ được chứa chữ cái, số, dấu gạch dưới và dấu gạch ngang")
    @Column(nullable = false, unique = true, length = 50)
    private String ma;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Size(max = 50, message = "Giới tính tối đa 50 ký tự")
    @Column(name = "gioi_tinh", length = 50)
    private String gioiTinh;

    @PastOrPresent(message = "Ngày sinh không được trong tương lai")
    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    @Column(name = "dia_chi")
    private String diaChi;

    @Size(max = 12, message = "Số điện thoại tối đa 12 ký tự")
    @Pattern(regexp = "^0[0-9]{9}$|^\\+84[0-9]{9}$", message = "Số điện thoại không đúng định dạng")
    @Column(name = "so_dien_thoai", length = 12)
    private String soDienThoai;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    @Size(max = 500, message = "Đường dẫn hình ảnh tối đa 500 ký tự")
    @Column(name = "hinh_anh")
    private String hinhAnh;

    @Column(name = "id_tai_khoan")
    private Integer idTaiKhoan;


    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @OneToMany(mappedBy = "khachHangChiTiet", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DiaChiKhachHang> diaChiList = new ArrayList<>();

    public void addDiaChi(DiaChiKhachHang diaChi) {
        diaChiList.add(diaChi);
        diaChi.setKhachHangChiTiet(this);
    }
}