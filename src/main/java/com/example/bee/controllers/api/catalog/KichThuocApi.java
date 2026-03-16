package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.KichThuoc;
import com.example.bee.repositories.catalog.KichThuocRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/kich-thuoc")
@RequiredArgsConstructor
public class KichThuocApi {

    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final KichThuocRepository kichThuocRepository;

    private String generateMa() {
        String ma;
        Random random = new Random();
        do {
            int randomNum = 1000 + random.nextInt(9000);
            ma = "KT" + randomNum;
        } while (kichThuocRepository.existsByMaIgnoreCase(ma));
        return ma;
    }

    @GetMapping
    public Page<KichThuoc> list(@RequestParam(required = false) String q,
                                @RequestParam(required = false) Boolean trangThai,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return kichThuocRepository.search(q, trangThai, pageable);
    }

    @GetMapping("/all-active")
    public ResponseEntity<List<KichThuoc>> getAllActive() {
        return ResponseEntity.ok(kichThuocRepository.findByTrangThaiTrue());
    }

    @GetMapping("/{id}")
    public KichThuoc getDetail(@PathVariable Integer id) {
        return kichThuocRepository.findById(id)
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
        if (ten.isEmpty() || ten.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên size max 100!");
        if (kichThuocRepository.existsByTenIgnoreCase(ten))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Size này có rồi!");
        if (kichThuocRepository.existsByMaIgnoreCase(ma))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã size bị trùng!");
        KichThuoc entity = new KichThuoc();
        entity.setMa(ma);
        entity.setTen(ten);
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        return ResponseEntity.ok(kichThuocRepository.save(entity));
    }

    @PutMapping("/{id}")
    public KichThuoc update(@PathVariable Integer id, @Valid @RequestBody KichThuoc body) {
        KichThuoc entity = kichThuocRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";
        if (newTen.isEmpty() || newTen.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên size max 100!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && kichThuocRepository.existsByTenIgnoreCase(newTen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trùng tên size!");
        }
        entity.setTen(newTen);
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : entity.getTrangThai());
        return kichThuocRepository.save(entity);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id) {
        KichThuoc kichThuoc = kichThuocRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (kichThuoc.getTrangThai() != null && kichThuoc.getTrangThai() == true) {
            boolean isUsed = sanPhamChiTietRepository.existsByKichThuoc_IdAndTrangThaiTrue(id);
            if (isUsed) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Không thể tắt size này! Đang có sản phẩm chi tiết sử dụng kích thước này."
                ));
            }
        }
        kichThuoc.setTrangThai(!kichThuoc.getTrangThai());
        kichThuocRepository.save(kichThuoc);
        return ResponseEntity.ok().build();
    }
}