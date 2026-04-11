package com.example.bee.controllers.api.pos;

import com.example.bee.services.PosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ban-hang")
@RequiredArgsConstructor
public class PosApi {

    private final PosService posService;

    @PostMapping("/hoa-don/cap-nhat-giu-hang")
    public ResponseEntity<?> capNhatGiuKho(@RequestBody Map<String, Object> body) {
        try {
            Integer spctId = (Integer) body.get("spctId");
            Integer delta = (Integer) body.get("delta");
            posService.updateHoldStock(spctId, delta);
            return ResponseEntity.ok(Map.of("message", "OK"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/hoan-thanh")
    public ResponseEntity<?> hoanThanhDonHang(@RequestBody Map<String, Object> payload) {
        try {
            return ResponseEntity.ok(posService.finishOrder(payload));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @PostMapping("/xac-nhan-thanh-toan-online")
    public ResponseEntity<?> xacNhanThanhToanOnline(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(posService.confirmOnlinePayment(body.get("maHD")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}