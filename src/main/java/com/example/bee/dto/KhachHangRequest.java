package com.example.bee.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class KhachHangRequest {

    private String ma;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String hoTen;

    @Size(max = 50, message = "Giới tính tối đa 50 ký tự")
    private String gioiTinh;

    @PastOrPresent(message = "Ngày sinh không được trong tương lai")
    private LocalDate ngaySinh;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String diaChi;

    @Size(max = 12, message = "Số điện thoại tối đa 12 ký tự")
    @Pattern(regexp = "^0[0-9]{9}$|^\\+84[0-9]{9}$",
            message = "Số điện thoại không đúng định dạng (ví dụ: 0912345678 hoặc +84912345678)")
    private String soDienThoai;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    @Size(max = 500, message = "Đường dẫn hình ảnh tối đa 500 ký tự")
    private String hinhAnh;

    private Integer idTaiKhoan;

    private Boolean trangThai;
}