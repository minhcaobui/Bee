package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.entities.catalog.MauSac;
import com.example.bee.repositories.catalog.MauSacRepository;
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
import java.security.SecureRandom;

@RestController
@RequestMapping("/api/mau-sac")
@RequiredArgsConstructor
public class MauSacApi {
    private final MauSacRepository repo;
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

    @GetMapping
    public Page<MauSac> list(@RequestParam(required = false) String q, @RequestParam(required = false) Boolean trangThai,
                             @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return repo.search(q, trangThai, PageRequest.of(page, size, Sort.by("id").descending()));
    }

    @GetMapping("/{id}")
    public MauSac getDetail(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu!"));
    }

    @PostMapping
    public ResponseEntity<MauSac> create(@Valid @RequestBody MauSac body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty()) ? generateMa() : body.getMa().trim().toUpperCase();

        if (ma.length() > 20 || !ma.matches("^[A-Z0-9]*$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã max 20!");
        if (ten.isEmpty() || ten.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên màu max 100!");
        if (repo.existsByTenIgnoreCase(ten)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Màu này có rồi!");

        MauSac entity = new MauSac();
        entity.setMa(ma);
        entity.setTen(ten);
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        return ResponseEntity.ok(repo.save(entity));
    }

    @PutMapping("/{id}")
    public MauSac update(@PathVariable Integer id, @Valid @RequestBody MauSac body) {
        MauSac entity = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";

        if (newTen.isEmpty() || newTen.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên màu max 100!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && repo.existsByTenIgnoreCase(newTen)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Trùng tên màu!");

        entity.setTen(newTen);
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : entity.getTrangThai());
        return repo.save(entity);
    }
}