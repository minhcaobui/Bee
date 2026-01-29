//package com.example.bee.controllers.api.product;
//
//import com.example.bee.dto.SanPhamRequest;
//import com.example.bee.entities.product.SanPham;
//import com.example.bee.repositories.catalog.ChatLieuRepository;
//import com.example.bee.repositories.catalog.DanhMucRepository;
//import com.example.bee.repositories.catalog.HangRepository;
//import com.example.bee.repositories.products.SanPhamRepository;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.server.ResponseStatusException;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import java.net.URI;
//import java.security.SecureRandom;
//
//@RestController
//@RequestMapping("/api/products")
//@RequiredArgsConstructor
//public class SanPhamApi {
//
//    private final SanPhamRepository repo;
//    private final DanhMucRepository danhMucRepo;
//    private final HangRepository hangRepo;
//    private final ChatLieuRepository chatLieuRepo;
//
//    private static final String MA_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//    private static final int MA_LEN = 10;
//    private static final SecureRandom RAND = new SecureRandom();
//
//    private String generateMa() {
//        String ma;
//        do {
//            StringBuilder sb = new StringBuilder(MA_LEN);
//            for (int i = 0; i < MA_LEN; i++) {
//                sb.append(MA_CHARS.charAt(RAND.nextInt(MA_CHARS.length())));
//            }
//            ma = sb.toString();
//        } while (repo.existsByMaIgnoreCase(ma)); // chống trùng
//        return ma;
//    }
//    // ===== GET: list =====
//    @GetMapping
//    public Page<SanPham> list(
//            @RequestParam(required = false) String q,
//            @RequestParam(required = false) Boolean trangThai,
//            @RequestParam(required = false) Integer idDanhMuc,
//            @RequestParam(required = false) Integer idHang,
//            @RequestParam(required = false) Integer idChatLieu,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size
//    ) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
//        return repo.search(q, trangThai, idDanhMuc, idHang, idChatLieu, pageable);
//    }
//
//    // ===== POST: tạo =====
//    @PostMapping
//    public ResponseEntity<SanPham> create(
//            @Valid @RequestBody SanPhamRequest body,
//            UriComponentsBuilder uriBuilder
//    ) {
//        SanPham sp = new SanPham();
//
//        // ===== XỬ LÝ MÃ (OPTIONAL) =====
//        String ma;
//        if (body.getMa() == null || body.getMa().trim().isEmpty()) {
//            // KHÔNG NHẬP → TỰ SINH
//            ma = generateMa();
//        } else {
//            // CÓ NHẬP → DÙNG MA NHẬP
//            ma = body.getMa().trim().toUpperCase();
//            if (repo.existsByMaIgnoreCase(ma)) {
//                throw new ResponseStatusException(
//                        HttpStatus.CONFLICT, "Mã sản phẩm đã tồn tại"
//                );
//            }
//        }
//        sp.setMa(ma);
//
//        // ===== MAP CÁC FIELD CÒN LẠI =====
//        mapCommonFields(sp, body);
//
//        try {
//            SanPham saved = repo.save(sp);
//            URI location = uriBuilder
//                    .path("/api/products/{id}")
//                    .buildAndExpand(saved.getId())
//                    .toUri();
//            return ResponseEntity.created(location).body(saved);
//        } catch (DataIntegrityViolationException e) {
//            throw new ResponseStatusException(
//                    HttpStatus.CONFLICT,
//                    "Không thể tạo sản phẩm, vui lòng thử lại",
//                    e
//            );
//        }
//    }
//
//
//    // ===== PUT: cập nhật (KHÔNG CHO SỬA MA) =====
//    @PutMapping("{id}")
//    public SanPham update(
//            @PathVariable Integer id,
//            @Valid @RequestBody SanPhamRequest body
//    ) {
//        SanPham sp = repo.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(
//                        HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
//
//        mapCommonFields(sp, body);
//        return repo.save(sp);
//    }
//
//    // ===== Helper =====
//    private void mapCommonFields(SanPham sp, SanPhamRequest body) {
//
//        sp.setTen(body.getTen().trim());
//        sp.setMoTa(safeTrim(body.getMoTa()));
//        sp.setHinhAnhDaiDien(safeTrim(body.getHinhAnhDaiDien()));
//        sp.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
//
//        sp.setDanhMuc(
//                danhMucRepo.findById(body.getIdDanhMuc())
//                        .orElseThrow(() -> new ResponseStatusException(
//                                HttpStatus.BAD_REQUEST, "Danh mục không tồn tại"))
//        );
//        sp.setHang(
//                hangRepo.findById(body.getIdHang())
//                        .orElseThrow(() -> new ResponseStatusException(
//                                HttpStatus.BAD_REQUEST, "Hãng không tồn tại"))
//        );
//        sp.setChatLieu(
//                chatLieuRepo.findById(body.getIdChatLieu())
//                        .orElseThrow(() -> new ResponseStatusException(
//                                HttpStatus.BAD_REQUEST, "Chất liệu không tồn tại"))
//        );
//    }
//
//    private static boolean isBlank(String s) {
//        return s == null || s.trim().isEmpty();
//    }
//
//    private static String safeTrim(String s) {
//        return s == null ? null : s.trim();
//    }
//}
//
