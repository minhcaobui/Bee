package com.example.bee.controllers.api.product;

import com.example.bee.dtos.ReviewAdminResponse;
import com.example.bee.services.DanhGiaSanPhamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quan-ly-danh-gia")
@RequiredArgsConstructor
public class DanhGiaSanPhamApi {

    private final DanhGiaSanPhamService danhGiaSanPhamService;

    @GetMapping
    public ResponseEntity<Page<ReviewAdminResponse>> layDanhSach(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer soSao,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(danhGiaSanPhamService.layDanhSachDanhGia(q, soSao, trangThai, sort, page, size));
    }

    @PostMapping("/{id}/phan-hoi")
    public ResponseEntity<?> phanHoiDanhGia(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return danhGiaSanPhamService.phanHoiDanhGia(id, payload);
    }
}