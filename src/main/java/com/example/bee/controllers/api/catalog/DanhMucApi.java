package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.DanhMuc;
import com.example.bee.repositories.catalog.DanhMucRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("api/danh-muc")
@RequiredArgsConstructor
public class DanhMucApi {

    private final DanhMucRepository repo;

    // ===== GET: danh sách phân trang (mặc định 5) =====
    @GetMapping
    public Page<DanhMuc> list(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "5") int size) {
        size = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return repo.findAll(pageable);
    }

    @GetMapping("{id}")
    public DanhMuc get(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));
    }

    @GetMapping("{ma}")
    public DanhMuc get(@PathVariable String ma) {
        return repo.findByMa(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));
    }

    // ===== POST: tạo mới (tự sinh mã nếu trống) =====
    @PostMapping
    public ResponseEntity<DanhMuc> create(
            @RequestBody DanhMuc body,
            UriComponentsBuilder uriBuilder) {

        body.setId(null);

        if (isBlank(body.getMa())) {
            body.setMa(nextAutoCode());
        } else {
            body.setMa(body.getMa().trim());
        }

        if (repo.existsByMaIgnoreCase(body.getMa())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã danh mục đã tồn tại");
        }

        body.setTen(safeTrim(body.getTen()));
        if (body.getMoTa() == null) body.setMoTa("Không có mô tả!");
        if (body.getTrangThai() == null) body.setTrangThai(true);

        try {
            DanhMuc saved = repo.save(body);
            URI location = uriBuilder.path("/api/danh-muc/{id}")
                    .buildAndExpand(saved.getId()).toUri();
            return ResponseEntity.created(location).body(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã danh mục bị trùng, vui lòng thử lại", e);
        }
    }

    // ===== PUT: cập nhật =====
    @PutMapping("{id}")
    public DanhMuc update(@PathVariable Integer id,
                           @Valid @RequestBody DanhMuc body) {

        DanhMuc e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));

        String newMa = isBlank(body.getMa()) ? e.getMa() : body.getMa().trim();
        if (repo.existsByMaIgnoreCaseAndIdNot(newMa, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã danh mục đã tồn tại");
        }

        e.setMa(newMa);
        e.setTen(safeTrimOrDefault(body.getTen(), e.getTen()));
        e.setMoTa(body.getMoTa());
        if (body.getTrangThai() != null) e.setTrangThai(body.getTrangThai());

        return repo.save(e);
    }

    // ===== Helpers =====
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
    private static String safeTrimOrDefault(String s, String def) {
        return s == null ? def : s.trim();
    }

    // Sinh mã ngắn, xác suất trùng rất thấp (8 ký tự hex)
    private String nextAutoCode() {
        return "C_" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }
}
