package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.MauSac;
import com.example.bee.services.MauSacService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mau-sac")
@RequiredArgsConstructor
public class MauSacApi {

    private final MauSacService mauSacService;

    @GetMapping
    public Page<MauSac> layDanhSach(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Boolean trangThai,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return mauSacService.layDanhSach(q, trangThai, page, size);
    }

    @GetMapping("/hoat-dong")
    public ResponseEntity<List<MauSac>> layTatCaHoatDong() {
        return ResponseEntity.ok(mauSacService.layTatCaHoatDong());
    }

    @GetMapping("/{id}")
    public MauSac layChiTiet(@PathVariable Integer id) {
        return mauSacService.layChiTiet(id);
    }

    @PostMapping
    public ResponseEntity<MauSac> taoMoi(@Valid @RequestBody MauSac body) {
        return ResponseEntity.ok(mauSacService.taoMoi(body));
    }

    @PutMapping("/{id}")
    public MauSac capNhat(@PathVariable Integer id, @Valid @RequestBody MauSac body) {
        return mauSacService.capNhat(id, body);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable Integer id) {
        return mauSacService.doiTrangThai(id);
    }
}