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

    // --- CÁC HÀM XỬ LÝ OTP CHUNG CHO CẢ KHÁCH HÀNG & NHÂN VIÊN ---
    @PostMapping("/gui-otp")
    public ResponseEntity<?> guiOtp(@RequestParam String email) {
        return commonService.guiOtp(email);
    }

    @PostMapping("/xac-thuc-otp")
    public ResponseEntity<?> xacThucOtp(@RequestParam String email, @RequestParam String otp) {
        return commonService.xacThucOtp(email, otp);
    }

    // --- CÁC HÀM TIỆN ÍCH DÙNG CHUNG (TÌM KIẾM, MÃ GIẢM GIÁ...) ---
    @GetMapping("/san-pham/tim-kiem")
    public ResponseEntity<List<SanPhamChiTiet>> timKiemSanPham(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Integer color,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(commonService.searchProductsForCheckout(q, color, size));
    }

    @GetMapping("/thuoc-tinh")
    public ResponseEntity<Map<String, Object>> layThuocTinh() {
        return ResponseEntity.ok(commonService.getAttributes());
    }

    @GetMapping("/khach-hang/tim-kiem")
    public ResponseEntity<List<KhachHang>> timKiemKhachHang(@RequestParam String q) {
        return ResponseEntity.ok(commonService.searchCustomers(q));
    }

    @PostMapping("/khach-hang")
    public ResponseEntity<?> taoMoiKhachHang(@RequestBody KhachHang kh) {
        try {
            return ResponseEntity.ok(commonService.createCustomerFast(kh));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @PostMapping("/ma-giam-gia/ap-dung")
    public ResponseEntity<?> apDungMaGiamGia(@RequestBody Map<String, Object> payload) {
        try {
            return ResponseEntity.ok(commonService.applyVoucher(payload));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/khach-hang/{id}/ma-giam-gia-da-dung")
    public ResponseEntity<?> layMaGiamGiaDaDungCuaKhach(@PathVariable Integer id) {
        return ResponseEntity.ok(commonService.getCustomerUsedVouchers(id));
    }
}