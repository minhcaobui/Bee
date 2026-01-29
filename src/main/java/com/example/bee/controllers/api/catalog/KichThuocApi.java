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
import java.security.SecureRandom;

@RestController
@RequestMapping("/api/kich-thuoc")
@RequiredArgsConstructor
public class KichThuocApi {
    private final KichThuocRepository repo;
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
    public Page<KichThuoc> list(@RequestParam(required = false) String q, @RequestParam(required = false) Boolean trangThai,
                                @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return repo.search(q, trangThai, PageRequest.of(page, size, Sort.by("id").descending()));
    }

    @GetMapping("/{id}")
    public KichThuoc getDetail(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu!"));
    }

    @PostMapping
    public ResponseEntity<KichThuoc> create(@Valid @RequestBody KichThuoc body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty()) ? generateMa() : body.getMa().trim().toUpperCase();

        if (ma.length() > 20 || !ma.matches("^[A-Z0-9]*$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã max 20!");
        if (ten.isEmpty() || ten.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên size max 100!");
        if (repo.existsByTenIgnoreCase(ten)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Size này có rồi!");

        KichThuoc entity = new KichThuoc();
        entity.setMa(ma);
        entity.setTen(ten);
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        return ResponseEntity.ok(repo.save(entity));
    }

    @PutMapping("/{id}")
    public KichThuoc update(@PathVariable Integer id, @Valid @RequestBody KichThuoc body) {
        KichThuoc entity = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";

        if (newTen.isEmpty() || newTen.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên size max 100!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && repo.existsByTenIgnoreCase(newTen)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Trùng tên size!");

        entity.setTen(newTen);
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : entity.getTrangThai());
        return repo.save(entity);
    }
}
