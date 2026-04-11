package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.DanhMuc;
import com.example.bee.services.DanhMucService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/danh-muc")
@RequiredArgsConstructor
public class DanhMucApi {

    private final DanhMucService danhMucService;

    @GetMapping
    public Page<DanhMuc> layDanhSach(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) Boolean trangThai,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return danhMucService.layDanhSach(q, trangThai, page, size);
    }

    @GetMapping("/hoat-dong")
    public ResponseEntity<List<DanhMuc>> layTatCaHoatDong() {
        return ResponseEntity.ok(danhMucService.layTatCaHoatDong());
    }

    @GetMapping("/{id}")
    public DanhMuc layChiTiet(@PathVariable Integer id) {
        return danhMucService.layChiTiet(id);
    }

    @PostMapping
    public ResponseEntity<?> taoMoi(@Valid @RequestBody DanhMuc body) {
        return ResponseEntity.ok(danhMucService.taoMoi(body));
    }

    @PutMapping("/{id}")
    public DanhMuc capNhat(@PathVariable Integer id, @Valid @RequestBody DanhMuc body) {
        return danhMucService.capNhat(id, body);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable Integer id) {
        return danhMucService.doiTrangThai(id);
    }
}