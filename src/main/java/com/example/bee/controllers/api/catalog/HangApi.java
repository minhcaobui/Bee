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

import java.time.LocalDateTime;
import java.util.Random;

@RestController
@RequestMapping("/api/hang")
@RequiredArgsConstructor
public class HangApi {
    private final HangRepository repo;

    private String generateMa() {
        String ma;
        Random random = new Random();
        do {
            int randomNum = 1000 + random.nextInt(9000);
            ma = "HANG" + randomNum;
        } while (repo.existsByMaIgnoreCase(ma));
        return ma;
    }

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
        // Nếu không truyền mã thì tự sinh
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty()) ? generateMa() : body.getMa().trim().toUpperCase();

        // --- 2. SỬA VALIDATE ĐỂ CHẤP NHẬN DẤU GẠCH DƯỚI (_) ---
        // Regex cũ: "^[A-Z0-9]*$" (chỉ chữ số) -> Thêm dấu _ vào thành "^[A-Z0-9_]*$"
        if (ma.length() > 20 || !ma.matches("^[A-Z0-9_]*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã max 20, không dấu tiếng Việt, cho phép dấu gạch dưới!");
        }

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
        entity.setNguoiSua(1);
        return repo.save(entity);
    }
}