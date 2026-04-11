package com.example.bee.controllers.api.staff;

import com.example.bee.entities.staff.ChucVu;
import com.example.bee.services.ChucVuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chuc-vu")
@RequiredArgsConstructor
public class ChucVuApi {

    private final ChucVuService chucVuService;

    @GetMapping
    public ResponseEntity<?> layDanhSach(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(chucVuService.layDanhSach(page, size, q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTiet(@PathVariable Integer id) {
        return chucVuService.layChiTiet(id);
    }

    @PostMapping
    public ResponseEntity<?> taoMoi(@RequestBody ChucVu request) {
        return chucVuService.taoMoi(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(@PathVariable Integer id, @RequestBody ChucVu request) {
        return chucVuService.capNhat(id, request);
    }
}