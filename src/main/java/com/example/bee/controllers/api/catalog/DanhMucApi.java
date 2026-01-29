package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.DanhMuc;
import com.example.bee.repositories.catalog.DanhMucRepository;
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
@RequestMapping("/api/danh-muc")
@RequiredArgsConstructor
public class DanhMucApi {
    private final DanhMucRepository danhMucRepository;
    private static final String MA_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RAND = new SecureRandom();

    private String generateMa() {
        String ma;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(MA_CHARS.charAt(RAND.nextInt(MA_CHARS.length())));
            ma = sb.toString();
        } while (danhMucRepository.existsByMaIgnoreCase(ma));
        return ma;
    }

    @GetMapping
    public Page<DanhMuc> list(@RequestParam(required = false) String q, @RequestParam(required = false) Boolean trangThai, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return danhMucRepository.search(q, trangThai, pageable);
    }

    @GetMapping("/{id}")
    public DanhMuc getById(@PathVariable Integer id) {
        return danhMucRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DanhMuc body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty())
                ? generateMa()
                : body.getMa().trim().toUpperCase();

        // 1. Check độ dài & Tiếng Việt cho MÃ
        if (ma.length() > 20) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã tối đa 20 ký tự thôi cu!");
        if (!ma.matches("^[A-Z0-9]*$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã đéo được có tiếng Việt hoặc khoảng trắng!");

        // 2. Check độ dài cho TÊN
        if (ten.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên đéo được để trống!");
        if (ten.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên dài quá (max 100), bớt văn vở lại!");

        // 3. Check trùng TÊN & MÃ
        if (danhMucRepository.existsByTenIgnoreCase(ten)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên này có thằng dùng rồi!");
        if (danhMucRepository.existsByMaIgnoreCase(ma)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã này bị trùng rồi!");

        // 4. Save
        DanhMuc entity = new DanhMuc();
        entity.setMa(ma);
        entity.setTen(ten);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim()); // Chỉ dành cho DM, Hãng, Chất liệu
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());

        return ResponseEntity.ok(danhMucRepository.save(entity));
    }

    @PutMapping("/{id}")
    public DanhMuc update(@PathVariable Integer id, @Valid @RequestBody DanhMuc body) {
        DanhMuc entity = danhMucRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String newTen = body.getTen() != null ? body.getTen().trim() : "";

        // 1. Check độ dài TÊN
        if (newTen.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên không được để trống!");
        if (newTen.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tối đa 100 ký tự thôi!");

        // 2. Check trùng TÊN (Trừ chính nó)
        if (!entity.getTen().equalsIgnoreCase(newTen) && danhMucRepository.existsByTenIgnoreCase(newTen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên này đã tồn tại ở bản ghi khác!");
        }

        // 3. Update thông tin
        entity.setTen(newTen);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : entity.getTrangThai());

        // 4. Set ngày sửa để bảng "Cân bằng" nhảy giờ
        entity.setNgaySua(LocalDateTime.now());

        return danhMucRepository.save(entity);
    }
}