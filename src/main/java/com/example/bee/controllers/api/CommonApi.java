package com.example.bee.controllers.api;

import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.services.CommonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chung")
@RequiredArgsConstructor
public class CommonApi {

    private final CommonService commonService;

    // --- CÁC HÀM XỬ LÝ OTP & KIỂM TRA DỮ LIỆU ---

    @PostMapping("/gui-otp")
    public ResponseEntity<?> guiOtp(@RequestParam String email) {
        return commonService.guiOtp(email);
    }

    @PostMapping("/xac-thuc-otp")
    public ResponseEntity<?> xacThucOtp(@RequestParam String email, @RequestParam String maXacThuc) {
        return commonService.xacThucOtp(email, maXacThuc);
    }

    @GetMapping("/kiem-tra-email")
    public ResponseEntity<?> kiemTraEmail(
            @RequestParam String email,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false, defaultValue = "KHACH_HANG") String loaiTaiKhoan) {
        return commonService.kiemTraEmail(email, id, loaiTaiKhoan);
    }

    @GetMapping("/kiem-tra-sdt")
    public ResponseEntity<?> kiemTraSdt(
            @RequestParam String soDienThoai,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false, defaultValue = "KHACH_HANG") String loaiTaiKhoan) {
        return commonService.kiemTraSdt(soDienThoai, id, loaiTaiKhoan);
    }

    // --- CÁC HÀM TIỆN ÍCH DÙNG CHUNG ---

    @GetMapping("/san-pham/tim-kiem")
    public ResponseEntity<List<SanPhamChiTiet>> timKiemSanPham(
            @RequestParam(required = false, defaultValue = "") String tuKhoa,
            @RequestParam(required = false) Integer idMauSac,
            @RequestParam(required = false) Integer idKichThuoc) {
        return ResponseEntity.ok(commonService.timKiemSanPhamChoThanhToan(tuKhoa, idMauSac, idKichThuoc));
    }

    @GetMapping("/thuoc-tinh")
    public ResponseEntity<Map<String, Object>> layThuocTinh() {
        return ResponseEntity.ok(commonService.layThuocTinh());
    }

    @GetMapping("/khach-hang/tim-kiem")
    public ResponseEntity<List<KhachHang>> timKiemKhachHang(@RequestParam String tuKhoa) {
        return ResponseEntity.ok(commonService.timKiemKhachHang(tuKhoa));
    }

    @PostMapping("/khach-hang")
    public ResponseEntity<?> taoMoiKhachHangNhanh(@RequestBody KhachHang kh) {
        try {
            return ResponseEntity.ok(commonService.taoMoiKhachHangNhanh(kh));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @PostMapping("/ma-giam-gia/ap-dung")
    public ResponseEntity<?> apDungMaGiamGia(@RequestBody Map<String, Object> duLieu) {
        try {
            return ResponseEntity.ok(commonService.apDungMaGiamGia(duLieu));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/khach-hang/{id}/ma-giam-gia-da-dung")
    public ResponseEntity<?> layMaGiamGiaDaDungCuaKhach(@PathVariable Integer id) {
        return ResponseEntity.ok(commonService.layMaGiamGiaDaDungCuaKhach(id));
    }
}