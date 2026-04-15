package com.example.bee.controllers.api.staff;

import com.example.bee.entities.staff.ChucVu;
import com.example.bee.repositories.staff.ChucVuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chuc-vu")
@RequiredArgsConstructor
public class ChucVuApi {

    private final ChucVuRepository chucVuRepo;

    @GetMapping
    public ResponseEntity<?> getDanhSach(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String q) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ChucVu> result = chucVuRepo.searchChucVu(q.trim(), pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getChiTiet(@PathVariable Integer id) {
        return chucVuRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createChucVu(@RequestBody ChucVu request) {
        if (request.getTen() == null || request.getTen().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên chức vụ không được để trống!"));
        }

        String maCV = request.getMa();
        if (maCV == null || maCV.trim().isEmpty()) {
            maCV = "CV" + System.currentTimeMillis();
        } else {
            maCV = maCV.trim().toUpperCase();
            if (chucVuRepo.existsByMa(maCV)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mã chức vụ đã tồn tại!"));
            }
        }

        ChucVu newCv = ChucVu.builder()
                .ma(maCV)
                .ten(request.getTen().trim())
                .moTa(request.getMoTa() != null ? request.getMoTa().trim() : "")
                .build();

        chucVuRepo.save(newCv);
        return ResponseEntity.ok(Map.of("message", "Tạo chức vụ thành công!"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateChucVu(@PathVariable Integer id, @RequestBody ChucVu request) {
        ChucVu existingCv = chucVuRepo.findById(id).orElse(null);
        if (existingCv == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy chức vụ!"));
        }

        if (request.getTen() == null || request.getTen().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên chức vụ không được để trống!"));
        }

        String maCV = request.getMa();
        if (maCV == null || maCV.trim().isEmpty()) {
            maCV = existingCv.getMa();
        } else {
            maCV = maCV.trim().toUpperCase();
            if (chucVuRepo.existsByMaAndIdNot(maCV, id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mã chức vụ đã bị trùng với chức vụ khác!"));
            }
        }

        existingCv.setMa(maCV);
        existingCv.setTen(request.getTen().trim());
        existingCv.setMoTa(request.getMoTa() != null ? request.getMoTa().trim() : "");

        chucVuRepo.save(existingCv);
        return ResponseEntity.ok(Map.of("message", "Cập nhật chức vụ thành công!"));
    }
}