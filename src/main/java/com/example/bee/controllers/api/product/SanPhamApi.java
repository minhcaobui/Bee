package com.example.bee.controllers.api.product;

import com.example.bee.dtos.SanPhamRequest;
import com.example.bee.dtos.VariantRequest;
import com.example.bee.entities.product.SanPham;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.services.SanPhamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/san-pham")
@RequiredArgsConstructor
public class SanPhamApi {

    private final SanPhamService sanPhamService;

    @GetMapping
    public ResponseEntity<Page<SanPham>> danhSachSanPham(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) Integer idDanhMuc,
            @RequestParam(required = false) Integer idHang,
            @RequestParam(required = false) Integer idChatLieu,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(sanPhamService.layDanhSachSanPham(q, trangThai, idDanhMuc, idHang, idChatLieu, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SanPham> chiTietSanPham(@PathVariable Integer id) {
        return ResponseEntity.ok(sanPhamService.layChiTietSanPham(id));
    }

    @PostMapping
    public ResponseEntity<?> taoMoiSanPham(@Valid @RequestBody SanPhamRequest body, UriComponentsBuilder uriBuilder) {
        SanPham saved = sanPhamService.taoMoiSanPham(body);
        URI location = uriBuilder.path("/api/san-pham/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> capNhatSanPham(@PathVariable Integer id, @Valid @RequestBody SanPhamRequest body) {
        return ResponseEntity.ok(sanPhamService.capNhatSanPham(id, body));
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThaiSanPham(@PathVariable Integer id) {
        return ResponseEntity.ok(sanPhamService.doiTrangThaiSanPham(id));
    }

    @GetMapping("/{id}/bien-the")
    public ResponseEntity<List<SanPhamChiTiet>> danhSachBienThe(@PathVariable Integer id) {
        return ResponseEntity.ok(sanPhamService.layDanhSachBienThe(id));
    }

    @PostMapping("/{productId}/bien-the")
    public ResponseEntity<?> taoMoiBienThe(@PathVariable Integer productId, @RequestBody VariantRequest req) {
        return ResponseEntity.ok(sanPhamService.taoMoiBienThe(productId, req));
    }

    @PutMapping("/bien-the/{id}")
    public ResponseEntity<?> capNhatBienThe(@PathVariable Integer id, @RequestBody VariantRequest req) {
        return ResponseEntity.ok(sanPhamService.capNhatBienThe(id, req));
    }

    @PatchMapping("/bien-the/{id}/cap-nhat-ton-kho")
    public ResponseEntity<?> capNhatTonKhoBienThe(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(sanPhamService.capNhatTonKhoBienThe(id, body));
    }

    @PatchMapping("/bien-the/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThaiBienThe(@PathVariable Integer id) {
        return ResponseEntity.ok(sanPhamService.doiTrangThaiBienThe(id));
    }
}