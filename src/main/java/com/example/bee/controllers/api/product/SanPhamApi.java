package com.example.bee.controllers.api.product;

import com.example.bee.dto.SanPhamRequest;
import com.example.bee.dto.VariantRequest;
import com.example.bee.entities.product.HinhAnhSanPham;
import com.example.bee.entities.product.SanPham;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.repositories.catalog.*;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.products.SanPhamRepository;
import jakarta.transaction.Transactional;
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

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class SanPhamApi {

    private final SanPhamRepository sanPhamrepo;
    private final DanhMucRepository danhMucRepo;
    private final HangRepository hangRepo;
    private final ChatLieuRepository chatLieuRepo;
    private final SanPhamChiTietRepository variantRepo;
    private final MauSacRepository mauSacRepo;
    private final KichThuocRepository kichThuocRepo;

    @GetMapping
    public Page<SanPham> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) Integer idDanhMuc,
            @RequestParam(required = false) Integer idHang,
            @RequestParam(required = false) Integer idChatLieu,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return sanPhamrepo.search(q, trangThai, idDanhMuc, idHang, idChatLieu, pageable);
    }

    private String generateMa() {
        for (int i = 1; i <= 9999; i++) {
            String ma = String.format("SP%04d", i); // SP0001 → SP9999
            if (!sanPhamrepo.existsByMaIgnoreCase(ma)) return ma;
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Đã hết mã sản phẩm (SP0001-SP9999)");
    }

    @GetMapping("/{id}")
    public ResponseEntity<SanPham> getOne(@PathVariable Integer id) {
        return sanPhamrepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<SanPham> create(@Valid @RequestBody SanPhamRequest body, UriComponentsBuilder uriBuilder) {
        SanPham sp = new SanPham();
        if (isBlank(body.getMa())) {
            sp.setMa(generateMa());
        } else {
            String ma = body.getMa().trim().toUpperCase();
            if (sanPhamrepo.existsByMaIgnoreCase(ma)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã sản phẩm đã tồn tại");
            }
            sp.setMa(ma);
        }
        mapCommonFields(sp, body);
        SanPham saved = sanPhamrepo.save(sp);
        URI location = uriBuilder.path("/api/products/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public SanPham update(@PathVariable Integer id, @Valid @RequestBody SanPhamRequest body) {
        SanPham sp = sanPhamrepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
        mapCommonFields(sp, body);
        return sanPhamrepo.save(sp);
    }

    private void mapCommonFields(SanPham sp, SanPhamRequest body) {
        sp.setTen(body.getTen().trim());
        sp.setMoTa(safeTrim(body.getMoTa()));
        sp.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        if (sp.getId() == null) {
            sp.setNgayTao(new Date()); // Nếu chưa có ID (Thêm mới) -> Gán ngày tạo
        } else {
            sp.setNgaySua(new Date()); // Nếu đã có ID (Cập nhật) -> Gán ngày sửa
        }
        sp.setDanhMuc(body.getIdDanhMuc() != null ? danhMucRepo.findById(body.getIdDanhMuc()).orElse(null) : null);
        sp.setHang(body.getIdHang() != null ? hangRepo.findById(body.getIdHang()).orElse(null) : null);
        sp.setChatLieu(body.getIdChatLieu() != null ? chatLieuRepo.findById(body.getIdChatLieu()).orElse(null) : null);
        if (body.getDanhSachHinhAnh() != null) {
            if (sp.getHinhAnhs() == null) {
                sp.setHinhAnhs(new ArrayList<>());
            } else {
                sp.getHinhAnhs().clear();
            }
            for (String path : body.getDanhSachHinhAnh()) {
                HinhAnhSanPham ha = new HinhAnhSanPham();
                ha.setUrl(path);
                ha.setSanPham(sp); // BẮT BUỘC PHẢI CÓ DÒNG NÀY ĐỂ MAPPING DB
                sp.getHinhAnhs().add(ha);
            }
        }
    }

    @PatchMapping("/{id}/trang-thai")
    @Transactional
    public ResponseEntity<?> toggleTrangThai(@PathVariable Integer id) {
        return sanPhamrepo.findById(id).map(sp -> {
            sp.setTrangThai(!sp.getTrangThai());
            sanPhamrepo.save(sp);
            return ResponseEntity.ok(Collections.singletonMap("message",
                    "Đã " + (sp.getTrangThai() ? "mở bán" : "ngừng bán") + " sản phẩm!"));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{id}/variants")
    public List<SanPhamChiTiet> getVariants(@PathVariable Integer id) {
        return variantRepo.findBySanPhamId(id);
    }

    @PostMapping("/{productId}/variants")
    @Transactional
    public ResponseEntity<?> createVariant(@PathVariable Integer productId, @RequestBody VariantRequest req) {
        SanPham sp = sanPhamrepo.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm gốc"));

        if (variantRepo.existsBySanPhamIdAndMauSacIdAndKichThuocId(productId, req.getIdMauSac(), req.getIdKichThuoc())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("message", "Biến thể Màu + Size này đã tồn tại!"));
        }
        SanPhamChiTiet variant = new SanPhamChiTiet();
        variant.setSanPham(sp);
        variant.setMauSac(mauSacRepo.findById(req.getIdMauSac()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Màu sắc không hợp lệ")));
        variant.setKichThuoc(kichThuocRepo.findById(req.getIdKichThuoc()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích thước không hợp lệ")));
        variant.setGiaBan(req.getGiaBan());
        variant.setHinhAnh(req.getHinhAnh());
        variant.setSoLuong(0);
        variant.setTrangThai(false);
        String skuCode;
        do {
            long nextId = (variantRepo.findMaxId() != null ? variantRepo.findMaxId() : 0L) + 1;
            skuCode = String.format("SKU%05d", nextId); // SKU00001 → SKU99999
        } while (variantRepo.existsBySku(skuCode));
        variant.setSku(skuCode);
        return ResponseEntity.ok(variantRepo.save(variant));
    }

    @PutMapping("/variants/{id}")
    @Transactional
    public ResponseEntity<?> updateVariant(@PathVariable Integer id, @RequestBody VariantRequest req) {
        return variantRepo.findById(id).map(variant -> {
            boolean isChangingAttr = !variant.getMauSac().getId().equals(req.getIdMauSac())
                    || !variant.getKichThuoc().getId().equals(req.getIdKichThuoc());
            if (isChangingAttr && variantRepo.existsBySanPhamIdAndMauSacIdAndKichThuocId(
                    variant.getSanPham().getId(), req.getIdMauSac(), req.getIdKichThuoc())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Biến thể Màu + Size này đã tồn tại!");
            }
            variant.setGiaBan(req.getGiaBan());
            variant.setMauSac(mauSacRepo.findById(req.getIdMauSac()).orElse(variant.getMauSac()));
            variant.setKichThuoc(kichThuocRepo.findById(req.getIdKichThuoc()).orElse(variant.getKichThuoc()));
            if (req.getHinhAnh() != null) variant.setHinhAnh(req.getHinhAnh());
            variantRepo.save(variant);
            return ResponseEntity.ok(Collections.singletonMap("message", "Cập nhật biến thể thành công!"));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể"));
    }

    @PatchMapping("/variants/{id}/update-stock")
    @Transactional
    public ResponseEntity<?> updateStock(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        return variantRepo.findById(id).map(variant -> {
            Integer newQty = body.get("soLuong");
            variant.setSoLuong(newQty);
            if (newQty > 0 && (variant.getTrangThai() == null || !variant.getTrangThai())) {
                variant.setTrangThai(true);
            } else if (newQty == 0 && (variant.getTrangThai() == null || variant.getTrangThai())) {
                variant.setTrangThai(false);
            }
            variantRepo.save(variant);
            return ResponseEntity.ok(Collections.singletonMap("message", "Cập nhật kho và trạng thái thành công!"));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không thấy biến thể"));
    }

    @PatchMapping("/variants/{id}/trang-thai")
    @Transactional
    public ResponseEntity<?> toggleVariantStatus(@PathVariable Integer id) {
        return variantRepo.findById(id).map(variant -> {
            variant.setTrangThai(!variant.getTrangThai());
            variantRepo.save(variant);
            String statusLabel = variant.getTrangThai() ? "đang kinh doanh" : "ngừng kinh doanh";
            return ResponseEntity.ok(Collections.singletonMap("message", "Biến thể [" + variant.getSku() + "] hiện " + statusLabel));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không thấy biến thể"));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}