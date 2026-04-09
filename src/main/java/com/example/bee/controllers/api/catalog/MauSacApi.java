package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.MauSac;
import com.example.bee.repositories.catalog.MauSacRepository;
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
@RequestMapping("/api/mau-sac")
@RequiredArgsConstructor
public class MauSacApi {

    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final MauSacRepository mauSacRepository;

    private String generateMa() {
        return "MS" + System.currentTimeMillis(); // Đổi "CL" thành "HANG", "KT", "MS" tương ứng với từng file
    }

    @GetMapping
    public Page<MauSac> list(@RequestParam(required = false) String q,
                             @RequestParam(required = false) Boolean trangThai,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return mauSacRepository.search(q, trangThai, pageable);
    }

    @GetMapping("/all-active")
    public ResponseEntity<List<MauSac>> getAllActive() {
        return ResponseEntity.ok(mauSacRepository.findByTrangThaiTrue());
    }

    @GetMapping("/{id}")
    public MauSac getDetail(@PathVariable Integer id) {
        return mauSacRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu!"));
    }

    @PostMapping
    public ResponseEntity<MauSac> create(@Valid @RequestBody MauSac body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty())
                ? generateMa()
                : body.getMa().trim().toUpperCase();
        if (ma.length() > 20 || !ma.matches("^[A-Z0-9_]*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã max 20, cho phép dấu '_'");
        }
        if (ten.isEmpty() || ten.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên màu max 100!");
        if (mauSacRepository.existsByTenIgnoreCase(ten))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Màu này có rồi!");
        if (mauSacRepository.existsByMaIgnoreCase(ma))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã màu bị trùng!");
        MauSac entity = new MauSac();
        entity.setMa(ma);
        entity.setTen(ten);
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        return ResponseEntity.ok(mauSacRepository.save(entity));
    }

    @PutMapping("/{id}")
    public MauSac update(@PathVariable Integer id, @Valid @RequestBody MauSac body) {
        MauSac entity = mauSacRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";
        if (newTen.isEmpty() || newTen.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên màu max 100!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && mauSacRepository.existsByTenIgnoreCase(newTen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trùng tên màu!");
        }
        Boolean newTrangThai = body.getTrangThai();
        if (newTrangThai != null && !newTrangThai && Boolean.TRUE.equals(entity.getTrangThai())) {
            boolean isUsed = sanPhamChiTietRepository.existsByMauSac_IdAndTrangThaiTrue(id);
            if (isUsed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể ngừng hoạt động! Đang có sản phẩm sử dụng màu sắc này.");
            }
        }

        entity.setTen(newTen);
        entity.setTrangThai(newTrangThai != null ? newTrangThai : entity.getTrangThai());
        return mauSacRepository.save(entity);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id) {
        MauSac mauSac = mauSacRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (mauSac.getTrangThai() != null && mauSac.getTrangThai() == true) {
            boolean isUsed = sanPhamChiTietRepository.existsByMauSac_IdAndTrangThaiTrue(id);
            if (isUsed) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Không thể tắt màu này! Đang có sản phẩm chi tiết sử dụng màu sắc này."
                ));
            }
        }
        mauSac.setTrangThai(!mauSac.getTrangThai());
        mauSacRepository.save(mauSac);
        return ResponseEntity.ok().build();
    }
}