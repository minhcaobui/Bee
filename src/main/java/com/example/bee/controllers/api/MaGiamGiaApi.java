package com.example.bee.controllers.api.promotion;

import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.services.MaGiamGiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;

@RestController
@RequestMapping("/api/ma-giam-gia")
@RequiredArgsConstructor
public class MaGiamGiaApi {

    private final MaGiamGiaService maGiamGiaService;

    @GetMapping
    public ResponseEntity<?> layDanhSach(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(maGiamGiaService.getAll(q, trangThai, from, to, PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTiet(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(maGiamGiaService.getDetail(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> taoMoi(@RequestBody MaGiamGia body) {
        try {
            return ResponseEntity.ok(maGiamGiaService.create(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(@PathVariable Integer id, @RequestBody MaGiamGia body) {
        try {
            return ResponseEntity.ok(maGiamGiaService.update(id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThaiNhanh(@PathVariable Integer id) {
        try {
            String message = maGiamGiaService.quickToggle(id);
            return ResponseEntity.ok(Collections.singletonMap("message", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @GetMapping("/hoat-dong")
    public ResponseEntity<?> layMaGiamGiaHoatDong() {
        return ResponseEntity.ok(maGiamGiaService.getActiveVouchers());
    }
}