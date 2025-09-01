package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.KichThuoc;
import com.example.bee.entities.catalog.MauSac;
import com.example.bee.repositories.catalog.KichThuocRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("api/kich-thuoc")
@RequiredArgsConstructor
public class KichThuocApi {

    private final KichThuocRepository repo;

    // ===== GET: danh sách phân trang (mặc định 5) =====
    @GetMapping
    public Page<KichThuoc> list(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "5") int size) {
        size = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return repo.findAll(pageable);
    }

    @GetMapping("{id}")
    public KichThuoc get(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kích thước"));
    }

    @GetMapping("{ma}")
    public KichThuoc get(@PathVariable String ma) {
        return repo.findByMa(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kích thước"));
    }

    // ===== POST: tạo mới (tự sinh mã nếu trống) =====
    @PostMapping
    public ResponseEntity<KichThuoc> create(
            @RequestBody KichThuoc body,
            UriComponentsBuilder uriBuilder) {

        body.setId(null);

        body.setMa(body.getMa().trim());
        if (repo.existsByMaIgnoreCase(body.getMa())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã kích thước đã tồn tại");
        }

        body.setTen(safeTrim(body.getTen()));

        try {
            KichThuoc saved = repo.save(body);
            URI location = uriBuilder.path("/api/kich-thuoc/{id}")
                    .buildAndExpand(saved.getId()).toUri();
            return ResponseEntity.created(location).body(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã kích thước bị trùng, vui lòng thử lại", e);
        }
    }

    // ===== PUT: cập nhật =====
    @PutMapping("{id}")
    public KichThuoc update(@PathVariable Integer id,
                         @Valid @RequestBody MauSac body) {

        KichThuoc e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kích thước"));

        String newMa = isBlank(body.getMa()) ? e.getMa() : body.getMa().trim();
        if (repo.existsByMaIgnoreCaseAndIdNot(newMa, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã kích thước đã tồn tại");
        }

        e.setMa(newMa);
        e.setTen(safeTrimOrDefault(body.getTen(), e.getTen()));

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
}
