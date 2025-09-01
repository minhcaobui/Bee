package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.repositories.catalog.ChatLieuRepository;
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
import java.util.UUID;

@RestController
@RequestMapping("api/chat-lieu")
@RequiredArgsConstructor
public class ChatLieuApi {

    private final ChatLieuRepository repo;

    // ===== GET: danh sách phân trang (mặc định 5) =====
    @GetMapping
    public Page<ChatLieu> list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "5") int size) {
        size = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return repo.findAll(pageable);
    }

    @GetMapping("{id}")
    public ChatLieu get(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chất liệu"));
    }

    @GetMapping("{ma}")
    public ChatLieu get(@PathVariable String ma) {
        return repo.findByMa(ma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chất liệu"));
    }

    // ===== POST: tạo mới (tự sinh mã nếu trống) =====
    @PostMapping
    public ResponseEntity<ChatLieu> create(
            @RequestBody ChatLieu body,
            UriComponentsBuilder uriBuilder) {

        body.setId(null);

        if (isBlank(body.getMa())) {
            body.setMa(nextAutoCode());
        } else {
            body.setMa(body.getMa().trim());
        }

        if (repo.existsByMaIgnoreCase(body.getMa())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã chất liệu đã tồn tại");
        }

        body.setTen(safeTrim(body.getTen()));
        if (body.getMoTa() == null) body.setMoTa("Không có mô tả!");
        if (body.getTrangThai() == null) body.setTrangThai(true);

        try {
            ChatLieu saved = repo.save(body);
            URI location = uriBuilder.path("/api/chat-lieu/{id}")
                    .buildAndExpand(saved.getId()).toUri();
            return ResponseEntity.created(location).body(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã chất liệu bị trùng, vui lòng thử lại", e);
        }
    }

    // ===== PUT: cập nhật =====
    @PutMapping("{id}")
    public ChatLieu update(@PathVariable Integer id,
                           @Valid @RequestBody ChatLieu body) {

        ChatLieu e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chất liệu"));

        String newMa = isBlank(body.getMa()) ? e.getMa() : body.getMa().trim();
        if (repo.existsByMaIgnoreCaseAndIdNot(newMa, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã chất liệu đã tồn tại");
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
        return "M_" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }
}
