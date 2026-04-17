package com.example.bee.controllers.api.product;

import com.example.bee.dto.SanPhamRequest;
import com.example.bee.dto.VariantRequest;
import com.example.bee.entities.product.HinhAnhSanPham;
import com.example.bee.entities.product.SanPham;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.repositories.catalog.*;
import com.example.bee.repositories.order.HoaDonChiTietRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.products.SanPhamRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDateTime;
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
    private final KhuyenMaiRepository khuyenMaiRepo;
    private final HoaDonChiTietRepository hoaDonChiTietRepo;

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
        Page<SanPham> pageResult = sanPhamrepo.search(q, trangThai, idDanhMuc, idHang, idChatLieu, pageable);
        LocalDateTime now = LocalDateTime.now();

        List<Object[]> totalSoldList = hoaDonChiTietRepo.countTotalSoldPerProduct();
        Map<Integer, Integer> soldMap = new HashMap<>();
        for (Object[] row : totalSoldList) {
            Integer productId = (Integer) row[0];
            Long total = (Long) row[1]; // SUM() trong JPQL trả về Long
            soldMap.put(productId, total.intValue());
        }

        for (SanPham sp : pageResult.getContent()) {

            sp.setSoLuotBan(soldMap.getOrDefault(sp.getId(), 0));

            if (sp.getChiTietSanPhams() != null) {
                for (SanPhamChiTiet spct : sp.getChiTietSanPhams()) {
                    BigDecimal giaGoc = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
                    BigDecimal giaSauKM = giaGoc;

                    // 🌟 Dùng hàm dò Sale mới: Tìm theo cả SP cha và SKU con
                    List<KhuyenMai> activeSales = khuyenMaiRepo.findActivePromotionsForSku(sp.getId(), spct.getId(), now);

                    if (activeSales != null && !activeSales.isEmpty()) {
                        KhuyenMai km = activeSales.get(0);
                        if ("PERCENT".equals(km.getLoai())) {
                            BigDecimal tyLe = km.getGiaTri().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                            BigDecimal tienGiam = giaGoc.multiply(tyLe).setScale(0, RoundingMode.HALF_UP);
                            giaSauKM = giaGoc.subtract(tienGiam);
                        } else {
                            if (km.getGiaTri() != null) {
                                giaSauKM = giaGoc.subtract(km.getGiaTri());
                            }
                        }
                    }
                    if (giaSauKM.compareTo(BigDecimal.ZERO) < 0) {
                        giaSauKM = BigDecimal.ZERO;
                    }
                    spct.setGiaSauKhuyenMai(giaSauKM);
                }
            }
        }
        return pageResult;
    }

    private String generateMa() {
        return "SP" + System.currentTimeMillis();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SanPham> getOne(@PathVariable Integer id) {
        Optional<SanPham> spOpt = sanPhamrepo.findById(id);
        if (spOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SanPham sp = spOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (sp.getChiTietSanPhams() != null) {
            for (SanPhamChiTiet spct : sp.getChiTietSanPhams()) {
                BigDecimal giaGoc = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
                BigDecimal giaSauKM = giaGoc;

                // 🌟 Dùng hàm dò Sale mới
                List<KhuyenMai> activeSales = khuyenMaiRepo.findActivePromotionsForSku(sp.getId(), spct.getId(), now);

                if (activeSales != null && !activeSales.isEmpty()) {
                    KhuyenMai km = activeSales.get(0);
                    if ("PERCENT".equals(km.getLoai())) {
                        BigDecimal tyLe = km.getGiaTri().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        BigDecimal tienGiam = giaGoc.multiply(tyLe).setScale(0, RoundingMode.HALF_UP);
                        giaSauKM = giaGoc.subtract(tienGiam);
                    } else {
                        if (km.getGiaTri() != null) {
                            giaSauKM = giaGoc.subtract(km.getGiaTri());
                        }
                    }
                }

                if (giaSauKM.compareTo(BigDecimal.ZERO) < 0) {
                    giaSauKM = BigDecimal.ZERO;
                }
                spct.setGiaSauKhuyenMai(giaSauKM);
            }
        }

        return ResponseEntity.ok(sp);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody SanPhamRequest body, UriComponentsBuilder uriBuilder) {
        SanPham sp = new SanPham();

        if (isBlank(body.getMa())) {
            sp.setMa(generateMa());
        } else {
            String ma = body.getMa().trim().toUpperCase();
            if (sanPhamrepo.existsByMaIgnoreCase(ma)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Collections.singletonMap("message", "Mã sản phẩm này đã tồn tại!"));
            }
            sp.setMa(ma);
        }

        String ten = body.getTen().trim();
        if (ten.length() > 255) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Tên sản phẩm tối đa 255 ký tự!"));
        }
        if (sp.getMa() != null && sp.getMa().length() > 50) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Mã sản phẩm tối đa 50 ký tự!"));
        }
        Integer idDM = body.getIdDanhMuc();
        Integer idHang = body.getIdHang();
        Integer idCL = body.getIdChatLieu();

        if (sanPhamrepo.existsByTenIgnoreCaseAndDanhMuc_IdAndHang_IdAndChatLieu_Id(ten, idDM, idHang, idCL)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("message", "Sản phẩm với Tên, Danh mục, Hãng và Chất liệu này đã tồn tại!"));
        }

        mapCommonFields(sp, body);
        SanPham saved = sanPhamrepo.save(sp);
        URI location = uriBuilder.path("/api/products/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody SanPhamRequest body) {
        SanPham sp = sanPhamrepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));

        String ten = body.getTen().trim();
        Integer idDM = body.getIdDanhMuc();
        Integer idHang = body.getIdHang();
        Integer idCL = body.getIdChatLieu();

        if (sanPhamrepo.existsByTenIgnoreCaseAndDanhMuc_IdAndHang_IdAndChatLieu_IdAndIdNot(ten, idDM, idHang, idCL, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("message", "Sản phẩm với Tên, Danh mục, Hãng và Chất liệu này đã tồn tại!"));
        }

        mapCommonFields(sp, body);
        return ResponseEntity.ok(sanPhamrepo.save(sp));
    }

    private void mapCommonFields(SanPham sp, SanPhamRequest body) {
        sp.setTen(body.getTen().trim());
        sp.setMoTa(safeTrim(body.getMoTa()));
        sp.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        if (sp.getId() == null) {
            sp.setNgayTao(new Date());
        } else {
            sp.setNgaySua(new Date());
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
                ha.setSanPham(sp);
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
        List<SanPhamChiTiet> variants = variantRepo.findBySanPhamId(id);
        LocalDateTime now = LocalDateTime.now();

        for (SanPhamChiTiet spct : variants) {
            BigDecimal giaGoc = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
            BigDecimal giaSauKM = giaGoc;

            // 🌟 Dùng hàm dò Sale mới
            List<KhuyenMai> activeSales = khuyenMaiRepo.findActivePromotionsForSku(id, spct.getId(), now);

            if (activeSales != null && !activeSales.isEmpty()) {
                KhuyenMai km = activeSales.get(0);
                if ("PERCENT".equals(km.getLoai())) {
                    BigDecimal tyLe = km.getGiaTri().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    BigDecimal tienGiam = giaGoc.multiply(tyLe).setScale(0, RoundingMode.HALF_UP);
                    giaSauKM = giaGoc.subtract(tienGiam);
                } else {
                    if (km.getGiaTri() != null) {
                        giaSauKM = giaGoc.subtract(km.getGiaTri());
                    }
                }
            }

            if (giaSauKM.compareTo(BigDecimal.ZERO) < 0) {
                giaSauKM = BigDecimal.ZERO;
            }
            spct.setGiaSauKhuyenMai(giaSauKM);
        }

        return variants;
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

        var mauSac = mauSacRepo.findById(req.getIdMauSac())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Màu sắc không hợp lệ"));
        var kichThuoc = kichThuocRepo.findById(req.getIdKichThuoc())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích thước không hợp lệ"));

        variant.setMauSac(mauSac);
        variant.setKichThuoc(kichThuoc);

        variant.setGiaBan(req.getGiaBan());
        variant.setHinhAnh(req.getHinhAnh());

        int soLuongNhap = (req.getSoLuong() != null) ? req.getSoLuong() : 0;
        variant.setSoLuong(soLuongNhap);

        variant.setTrangThai(soLuongNhap > 0);

        String maSanPham = sp.getMa();
        String maMau = mauSac.getMa();
        String maKichThuoc = kichThuoc.getMa();

        String skuCode = String.format("%s-%s-%s", maSanPham, maMau, maKichThuoc);

        if (variantRepo.existsBySku(skuCode)) {
            skuCode += "-" + System.currentTimeMillis();
        }

        variant.setSku(skuCode);
        return ResponseEntity.ok(variantRepo.save(variant));
    }

    @PutMapping("/variants/{id}")
    @Transactional
    public ResponseEntity<?> updateVariant(@PathVariable Integer id, @RequestBody VariantRequest req) {
        return variantRepo.findById(id).map(variant -> {
            Integer currentMauId = variant.getMauSac() != null ? variant.getMauSac().getId() : null;
            Integer currentSizeId = variant.getKichThuoc() != null ? variant.getKichThuoc().getId() : null;

            boolean isChangingAttr = false;
            if (req.getIdMauSac() != null && !req.getIdMauSac().equals(currentMauId)) isChangingAttr = true;
            if (req.getIdKichThuoc() != null && !req.getIdKichThuoc().equals(currentSizeId)) isChangingAttr = true;

            if (isChangingAttr && variantRepo.existsBySanPhamIdAndMauSacIdAndKichThuocId(
                    variant.getSanPham().getId(), req.getIdMauSac(), req.getIdKichThuoc())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Biến thể Màu + Size này đã tồn tại!");
            }

            if (req.getGiaBan() != null) {
                if (req.getGiaBan().compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá bán không được nhỏ hơn 0!");
                }
                variant.setGiaBan(req.getGiaBan());
            }

            if (req.getIdMauSac() != null) {
                variant.setMauSac(mauSacRepo.findById(req.getIdMauSac()).orElse(variant.getMauSac()));
            }
            if (req.getIdKichThuoc() != null) {
                variant.setKichThuoc(kichThuocRepo.findById(req.getIdKichThuoc()).orElse(variant.getKichThuoc()));
            }
            if (req.getHinhAnh() != null) {
                variant.setHinhAnh(req.getHinhAnh());
            }

            variantRepo.save(variant);
            return ResponseEntity.ok(Collections.singletonMap("message", "Cập nhật biến thể thành công!"));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể"));
    }

    @PatchMapping("/variants/{id}/update-stock")
    @Transactional
    public ResponseEntity<?> updateStock(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        return variantRepo.findById(id).map(variant -> {
            Integer newQty = body.get("soLuong");

            if (newQty == null || newQty < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng tồn kho không được nhỏ hơn 0!");
            }

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

    private String removeVietnameseTones(String str) {
        if (str == null) return "";
        str = str.replaceAll("à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ", "a");
        str = str.replaceAll("è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ", "e");
        str = str.replaceAll("ì|í|ị|ỉ|ĩ", "i");
        str = str.replaceAll("ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ", "o");
        str = str.replaceAll("ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ", "u");
        str = str.replaceAll("ỳ|ý|ỵ|ỷ|ỹ", "y");
        str = str.replaceAll("đ", "d");
        str = str.replaceAll("À|Á|Ạ|Ả|Ã|Â|Ầ|Ấ|Ậ|Ẩ|Ẫ|Ă|Ằ|Ắ|Ặ|Ẳ|Ẵ", "A");
        str = str.replaceAll("[^a-zA-Z0-9 ]", "");
        return str.replaceAll("\\s+", "").toUpperCase();
    }

}