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
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import java.math.BigDecimal;

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
    private final KhuyenMaiRepository khuyenMaiRepo;

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

        // 1. Lấy danh sách sản phẩm từ DB
        Page<SanPham> pageResult = sanPhamrepo.search(q, trangThai, idDanhMuc, idHang, idChatLieu, pageable);

        // 2. 🌟 VÒNG LẶP TÍNH GIÁ SALE CHO TỪNG BIẾN THỂ TRƯỚC KHI TRẢ VỀ FRONTEND
        for (SanPham sp : pageResult.getContent()) {
            // Tìm xem SP này có đang được áp dụng đợt Sale nào không
            List<KhuyenMai> activeSales = khuyenMaiRepo.findActiveKhuyenMaiBySanPhamId(sp.getId());

            // Nếu sản phẩm có biến thể (Màu/Size)
            if (sp.getChiTietSanPhams() != null) {
                for (SanPhamChiTiet spct : sp.getChiTietSanPhams()) {
                    BigDecimal giaGoc = spct.getGiaBan();
                    if (giaGoc == null) giaGoc = BigDecimal.ZERO;

                    BigDecimal giaSauKM = giaGoc;

                    // Nếu có đợt Sale đang chạy -> Bắt đầu trừ tiền
                    if (activeSales != null && !activeSales.isEmpty()) {
                        KhuyenMai km = activeSales.get(0); // Lấy đợt Sale đầu tiên
                        if ("PERCENT".equals(km.getLoai())) {
                            BigDecimal tyLe = km.getGiaTri().divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                            BigDecimal tienGiam = giaGoc.multiply(tyLe).setScale(0, java.math.RoundingMode.HALF_UP);
                            giaSauKM = giaGoc.subtract(tienGiam);
                        } else {
                            if (km.getGiaTri() != null) {
                                giaSauKM = giaGoc.subtract(km.getGiaTri());
                            }
                        }
                    }

                    // Không để giá sale bị âm
                    if (giaSauKM.compareTo(BigDecimal.ZERO) < 0) {
                        giaSauKM = BigDecimal.ZERO;
                    }

                    // Gán giá sau Sale vào biến thể để Frontend đọc được
                    spct.setGiaSauKhuyenMai(giaSauKM);
                }
            }
        }

        // 3. Trả về kết quả đã được tính toán
        return pageResult;
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
        Optional<SanPham> spOpt = sanPhamrepo.findById(id);
        if (spOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SanPham sp = spOpt.get();

        // 🌟 VÒNG LẶP TÍNH GIÁ KHUYẾN MÃI CHO TỪNG BIẾN THỂ
        List<KhuyenMai> activeSales = khuyenMaiRepo.findActiveKhuyenMaiBySanPhamId(sp.getId());

        if (sp.getChiTietSanPhams() != null) {
            for (SanPhamChiTiet spct : sp.getChiTietSanPhams()) {
                BigDecimal giaGoc = spct.getGiaBan();
                if (giaGoc == null) giaGoc = BigDecimal.ZERO;

                BigDecimal giaSauKM = giaGoc;

                // Nếu có đợt Sale đang chạy -> Bắt đầu trừ tiền
                if (activeSales != null && !activeSales.isEmpty()) {
                    KhuyenMai km = activeSales.get(0);
                    if ("PERCENT".equals(km.getLoai())) {
                        BigDecimal tyLe = km.getGiaTri().divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                        BigDecimal tienGiam = giaGoc.multiply(tyLe).setScale(0, java.math.RoundingMode.HALF_UP);
                        giaSauKM = giaGoc.subtract(tienGiam);
                    } else {
                        if (km.getGiaTri() != null) {
                            giaSauKM = giaGoc.subtract(km.getGiaTri());
                        }
                    }
                }

                // Không để giá sale bị âm
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
        List<SanPhamChiTiet> variants = variantRepo.findBySanPhamId(id);

        // 🌟 VÒNG LẶP TÍNH GIÁ KHUYẾN MÃI CHO DANH SÁCH BIẾN THỂ
        List<KhuyenMai> activeSales = khuyenMaiRepo.findActiveKhuyenMaiBySanPhamId(id);

        for (SanPhamChiTiet spct : variants) {
            BigDecimal giaGoc = spct.getGiaBan();
            if (giaGoc == null) giaGoc = BigDecimal.ZERO;

            BigDecimal giaSauKM = giaGoc;

            // Nếu có đợt Sale đang chạy -> Bắt đầu trừ tiền
            if (activeSales != null && !activeSales.isEmpty()) {
                KhuyenMai km = activeSales.get(0);
                if ("PERCENT".equals(km.getLoai())) {
                    BigDecimal tyLe = km.getGiaTri().divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                    BigDecimal tienGiam = giaGoc.multiply(tyLe).setScale(0, java.math.RoundingMode.HALF_UP);
                    giaSauKM = giaGoc.subtract(tienGiam);
                } else {
                    if (km.getGiaTri() != null) {
                        giaSauKM = giaGoc.subtract(km.getGiaTri());
                    }
                }
            }

            // Không để giá sale bị âm
            if (giaSauKM.compareTo(BigDecimal.ZERO) < 0) {
                giaSauKM = BigDecimal.ZERO;
            }

            // Gán giá sau Sale vào biến thể để Frontend đọc được
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

        // 1. Lấy ra Object Màu và Kích thước thật từ DB
        var mauSac = mauSacRepo.findById(req.getIdMauSac())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Màu sắc không hợp lệ"));
        var kichThuoc = kichThuocRepo.findById(req.getIdKichThuoc())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích thước không hợp lệ"));

        variant.setMauSac(mauSac);
        variant.setKichThuoc(kichThuoc);

        variant.setGiaBan(req.getGiaBan());
        variant.setHinhAnh(req.getHinhAnh());

        // 🌟 2. CẬP NHẬT: Nhận Tồn kho từ Frontend truyền lên (Thay vì set cứng = 0)
        int soLuongNhap = (req.getSoLuong() != null) ? req.getSoLuong() : 0;
        variant.setSoLuong(soLuongNhap);

        // Bật trạng thái Tự động (Nếu SL > 0 thì tự bật Đang kinh doanh)
        variant.setTrangThai(soLuongNhap > 0);

        // 🌟 3. NÂNG CẤP: Logic sinh mã SKU thông minh (Ví dụ: SP0001-DEN-XL)
        String maSanPham = sp.getMa(); // Lấy mã SP gốc
        String maMau = removeVietnameseTones(mauSac.getTen()); // Hàm chuyển tiếng việt không dấu
        String maKichThuoc = removeVietnameseTones(kichThuoc.getTen());

        // Ép chuỗi thành mã (Ví dụ: SP0001-DEN-XL)
        String skuCode = String.format("%s-%s-%s", maSanPham, maMau, maKichThuoc);

        // Nếu mã này lỡ bị trùng (Hiếm khi xảy ra), thì cộng thêm random
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

    // 🌟 HÀM TIỆN ÍCH DÙNG ĐỂ BÓC DẤU TIẾNG VIỆT (Nên để ở cuối class SanPhamApi)
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
        // ... Cắt các ký tự đặc biệt
        str = str.replaceAll("[^a-zA-Z0-9 ]", "");
        return str.replaceAll("\\s+", "").toUpperCase();
    }

}