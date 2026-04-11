package com.example.bee.controllers.api;

import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.services.CommonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonApi {

    private final CommonService commonService;

    @GetMapping("/invoices/{id}/print-data")
    public ResponseEntity<?> getInvoicePrintData(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(commonService.getInvoicePrintData(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<SanPhamChiTiet>> searchProducts(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Integer color,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(commonService.searchProductsForCheckout(q, color, size));
    }

    @GetMapping("/attributes")
    public ResponseEntity<Map<String, Object>> getAttributes() {
        return ResponseEntity.ok(commonService.getAttributes());
    }

    @GetMapping("/customers/search")
    public ResponseEntity<List<KhachHang>> searchCustomers(@RequestParam String q) {
        return ResponseEntity.ok(commonService.searchCustomers(q));
    }

    @PostMapping("/customers")
    public ResponseEntity<?> createCustomer(@RequestBody KhachHang kh) {
        try {
            return ResponseEntity.ok(commonService.createCustomerFast(kh));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @PostMapping("/vouchers/apply")
    public ResponseEntity<?> applyVoucher(@RequestBody Map<String, Object> payload) {
        try {
            return ResponseEntity.ok(commonService.applyVoucher(payload));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/customers/{id}/used-vouchers")
    public ResponseEntity<?> getCustomerUsedVouchers(@PathVariable Integer id) {
        return ResponseEntity.ok(commonService.getCustomerUsedVouchers(id));
    }
}