package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.KichThuoc;
import com.example.bee.repositories.catalog.KichThuocRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Random;

@RestController
@RequestMapping("/api/kich-thuoc")
@RequiredArgsConstructor
public class KichThuocApi {
    private final KichThuocRepository repo;

    // --- GEN MÃ SIZE (KT_ + 6 số) ---
    private String generateMa() {
        String ma;
        Random random = new Random();
        do {
            int randomNum = 1000 + random.nextInt(9000);
            ma = "KT" + randomNum;
        } while (repo.existsByMaIgnoreCase(ma));
        return ma;
    }

    @GetMapping
    public Page<KichThuoc> list(@RequestParam(required = false) String q,
                                @RequestParam(required = false) Boolean trangThai,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return repo.search(q, trangThai, pageable);
    }

    @GetMapping("/{id}")
    public KichThuoc getDetail(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu!"));
    }

    @PostMapping
    public ResponseEntity<KichThuoc> create(@Valid @RequestBody KichThuoc body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty())
                ? generateMa()
                : body.getMa().trim().toUpperCase();

        if (ma.length() > 20 || !ma.matches("^[A-Z0-9_]*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã max 20, cho phép dấu '_'");
        }

        if (ten.isEmpty() || ten.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên size max 100!");

        if (repo.existsByTenIgnoreCase(ten)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Size này có rồi!");
        if (repo.existsByMaIgnoreCase(ma)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã size bị trùng!");

        KichThuoc entity = new KichThuoc();
        entity.setMa(ma);
        entity.setTen(ten);
        // Đã xóa ngày tạo, ngày sửa
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);

        return ResponseEntity.ok(repo.save(entity));
    }

    @PutMapping("/{id}")
    public KichThuoc update(@PathVariable Integer id, @Valid @RequestBody KichThuoc body) {
        KichThuoc entity = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";

        if (newTen.isEmpty() || newTen.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên size max 100!");

        if (!entity.getTen().equalsIgnoreCase(newTen) && repo.existsByTenIgnoreCase(newTen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trùng tên size!");
        }

        entity.setTen(newTen);
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : entity.getTrangThai());
        // Đã xóa ngày sửa, người sửa

        return repo.save(entity);
    }
}