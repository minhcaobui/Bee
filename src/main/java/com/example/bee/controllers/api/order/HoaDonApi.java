package com.example.bee.controllers.api.order;

import com.example.bee.dtos.request.CheckoutRequest;
import com.example.bee.entities.order.HoaDon;
import com.example.bee.services.HoaDonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/hoa-don")
@RequiredArgsConstructor
public class HoaDonApi {

    private final HoaDonService hoaDonService;

    @GetMapping("/cho-xu-ly")
    public ResponseEntity<Page<HoaDon>> layDonHangChoXuLy(
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
        return ResponseEntity.ok(hoaDonService.timKiemDonHangChoXuLy((q != null && !q.trim().isEmpty()) ? q.trim() : null, statusId, loaiHoaDon, (phuongThucThanhToan != null && !phuongThucThanhToan.trim().isEmpty()) ? phuongThucThanhToan : null, startDate, endDate, PageRequest.of(page, size)));
    }

    @GetMapping("/lich-su")
    public ResponseEntity<Page<HoaDon>> layLichSu(
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
        return ResponseEntity.ok(hoaDonService.timKiemLichSuHoaDon((q != null && !q.trim().isEmpty()) ? q.trim() : null, statusId, nhanVienId, loaiHoaDon, (phuongThucThanhToan != null && !phuongThucThanhToan.trim().isEmpty()) ? phuongThucThanhToan : null, startDate, endDate, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTiet(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(hoaDonService.layChiTietHoaDon(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/trang-thai-tiep-theo")
    public ResponseEntity<?> chuyenTrangThaiTiepTheo(@PathVariable Integer id, @RequestBody Map<String, String> req) {
        try {
            return ResponseEntity.ok(hoaDonService.chuyenTrangThaiTiepTheo(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/yeu-cau-thanh-toan")
    public ResponseEntity<?> yeuCauThanhToan(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(hoaDonService.yeuCauThanhToanLai(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/huy-don")
    public ResponseEntity<?> huyDon(@PathVariable Integer id, @RequestBody Map<String, String> req) {
        try {
            return ResponseEntity.ok(hoaDonService.huyDonHang(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/thanh-toan")
    public ResponseEntity<?> thanhToan(@RequestBody CheckoutRequest req) {
        try {
            return ResponseEntity.ok(hoaDonService.thanhToanDonHang(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/cua-toi")
    public ResponseEntity<?> layDonHangCuaToi() {
        try {
            return ResponseEntity.ok(hoaDonService.layDanhSachDonHangCuaToi());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/tra-cuu/{ma}")
    public ResponseEntity<?> traCuuNhanh(@PathVariable String ma) {
        try {
            return ResponseEntity.ok(hoaDonService.traCuuNhanhHoaDon(ma));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{id}/du-lieu-in")
    public ResponseEntity<?> layDuLieuIn(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(hoaDonService.layDuLieuInHoaDon(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/thong-bao-moi")
    public ResponseEntity<?> layThongBaoMoi() {
        return ResponseEntity.ok(hoaDonService.layDonHangOnlineMoi());
    }

    @PostMapping("/{id}/xac-nhan-chuyen-khoan")
    public ResponseEntity<?> xacNhanChuyenKhoan(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(hoaDonService.xacNhanDaNhanChuyenKhoan(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/voucher-da-dung")
    public ResponseEntity<?> layVoucherDaDung() {
        return ResponseEntity.ok(hoaDonService.layDanhSachVoucherKhachDaDung());
    }
}