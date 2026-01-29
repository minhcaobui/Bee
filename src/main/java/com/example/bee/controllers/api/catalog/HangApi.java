package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.Hang;
import com.example.bee.repositories.catalog.HangRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/hang")
@RequiredArgsConstructor
public class HangApi {
    private final HangRepository repo;
    private static final String MA_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RAND = new SecureRandom();

    private String generateMa() {
        String ma;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(MA_CHARS.charAt(RAND.nextInt(MA_CHARS.length())));
            ma = sb.toString();
        } while (repo.existsByMaIgnoreCase(ma));
        return ma;
    }

    // --- TRẢ LẠI PHÂN TRANG CHO MÀY ĐÂY ---
    @GetMapping
    public Page<Hang> list(@RequestParam(required = false) String q,
                           @RequestParam(required = false) Boolean trangThai,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return repo.search(q, trangThai, pageable);
    }

    @GetMapping("/{id}")
    public Hang getById(@PathVariable Integer id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Hang> create(@Valid @RequestBody Hang body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty()) ? generateMa() : body.getMa().trim().toUpperCase();

        if (ma.length() > 20 || !ma.matches("^[A-Z0-9]*$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã max 20, không tiếng Việt!");
        if (ten.isEmpty() || ten.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên max 100 chữ!");
        if (repo.existsByTenIgnoreCase(ten)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên hãng này có rồi!");
        if (repo.existsByMaIgnoreCase(ma)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã này bị trùng!");

        Hang entity = new Hang();
        entity.setMa(ma);
        entity.setTen(ten);
        entity.setMoTa(body.getMoTa());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());
        return ResponseEntity.ok(repo.save(entity));
    }

    @PutMapping("/{id}")
    public Hang update(@PathVariable Integer id, @Valid @RequestBody Hang body) {
        Hang entity = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";

        if (newTen.isEmpty() || newTen.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên max 100 chữ!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && repo.existsByTenIgnoreCase(newTen)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên hãng trùng rồi!");

        entity.setTen(newTen);
        entity.setMoTa(body.getMoTa());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : entity.getTrangThai());
        entity.setNgaySua(LocalDateTime.now());
        return repo.save(entity);
    }
}