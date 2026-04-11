package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.Hang;
import com.example.bee.services.HangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hang")
@RequiredArgsConstructor
public class HangApi {

    private final HangService hangService;

    @GetMapping
    public Page<Hang> layDanhSach(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) Boolean trangThai,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return hangService.layDanhSach(q, trangThai, page, size);
    }

    @GetMapping("/hoat-dong")
    public ResponseEntity<List<Hang>> layTatCaHoatDong() {
        return ResponseEntity.ok(hangService.layTatCaHoatDong());
    }

    @GetMapping("/{id}")
    public Hang layChiTiet(@PathVariable Integer id) {
        return hangService.layChiTiet(id);
    }

    @PostMapping
    public ResponseEntity<Hang> taoMoi(@Valid @RequestBody Hang body) {
        return ResponseEntity.ok(hangService.taoMoi(body));
    }

    @PutMapping("/{id}")
    public Hang capNhat(@PathVariable Integer id, @Valid @RequestBody Hang body) {
        return hangService.capNhat(id, body);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable Integer id) {
        return hangService.doiTrangThai(id);
    }
}