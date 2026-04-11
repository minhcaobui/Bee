package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.KichThuoc;
import com.example.bee.services.KichThuocService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kich-thuoc")
@RequiredArgsConstructor
public class KichThuocApi {

    private final KichThuocService kichThuocService;

    @GetMapping
    public Page<KichThuoc> layDanhSach(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) Boolean trangThai,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return kichThuocService.layDanhSach(q, trangThai, page, size);
    }

    @GetMapping("/hoat-dong")
    public ResponseEntity<List<KichThuoc>> layTatCaHoatDong() {
        return ResponseEntity.ok(kichThuocService.layTatCaHoatDong());
    }

    @GetMapping("/{id}")
    public KichThuoc layChiTiet(@PathVariable Integer id) {
        return kichThuocService.layChiTiet(id);
    }

    @PostMapping
    public ResponseEntity<KichThuoc> taoMoi(@Valid @RequestBody KichThuoc body) {
        return ResponseEntity.ok(kichThuocService.taoMoi(body));
    }

    @PutMapping("/{id}")
    public KichThuoc capNhat(@PathVariable Integer id, @Valid @RequestBody KichThuoc body) {
        return kichThuocService.capNhat(id, body);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable Integer id) {
        return kichThuocService.doiTrangThai(id);
    }
}