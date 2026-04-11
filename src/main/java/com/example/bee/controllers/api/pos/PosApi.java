package com.example.bee.controllers.api.pos;

import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.services.PosService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosApi {

    private final PosService posService;

    @GetMapping("/products/search")
    public ResponseEntity<List<SanPhamChiTiet>> searchProducts(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Integer color,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(posService.searchProducts(q, color, size));
    }

    @PostMapping("/invoices/update-hold")
    public ResponseEntity<?> updateHoldStock(@RequestBody Map<String, Object> body) {
        try {
            Integer spctId = (Integer) body.get("spctId");
            Integer delta = (Integer) body.get("delta");
            posService.updateHoldStock(spctId, delta);
            return ResponseEntity.ok(Map.of("message", "OK"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/finish")
    public ResponseEntity<?> finishOrder(@RequestBody Map<String, Object> payload) {
        try {
            return ResponseEntity.ok(posService.finishOrder(payload));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-online-payment")
    public ResponseEntity<?> confirmOnlinePayment(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(posService.confirmOnlinePayment(body.get("maHD")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/vouchers/apply")
    public ResponseEntity<?> applyVoucher(@RequestBody Map<String, Object> payload) {
        try {
            return ResponseEntity.ok(posService.applyVoucher(payload));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/customers/search")
    public ResponseEntity<List<KhachHang>> searchCustomers(@RequestParam String q) {
        return ResponseEntity.ok(posService.searchCustomers(q));
    }

    @PostMapping("/customers")
    public ResponseEntity<?> createCustomer(@RequestBody KhachHang kh) {
        try {
            return ResponseEntity.ok(posService.createCustomer(kh));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/attributes")
    public ResponseEntity<Map<String, Object>> getAttributes() {
        return ResponseEntity.ok(posService.getAttributes());
    }

    @GetMapping("/{id}/print-data")
    public ResponseEntity<?> getInvoicePrintData(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(posService.getInvoicePrintData(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/customers/{id}/used-vouchers")
    public ResponseEntity<?> getCustomerUsedVouchers(@PathVariable Integer id) {
        return ResponseEntity.ok(posService.getCustomerUsedVouchers(id));
    }
}