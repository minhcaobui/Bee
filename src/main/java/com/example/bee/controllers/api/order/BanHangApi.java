package com.example.bee.controllers.api.order;

import com.example.bee.services.PosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ban-hang")
@RequiredArgsConstructor
public class BanHangApi {

    private final PosService posService;

    @PostMapping("/hoa-don/cap-nhat-giu-hang")
    public ResponseEntity<?> capNhatGiuKho(@RequestBody Map<String, Object> duLieu) {
        try {
            Integer idSanPhamChiTiet = (Integer) duLieu.get("idSanPhamChiTiet");
            Integer soLuongThayDoi = (Integer) duLieu.get("soLuongThayDoi");
            posService.capNhatGiuKho(idSanPhamChiTiet, soLuongThayDoi);
            return ResponseEntity.ok(Map.of("message", "OK"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/hoan-thanh")
    public ResponseEntity<?> hoanThanhDonHang(@RequestBody Map<String, Object> duLieu) {
        try {
            return ResponseEntity.ok(posService.hoanThanhDonHang(duLieu));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @PostMapping("/xac-nhan-thanh-toan-online")
    public ResponseEntity<?> xacNhanThanhToanOnline(@RequestBody Map<String, String> duLieu) {
        try {
            return ResponseEntity.ok(posService.xacNhanThanhToanOnline(duLieu.get("maHD")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}