package com.example.bee.controllers.api.payment;

import com.example.bee.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/thanh-toan")
@RequiredArgsConstructor
public class ThanhToanApi {

    private final PaymentService paymentService;

    @PostMapping("/momo/{idHoaDon}")
    public ResponseEntity<?> layUrlMomo(@PathVariable Integer idHoaDon) {
        try {
            return ResponseEntity.ok(Map.of("payUrl", paymentService.getMomoUrl(idHoaDon)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/momo-phan-hoi")
    public ResponseEntity<Void> phanHoiMomo(HttpServletRequest request) {
        String redirectUrl = paymentService.momoCallback(request);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    @PostMapping("/vnpay/{idHoaDon}")
    public ResponseEntity<?> layUrlVnPay(@PathVariable Integer idHoaDon) {
        try {
            return ResponseEntity.ok(Map.of("payUrl", paymentService.getVnPayUrl(idHoaDon)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/vnpay-phan-hoi")
    public ResponseEntity<Void> phanHoiVnPay(HttpServletRequest request) {
        String redirectUrl = paymentService.vnpayCallback(request);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }
}