package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.Hang;
import com.example.bee.repositories.catalog.HangRepository;
import com.example.bee.repositories.products.SanPhamRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/hang")
@RequiredArgsConstructor
public class HangApi {

    private final HangRepository hangRepository;
    private final SanPhamRepository sanPhamRepository;

    private String generateMa() {
        String ma;
        Random random = new Random();
        do {
            int randomNum = 1000 + random.nextInt(9000);
            ma = "HANG" + randomNum;
        } while (hangRepository.existsByMaIgnoreCase(ma));
        return ma;
    }

    @GetMapping
    public Page<Hang> list(@RequestParam(required = false) String q,
                           @RequestParam(required = false) Boolean trangThai,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return hangRepository.search(q, trangThai, pageable);
    }

    @GetMapping("/all-active")
    public ResponseEntity<List<Hang>> getAllActive() {
        return ResponseEntity.ok(hangRepository.findByTrangThaiTrue());
    }

    @GetMapping("/{id}")
    public Hang getById(@PathVariable Integer id) {
        return hangRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Hang> create(@Valid @RequestBody Hang body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty()) ? generateMa() : body.getMa().trim().toUpperCase();
        if (ma.length() > 20 || !ma.matches("^[A-Z0-9_]*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã max 20, không dấu tiếng Việt, cho phép dấu gạch dưới!");
        }
        if (ten.isEmpty() || ten.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên max 100 chữ!");
        if (hangRepository.existsByTenIgnoreCase(ten))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên hãng này có rồi!");
        if (hangRepository.existsByMaIgnoreCase(ma))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã này bị trùng!");
        Hang entity = new Hang();
        entity.setMa(ma);
        entity.setTen(ten);
        entity.setMoTa(body.getMoTa());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());
        return ResponseEntity.ok(hangRepository.save(entity));
    }

    @PutMapping("/{id}")
    public Hang update(@PathVariable Integer id, @Valid @RequestBody Hang body) {
        Hang entity = hangRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";
        if (newTen.isEmpty() || newTen.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên max 100 chữ!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && hangRepository.existsByTenIgnoreCase(newTen))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên hãng trùng rồi!");
        entity.setTen(newTen);
        entity.setMoTa(body.getMoTa());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : entity.getTrangThai());
        entity.setNgaySua(LocalDateTime.now());
        return hangRepository.save(entity);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id) {
        Hang hang = hangRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (hang.getTrangThai() != null && hang.getTrangThai() == true) {
            boolean isUsed = sanPhamRepository.existsByHang_IdAndTrangThaiTrue(id);
            if (isUsed) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Không thể ngừng hoạt động! Đang có sản phẩm thuộc hãng này đang được bày bán."
                ));
            }
        }
        hang.setTrangThai(!hang.getTrangThai());
        hangRepository.save(hang);
        return ResponseEntity.ok().build();
    }
}