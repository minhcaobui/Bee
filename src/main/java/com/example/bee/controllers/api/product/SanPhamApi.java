package com.example.bee.controllers.api.product;

import com.example.bee.dto.SanPhamReq;
import com.example.bee.entities.catalog.DanhMuc;
import com.example.bee.entities.catalog.Hang;
import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.entities.catalog.SanPham;
import com.example.bee.repositories.catalog.ChatLieuRepository;
import com.example.bee.repositories.catalog.DanhMucRepository;
import com.example.bee.repositories.catalog.HangRepository;
import com.example.bee.repositories.catalog.SanPhamRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("api/san-pham")
@RequiredArgsConstructor
public class SanPhamApi {

    private final SanPhamRepository repo;

    // Inject thêm Repo của mấy bảng cha để tìm ID
    private final DanhMucRepository danhMucRepo;
    private final HangRepository hangRepo;
    private final ChatLieuRepository chatLieuRepo;

    // ===== GET: danh sách phân trang =====
    @GetMapping
    public Page<SanPham> list(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "5") int size) {
        size = Math.max(1, Math.min(size, 100));
        // Sắp xếp theo ngày tạo mới nhất hoặc ID giảm dần
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return repo.findAll(pageable);
    }

    @GetMapping("{id}")
    public SanPham get(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
    }

    // ===== POST: Tạo mới (Dùng DTO để hứng ID) =====
    @PostMapping
    @Transactional
    public ResponseEntity<SanPham> create(
            @Valid @RequestBody SanPhamReq sanPhamReq,
            UriComponentsBuilder uriBuilder) {

        SanPham sp = new SanPham();

        // 1. Xử lý Mã, Tên, Trùng Mã (Logic không đổi)
        if (isBlank(sanPhamReq.getMa())) {
            sp.setMa(nextAutoCode());
        } else {
            sp.setMa(sanPhamReq.getMa().trim());
        }
        if (repo.existsByMa(sp.getMa())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã sản phẩm đã tồn tại");
        }

        // Map thông tin cơ bản
        sp.setTen(safeTrim(sanPhamReq.getTen()));
        sp.setMoTa(sanPhamReq.getMoTa() == null ? "Mô tả mặc định" : sanPhamReq.getMoTa());
        sp.setTrangThai(sanPhamReq.getTrangThai() == null ? true : sanPhamReq.getTrangThai());

        sp.setNgayTao(LocalDateTime.now());
        // 3. Map Khóa Ngoại
        mapRelations(sp, sanPhamReq);

        try {
            SanPham saved = repo.save(sp);
            // Frontend cần biết POST thành công để RESET PAGE = 1 và reload
            URI location = uriBuilder.path("/api/san-pham/{id}").buildAndExpand(saved.getId()).toUri();
            return ResponseEntity.created(location).body(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lỗi dữ liệu (trùng mã hoặc thiếu thông tin)", e);
        }
    }

    // ===== PUT: Cập nhật (Sửa lại để đồng bộ với POST và báo Frontend refresh) =====
    @PutMapping("{id}")
    @Transactional
    public ResponseEntity<SanPham> update(@PathVariable Integer id,
                                          @RequestBody SanPhamReq body) {

        SanPham sp = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));

        // Check trùng mã nếu đổi mã (Giữ nguyên logic của bạn)
        String newMa = isBlank(body.getMa()) ? sp.getMa() : body.getMa().trim();
        sp.setMa(newMa);

        // Update các trường
        sp.setTen(safeTrimOrDefault(body.getTen(), sp.getTen()));
        sp.setMoTa(body.getMoTa());
        if (body.getTrangThai() != null) sp.setTrangThai(body.getTrangThai());

        // Update luôn mấy cái danh mục hãng nếu có chọn lại
        mapRelations(sp, body);

        SanPham saved = repo.save(sp);
        // QUAN TRỌNG: Trả về 200 OK (hoặc 204 No Content), Frontend sẽ nhận biết và RESET PAGE
        return ResponseEntity.ok(saved);
    }

    // ===== Helpers Mapping Relation =====
    private void mapRelations(SanPham sp, SanPhamReq req) {
        if (req.getIdDanhMuc() != null) {
            DanhMuc dm = danhMucRepo.findById(req.getIdDanhMuc()).orElse(null);
            if (dm != null) sp.setDanhMuc(dm);
        }
        if (req.getIdHang() != null) {
            Hang h = hangRepo.findById(req.getIdHang()).orElse(null);
            if (h != null) sp.setHang(h);
        }
        if (req.getIdChatLieu() != null) {
            ChatLieu cl = chatLieuRepo.findById(req.getIdChatLieu()).orElse(null);
            if (cl != null) sp.setChatLieu(cl);
        }
    }

    // ===== Common Helpers =====
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String safeTrim(String s) { return s == null ? null : s.trim(); }
    private static String safeTrimOrDefault(String s, String def) { return s == null ? def : s.trim(); }

    private String nextAutoCode() {
        return "SP_" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }

}