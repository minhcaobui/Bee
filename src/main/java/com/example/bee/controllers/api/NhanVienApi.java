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

    @PostMapping("/gui-otp")
    @ResponseBody
    public ResponseEntity<?> guiOtp(@RequestParam String email) {
        return nhanVienService.guiOtp(email);
    }

    @PostMapping("/xac-thuc-otp")
    @ResponseBody
    public ResponseEntity<?> xacThucOtp(@RequestParam String email, @RequestParam String otp) {
        return nhanVienService.xacThucOtp(email, otp);
    }

    @GetMapping("/kiem-tra-email")
    @ResponseBody
    public ResponseEntity<?> kiemTraEmail(@RequestParam String email, @RequestParam(required = false) Integer id) {
        return nhanVienService.kiemTraEmail(email, id);
    }

    @GetMapping("/kiem-tra-sdt")
    @ResponseBody
    public ResponseEntity<?> kiemTraSdt(@RequestParam String phone, @RequestParam(required = false) Integer id) {
        return nhanVienService.kiemTraSdt(phone, id);
    }

    @GetMapping("/chi-tiet/{id}")
    @ResponseBody
    public ResponseEntity<NhanVien> layChiTiet(@PathVariable Integer id) {
        return nhanVienService.layChiTiet(id);
    }

    @GetMapping("/danh-sach-json")
    @ResponseBody
    public List<NhanVien> layDanhSachJson(
            @RequestParam(value = "q", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "status", required = false) Boolean status) {
        return nhanVienService.layDanhSachJson(keyword, status);
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
            @RequestParam(value = "chucVuId", required = true) Integer chucVuId,
            @RequestParam(value = "trangThai", required = false, defaultValue = "true") Boolean trangThai,
            @RequestParam(value = "hinhAnh", required = false) String hinhAnh,
            @RequestParam(value = "password", required = false) String pass,
            @RequestParam(value = "confirmPassword", required = false) String confirm) {
        return nhanVienService.luuHoSo(id, hoTen, soDienThoai, email, ngaySinhStr, gioiTinh, diaChi, chucVuId, trangThai, hinhAnh, pass, confirm);
    }

    @GetMapping("/tim-kiem")
    public String timKiem(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "status", required = false) Boolean status,
            Model model) {
        List<NhanVien> ketQua = nhanVienService.layDanhSachJson(keyword, status);
        model.addAttribute("list", ketQua);
        return "admin/staff/staff :: table_body";
    }

    @GetMapping("/test-dang-nhap")
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
    public ResponseEntity<?> capNhatHoSoCuaToi(@RequestBody Map<String, String> payload, Authentication authentication) {
        return nhanVienService.capNhatHoSoCuaToi(payload, authentication);
    }

    @PostMapping("/doi-mat-khau")
    @ResponseBody
    public ResponseEntity<?> doiMatKhauAnToan(@RequestBody Map<String, String> payload, Authentication authentication) {
        return nhanVienService.doiMatKhauAnToan(payload, authentication);
    }
}