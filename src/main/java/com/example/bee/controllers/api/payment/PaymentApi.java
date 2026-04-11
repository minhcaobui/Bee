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
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentApi {

    private final PaymentService paymentService;

    @PostMapping("/momo/{invoiceId}")
    public ResponseEntity<?> getMomoUrl(@PathVariable Integer invoiceId) {
        try {
            return ResponseEntity.ok(Map.of("payUrl", paymentService.getMomoUrl(invoiceId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/momo-callback")
    public ResponseEntity<Void> momoCallback(HttpServletRequest request) {
        String redirectUrl = paymentService.momoCallback(request);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    @PostMapping("/vnpay/{invoiceId}")
    public ResponseEntity<?> getVnPayUrl(@PathVariable Integer invoiceId) {
        try {
            return ResponseEntity.ok(Map.of("payUrl", paymentService.getVnPayUrl(invoiceId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/vnpay-callback")
    public ResponseEntity<Void> vnpayCallback(HttpServletRequest request) {
        String redirectUrl = paymentService.vnpayCallback(request);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }
}