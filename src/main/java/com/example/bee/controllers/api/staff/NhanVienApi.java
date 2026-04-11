package com.example.bee.controllers.api.staff;

import com.example.bee.entities.staff.NhanVien;
import com.example.bee.services.NhanVienService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/nhan-vien")
@RequiredArgsConstructor
public class NhanVienApi {

    private final NhanVienService nhanVienService;

    @GetMapping("/chi-tiet/{id}")
    @ResponseBody
    public ResponseEntity<NhanVien> layChiTiet(@PathVariable Integer id) {
        return nhanVienService.layChiTiet(id);
    }

    @GetMapping("/danh-sach-json")
    @ResponseBody
    public List<NhanVien> layDanhSachJson(
            @RequestParam(value = "tuKhoa", required = false, defaultValue = "") String tuKhoa,
            @RequestParam(value = "trangThai", required = false) Boolean trangThai) {
        return nhanVienService.layDanhSachJson(tuKhoa, trangThai);
    }

    @PatchMapping("/doi-trang-thai/{id}")
    @ResponseBody
    public ResponseEntity<?> doiTrangThai(@PathVariable Integer id) {
        return nhanVienService.doiTrangThai(id);
    }

    @PostMapping("/luu")
    @ResponseBody
    public ResponseEntity<?> luuHoSo(
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "hoTen", required = true) String hoTen,
            @RequestParam(value = "soDienThoai", required = true) String soDienThoai,
            @RequestParam(value = "email", required = true) String email,
            @RequestParam(value = "ngaySinh", required = true) String ngaySinhStr,
            @RequestParam(value = "gioiTinh", required = false) String gioiTinh,
            @RequestParam(value = "diaChi", required = false) String diaChi,
            @RequestParam(value = "idChucVu", required = true) Integer idChucVu,
            @RequestParam(value = "trangThai", required = false, defaultValue = "true") Boolean trangThai,
            @RequestParam(value = "hinhAnh", required = false) String hinhAnh,
            @RequestParam(value = "matKhau", required = false) String matKhau,
            @RequestParam(value = "xacNhanMatKhau", required = false) String xacNhanMatKhau) {
        return nhanVienService.luuHoSo(id, hoTen, soDienThoai, email, ngaySinhStr, gioiTinh, diaChi, idChucVu, trangThai, hinhAnh, matKhau, xacNhanMatKhau);
    }

    @GetMapping("/tim-kiem")
    public String timKiem(
            @RequestParam(value = "tuKhoa", required = false, defaultValue = "") String tuKhoa,
            @RequestParam(value = "trangThai", required = false) Boolean trangThai,
            Model model) {
        List<NhanVien> ketQua = nhanVienService.layDanhSachJson(tuKhoa, trangThai);
        model.addAttribute("list", ketQua);
        return "admin/staff/staff :: table_body";
    }

    @GetMapping("/kiem-tra-dang-nhap")
    @ResponseBody
    public String kiemTraDangNhap() {
        return nhanVienService.kiemTraDangNhap();
    }

    @GetMapping("/ho-so-cua-toi")
    @ResponseBody
    public ResponseEntity<?> layHoSoCuaToi(Authentication authentication) {
        return nhanVienService.layHoSoCuaToi(authentication);
    }

    @PutMapping("/ho-so-cua-toi")
    @ResponseBody
    public ResponseEntity<?> capNhatHoSoCuaToi(@RequestBody Map<String, String> duLieu, Authentication authentication) {
        return nhanVienService.capNhatHoSoCuaToi(duLieu, authentication);
    }

    @PostMapping("/doi-mat-khau")
    @ResponseBody
    public ResponseEntity<?> doiMatKhauAnToan(@RequestBody Map<String, String> duLieu, Authentication authentication) {
        return nhanVienService.doiMatKhauAnToan(duLieu, authentication);
    }
}