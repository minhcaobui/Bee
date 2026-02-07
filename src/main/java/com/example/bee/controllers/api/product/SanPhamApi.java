package com.example.bee.controllers.api.product;

import com.example.bee.dto.SanPhamRequest;
import com.example.bee.entities.product.HinhAnhSanPham;
import com.example.bee.entities.product.SanPham;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.repositories.catalog.ChatLieuRepository;
import com.example.bee.repositories.catalog.DanhMucRepository;
import com.example.bee.repositories.catalog.HangRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class SanPhamApi {

    private final SanPhamRepository sanPhamrepo;
    private final DanhMucRepository danhMucRepo;
    private final HangRepository hangRepo;
    private final ChatLieuRepository chatLieuRepo;
    private final SanPhamChiTietRepository variantRepo;

    private static final String MA_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MA_LEN = 10;
    private static final SecureRandom RAND = new SecureRandom();

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

    @GetMapping("/{id}")
    public ResponseEntity<SanPham> getOne(@PathVariable Integer id) {
        return sanPhamrepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SanPham> create(@Valid @RequestBody SanPhamRequest body, UriComponentsBuilder uriBuilder) {
        SanPham sp = new SanPham();

        // Xử lý mã
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

        if (sp.getId() != null) {
            sp.setNgaySua(new Date());
        }

        // Map quan hệ
        sp.setDanhMuc(danhMucRepo.findById(body.getIdDanhMuc())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục không tồn tại")));
        sp.setHang(hangRepo.findById(body.getIdHang())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hãng không tồn tại")));
        sp.setChatLieu(chatLieuRepo.findById(body.getIdChatLieu())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chất liệu không tồn tại")));

        // Xử lý hình ảnh
        if (body.getDanhSachHinhAnh() != null) {
            sp.getHinhAnhs().clear();
            for (String path : body.getDanhSachHinhAnh()) {
                HinhAnhSanPham ha = new HinhAnhSanPham();

                // SỬA DÒNG NÀY: dùng setUrl thay vì setTenHinhAnh
                ha.setUrl(path);

                sp.addHinhAnh(ha);
            }
        }
    }

    private String generateMa() {
        String ma;
        do {
            StringBuilder sb = new StringBuilder(MA_LEN);
            for (int i = 0; i < MA_LEN; i++) sb.append(MA_CHARS.charAt(RAND.nextInt(MA_CHARS.length())));
            ma = sb.toString();
        } while (sanPhamrepo.existsByMaIgnoreCase(ma));
        return ma;
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> toggleTrangThai(@PathVariable Integer id) {
        return sanPhamrepo.findById(id).map(sp -> {
            sp.setTrangThai(!sp.getTrangThai()); // Đảo ngược trạng thái hiện tại
            sanPhamrepo.save(sp);
            return ResponseEntity.ok(Collections.singletonMap("message",
                    "Đã " + (sp.getTrangThai() ? "mở bán" : "ngừng bán") + " sản phẩm!"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/variants")
    public List<SanPhamChiTiet> getVariants(@PathVariable Integer id) {
        return variantRepo.findBySanPhamId(id);
    }

    @PostMapping("/variants")
    public ResponseEntity<?> createVariant(@RequestBody SanPhamChiTiet variant) {
        if (variantRepo.existsBySanPhamIdAndMauSacIdAndKichThuocId(
                variant.getSanPham().getId(),
                variant.getMauSac().getId(),
                variant.getKichThuoc().getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Biến thể Màu + Size này đã tồn tại!");
        }
        return ResponseEntity.ok(variantRepo.save(variant));
    }

    @PatchMapping("/variants/{id}/update-stock")
    public ResponseEntity<?> updateStock(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        // 1. Tìm biến thể trong Database
        return variantRepo.findById(id).map(variant -> {
            // 2. Lấy số lượng mới từ Request gửi lên
            Integer newQty = body.get("soLuong");

            // 3. Cập nhật và lưu
            variant.setSoLuong(newQty);
            variantRepo.save(variant);

            return ResponseEntity.ok(Collections.singletonMap("message", "Cập nhật kho thành công!"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/variants/{id}/trang-thai")
    public ResponseEntity<?> toggleVariantStatus(@PathVariable Integer id) {
        return variantRepo.findById(id).map(variant -> {
            // 1. Đảo ngược trạng thái
            variant.setTrangThai(!variant.getTrangThai());

            // 2. Lưu thay đổi
            variantRepo.save(variant);

            // 3. Trả về thông báo đẹp cho Frontend
            String statusLabel = variant.getTrangThai() ? "đang kinh doanh" : "ngừng kinh doanh";
            return ResponseEntity.ok(Collections.singletonMap("message",
                    "Biến thể [" + variant.getSku() + "] hiện " + statusLabel));

        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Collections.singletonMap("message", "Không tìm thấy biến thể này!")));
    }

    @PutMapping("/variants/{id}")
    public ResponseEntity<?> updateVariant(@PathVariable Integer id, @RequestBody SanPhamChiTiet variantRequest) {
        return variantRepo.findById(id).map(variant -> {
            // 1. Cập nhật các trường thông tin
            variant.setGiaBan(variantRequest.getGiaBan());
            variant.setMauSac(variantRequest.getMauSac());
            variant.setKichThuoc(variantRequest.getKichThuoc());

            // Cập nhật hình ảnh nếu có gửi lên
            if (variantRequest.getHinhAnh() != null) {
                variant.setHinhAnh(variantRequest.getHinhAnh());
            }

            // 2. Lưu vào database
            variantRepo.save(variant);

            // 3. Trả về thông báo thành công
            return ResponseEntity.ok(Collections.singletonMap("message", "Cập nhật biến thể thành công!"));

        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Collections.singletonMap("message", "Không tìm thấy biến thể để cập nhật!")));
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private String safeTrim(String s) { return s == null ? null : s.trim(); }
}

