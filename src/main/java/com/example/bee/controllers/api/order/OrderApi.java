package com.example.bee.controllers.api.order;

import com.example.bee.dtos.request.CheckoutRequest;
import com.example.bee.entities.order.HoaDon;
import com.example.bee.services.OrderService;
import com.example.bee.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/hoa-don")
@RequiredArgsConstructor
public class OrderApi {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @GetMapping("/don-hang")
    public ResponseEntity<Page<HoaDon>> getDonHangChoXuLy(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) Integer loaiHoaDon,
            @RequestParam(required = false) String phuongThucThanhToan,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (endDate != null) {
            Calendar cal = Calendar.getInstance(); cal.setTime(endDate);
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            endDate = cal.getTime();
        }
        return ResponseEntity.ok(orderService.searchDonHangChoXuLy((q != null && !q.trim().isEmpty()) ? q.trim() : null, statusId, loaiHoaDon, (phuongThucThanhToan != null && !phuongThucThanhToan.trim().isEmpty()) ? phuongThucThanhToan : null, startDate, endDate, PageRequest.of(page, size)));
    }

    @GetMapping("/lich-su")
    public ResponseEntity<Page<HoaDon>> getLichSu(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) Integer nhanVienId,
            @RequestParam(required = false) Integer loaiHoaDon,
            @RequestParam(required = false) String phuongThucThanhToan,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (endDate != null) {
            Calendar cal = Calendar.getInstance(); cal.setTime(endDate);
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            endDate = cal.getTime();
        }
        return ResponseEntity.ok(orderService.searchLichSuHoaDon((q != null && !q.trim().isEmpty()) ? q.trim() : null, statusId, nhanVienId, loaiHoaDon, (phuongThucThanhToan != null && !phuongThucThanhToan.trim().isEmpty()) ? phuongThucThanhToan : null, startDate, endDate, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(orderService.getDetail(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/next-status")
    public ResponseEntity<?> nextStatus(@PathVariable Integer id, @RequestBody Map<String, String> req) {
        try {
            return ResponseEntity.ok(orderService.nextStatus(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/request-payment")
    public ResponseEntity<?> requestPayment(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(orderService.requestPayment(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/huy")
    public ResponseEntity<?> huyDon(@PathVariable Integer id, @RequestBody Map<String, String> req) {
        try {
            return ResponseEntity.ok(orderService.huyDon(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest req) {
        try {
            return ResponseEntity.ok(orderService.checkout(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders() {
        try {
            return ResponseEntity.ok(orderService.getMyOrders());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/tra-cuu/{ma}")
    public ResponseEntity<?> traCuuNhanh(@PathVariable String ma) {
        try {
            return ResponseEntity.ok(orderService.traCuuNhanh(ma));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{id}/print-data")
    public ResponseEntity<?> getInvoicePrintData(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(orderService.getInvoicePrintData(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/thong-bao-moi")
    public ResponseEntity<?> getNewOnlineOrders() {
        return ResponseEntity.ok(orderService.getNewOnlineOrders());
    }

    @PostMapping("/{id}/confirm-transfer")
    public ResponseEntity<?> confirmTransferPayment(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(orderService.confirmTransferPayment(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my-used-vouchers")
    public ResponseEntity<?> getMyUsedVouchers() {
        return ResponseEntity.ok(orderService.getMyUsedVouchers());
    }
}