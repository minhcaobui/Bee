package com.example.bee.controllers.api.promotion;

import com.example.bee.dto.PromotionRequest;
import com.example.bee.entities.product.SanPham;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.entities.promotion.KhuyenMaiSanPham;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
import com.example.bee.repositories.products.SanPhamRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import com.example.bee.repositories.promotion.KhuyenMaiSanPhamRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/khuyen-mai")
@RequiredArgsConstructor
public class KhuyenMaiApi {

    private final KhuyenMaiRepository khuyenMaiRepository;
    private final KhuyenMaiSanPhamRepository khuyenMaiSanPhamRepository;
    private final SanPhamRepository sanPhamRepo;
    private final SanPhamChiTietRepository sanPhamChiTietRepo;
    private final ThongBaoRepository thongBaoRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    private void validateChongLo(PromotionRequest req, List<SanPham> danhSachSanPham, List<SanPhamChiTiet> danhSachSku) {
        if (req.getTen() == null || req.getTen().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên chương trình không được để trống.");
        }
        if (req.getNgayBatDau() == null || req.getNgayKetThuc() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng thiết lập đầy đủ thời gian bắt đầu và kết thúc.");
        }
        if (req.getNgayKetThuc().isBefore(req.getNgayBatDau().plusMinutes(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời hạn chương trình phải có độ dài tối thiểu 5 phút.");
        }

        boolean isPercent = req.getLoai() != null &&
                (req.getLoai().equalsIgnoreCase("PERCENT") || req.getLoai().contains("%"));

        if (isPercent) {
            if (req.getGiaTri() == null || req.getGiaTri().compareTo(BigDecimal.ONE) < 0 || req.getGiaTri().compareTo(new BigDecimal("70")) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lỗi: Sale sản phẩm theo % chỉ được cấu hình từ 1% đến tối đa 70% (Quy định chống phá giá).");
            }
        } else {
            if (req.getGiaTri() == null || req.getGiaTri().compareTo(new BigDecimal("1000")) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mức giảm giá tối thiểu là 1.000 VNĐ.");
            }

            // Validate cấp độ Sản phẩm
            for (SanPham sp : danhSachSanPham) {
                if (sp.getChiTietSanPhams() == null || sp.getChiTietSanPhams().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Sản phẩm '" + sp.getTen() + "' chưa có biến thể (SKU) nào nên không có giá gốc để đối chiếu. Vui lòng bổ sung SKU trước khi áp dụng Sale!");
                }

                BigDecimal minPrice = sp.getChiTietSanPhams().stream()
                        .map(ct -> ct.getGiaBan() != null ? ct.getGiaBan() : BigDecimal.ZERO)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                if (minPrice.compareTo(BigDecimal.ZERO) == 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Sản phẩm '" + sp.getTen() + "' đang có biến thể giá 0đ, không thể áp dụng Khuyến mãi tiền mặt.");
                }

                BigDecimal maxSafeDiscount = minPrice.multiply(new BigDecimal("0.7"));

                if (req.getGiaTri().compareTo(maxSafeDiscount) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Lỗi rủi ro lỗ vốn: Mức giảm " + String.format("%,.0f", req.getGiaTri()) + "đ quá lớn so với sản phẩm '" + sp.getTen() +
                                    "' (Giá bán thấp nhất: " + String.format("%,.0f", minPrice) + "đ). Chỉ được giảm tối đa 70% giá gốc.");
                }
            }

            // Validate cấp độ SKU (Biến thể)
            if (danhSachSku != null) {
                for (SanPhamChiTiet sku : danhSachSku) {
                    BigDecimal giaBan = sku.getGiaBan() != null ? sku.getGiaBan() : BigDecimal.ZERO;

                    if (giaBan.compareTo(BigDecimal.ZERO) == 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Biến thể '" + sku.getSku() + "' đang có giá 0đ, không thể áp dụng Khuyến mãi tiền mặt.");
                    }

                    BigDecimal maxSafeDiscount = giaBan.multiply(new BigDecimal("0.7"));

                    if (req.getGiaTri().compareTo(maxSafeDiscount) > 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Lỗi rủi ro lỗ vốn: Mức giảm " + String.format("%,.0f", req.getGiaTri()) + "đ quá lớn so với biến thể '" + sku.getSku() +
                                        "' (Giá bán: " + String.format("%,.0f", giaBan) + "đ). Chỉ được giảm tối đa 70% giá gốc.");
                    }
                }
            }
        }
    }

    private void validateConflict(Integer currentId, PromotionRequest request) {
        Integer safeId = (currentId == null) ? -1 : currentId;
        boolean hasProducts = request.getIdSanPhams() != null && !request.getIdSanPhams().isEmpty();
        boolean hasSkus = request.getIdSanPhamChiTiets() != null && !request.getIdSanPhamChiTiets().isEmpty();

        if (!hasProducts && !hasSkus) {
            return;
        }

        // Kiểm tra trùng lịch cho cấp Sản phẩm
        if (hasProducts) {
            List<KhuyenMai> conflicts = khuyenMaiRepository.checkTrungLich(
                    request.getIdSanPhams(),
                    request.getNgayBatDau(),
                    request.getNgayKetThuc(),
                    safeId
            );

            if (!conflicts.isEmpty()) {
                KhuyenMai km = conflicts.get(0);
                String tenSpTrung = "các sản phẩm đã chọn";
                if (km.getSanPhams() != null && !km.getSanPhams().isEmpty()) {
                    tenSpTrung = km.getSanPhams().stream()
                            .filter(sp -> request.getIdSanPhams().contains(sp.getId()))
                            .map(SanPham::getTen)
                            .collect(Collectors.joining(", "));
                }

                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sản phẩm [" + tenSpTrung + "] đang thuộc đợt Sale: '" + km.getTen() +
                                "'. Một sản phẩm không được phép tham gia 2 đợt Khuyến mãi giảm giá cùng lúc!");
            }
        }

        // Kiểm tra trùng lịch cho cấp SKU (Cần đảm bảo hàm checkTrungLichSku đã có trong Repository)
        if (hasSkus) {
            List<KhuyenMai> skuConflicts = khuyenMaiRepository.checkTrungLichSku(
                    request.getIdSanPhamChiTiets(),
                    request.getNgayBatDau(),
                    request.getNgayKetThuc(),
                    safeId
            );

            if (!skuConflicts.isEmpty()) {
                KhuyenMai km = skuConflicts.get(0);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Một số biến thể sản phẩm (SKU) đã chọn đang thuộc đợt Sale: '" + km.getTen() +
                                "'. Một biến thể không được phép tham gia 2 đợt Khuyến mãi giảm giá cùng lúc!");
            }
        }
    }

    private boolean checkDaSuDung(Integer khuyenMaiId) {
        return false;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoUpdateStatus() {
        khuyenMaiRepository.autoDeactivateExpiredPromotions(LocalDateTime.now());
    }

    private String generateCode() {
        String timeStr = String.valueOf(System.currentTimeMillis());
        return "KM" + timeStr.substring(timeStr.length() - 8);
    }

    @GetMapping("/san-pham")
    public ResponseEntity<?> getAllProducts() {
        List<SanPham> list = sanPhamRepo.getAllActiveProducts();

        List<Map<String, Object>> result = list.stream().map(sp -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", sp.getId());
            item.put("ma", sp.getMa());
            item.put("ten", sp.getTen());

            // Lấy thêm danh sách SKU con và format tên phân loại (Màu sắc - Kích thước)
            List<Map<String, Object>> skus = new ArrayList<>();
            if (sp.getChiTietSanPhams() != null && !sp.getChiTietSanPhams().isEmpty()) {
                for (com.example.bee.entities.product.SanPhamChiTiet sku : sp.getChiTietSanPhams()) {
                    if (sku.getTrangThai() != null && sku.getTrangThai()) { // Chỉ lấy SKU đang bán
                        Map<String, Object> s = new HashMap<>();
                        s.put("id", sku.getId());
                        s.put("sku", sku.getSku());

                        // Xử lý an toàn tránh lỗi NullPointerException nếu thiếu màu/size
                        String mau = (sku.getMauSac() != null) ? sku.getMauSac().getTen() : "";
                        String size = (sku.getKichThuoc() != null) ? sku.getKichThuoc().getTen() : "";
                        String phanLoai = mau + (mau.isEmpty() || size.isEmpty() ? "" : " - ") + size;

                        if (phanLoai.trim().isEmpty()) {
                            phanLoai = "Mặc định";
                        }

                        s.put("ten", phanLoai);
                        skus.add(s);
                    }
                }
            }
            item.put("skus", skus); // Đính kèm mảng skus vào object sản phẩm trả về cho Frontend

            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping
    public Page<KhuyenMai> promoList(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) Boolean hinhThuc,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        String keyword = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        return khuyenMaiRepository.searchEverything(keyword, trangThai, from, to, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        KhuyenMai khuyenMai = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<KhuyenMaiSanPham> mappings = khuyenMaiSanPhamRepository.findAllByIdKhuyenMai(id);

        List<Integer> productIds = mappings.stream()
                .filter(m -> m.getIdSanPham() != null)
                .map(KhuyenMaiSanPham::getIdSanPham)
                .collect(Collectors.toList());

        List<Integer> skuIds = mappings.stream()
                .filter(m -> m.getIdSanPhamChiTiet() != null)
                .map(KhuyenMaiSanPham::getIdSanPhamChiTiet)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", khuyenMai);
        response.put("productIds", productIds);
        response.put("skuIds", skuIds);
        response.put("canUpdateStartDate", !checkDaSuDung(id));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody PromotionRequest body) {
        List<SanPham> danhSachSanPham = new ArrayList<>();
        if (body.getIdSanPhams() != null && !body.getIdSanPhams().isEmpty()) {
            danhSachSanPham = sanPhamRepo.findAllById(body.getIdSanPhams());
        }

        List<SanPhamChiTiet> danhSachSku = new ArrayList<>();
        if (body.getIdSanPhamChiTiets() != null && !body.getIdSanPhamChiTiets().isEmpty()) {
            danhSachSku = sanPhamChiTietRepo.findAllById(body.getIdSanPhamChiTiets());
        }

        validateChongLo(body, danhSachSanPham, danhSachSku);
        validateConflict(null, body);

        // Đảm bảo không tạo khuyến mãi trong quá khứ
        if (body.getNgayBatDau().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu không được đặt ở quá khứ.");
        }
        if (body.getNgayKetThuc().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc không được đặt ở quá khứ.");
        }

        String code = (body.getMa() != null && !body.getMa().trim().isEmpty())
                ? body.getMa().trim().toUpperCase()
                : generateCode();
        if (khuyenMaiRepository.existsByMa(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi đã tồn tại!");
        }

        KhuyenMai entity = new KhuyenMai();
        entity.setMa(code);
        mapToEntity(body, entity);
        KhuyenMai saved = khuyenMaiRepository.save(entity);

        saveProductsAndSkus(saved.getId(), body.getIdSanPhams(), body.getIdSanPhamChiTiets());

        try {
            java.util.List<com.example.bee.entities.account.TaiKhoan> khachHangs = taiKhoanRepository.findByVaiTro_Ma("ROLE_CUSTOMER");
            if (khachHangs != null && !khachHangs.isEmpty()) {
                java.util.List<com.example.bee.entities.notification.ThongBao> thongBaos = new java.util.ArrayList<>();
                for (com.example.bee.entities.account.TaiKhoan tk : khachHangs) {
                    com.example.bee.entities.notification.ThongBao tb = new com.example.bee.entities.notification.ThongBao();
                    tb.setTaiKhoanId(tk.getId());
                    tb.setTieuDe("Đợt Sale mới: " + saved.getTen());
                    tb.setNoiDung("Chương trình khuyến mãi giảm giá các mặt hàng đã bắt đầu. Nhanh tay săn sale kẻo lỡ!");
                    tb.setLoaiThongBao("VOUCHER");
                    tb.setDaDoc(false);
                    tb.setDaXoa(false);
                    thongBaos.add(tb);
                }
                thongBaoRepository.saveAll(thongBaos);
            }
        } catch (Exception e) {
            System.out.println("Lỗi gửi thông báo Sale: " + e.getMessage());
        }

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody PromotionRequest body) {
        KhuyenMai entity = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<SanPham> danhSachSanPham = new ArrayList<>();
        if (body.getIdSanPhams() != null && !body.getIdSanPhams().isEmpty()) {
            danhSachSanPham = sanPhamRepo.findAllById(body.getIdSanPhams());
        }

        List<SanPhamChiTiet> danhSachSku = new ArrayList<>();
        if (body.getIdSanPhamChiTiets() != null && !body.getIdSanPhamChiTiets().isEmpty()) {
            danhSachSku = sanPhamChiTietRepo.findAllById(body.getIdSanPhamChiTiets());
        }

        validateChongLo(body, danhSachSanPham, danhSachSku);
        validateConflict(id, body);

        // Đảm bảo cập nhật ngày kết thúc không nằm trong quá khứ
        if (body.getNgayKetThuc().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc không được đặt ở quá khứ.");
        }

        if (entity.getNgayBatDau().isBefore(LocalDateTime.now())) {
            if (!entity.getNgayBatDau().isEqual(body.getNgayBatDau())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Chương trình đã hoặc đang diễn ra, không thể thay đổi Ngày Bắt Đầu!");
            }
        } else {
            // Nếu chương trình chưa diễn ra, cho phép đổi ngày bắt đầu nhưng không được đặt trong quá khứ
            if (body.getNgayBatDau().isBefore(LocalDateTime.now().minusMinutes(1))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu không được đặt ở quá khứ.");
            }
        }

        mapToEntity(body, entity);
        KhuyenMai saved = khuyenMaiRepository.save(entity);

        // Xóa các liên kết cũ và thêm lại
        khuyenMaiSanPhamRepository.deleteByIdKhuyenMai(id);
        saveProductsAndSkus(saved.getId(), body.getIdSanPhams(), body.getIdSanPhamChiTiets());

        return ResponseEntity.ok(saved);
    }

    private void mapToEntity(PromotionRequest dto, KhuyenMai entity) {
        entity.setTen(dto.getTen().trim());
        entity.setLoai(dto.getLoai());
        entity.setGiaTri(dto.getGiaTri());
        entity.setNgayBatDau(dto.getNgayBatDau());
        entity.setNgayKetThuc(dto.getNgayKetThuc());
        entity.setChoPhepCongDon(dto.getChoPhepCongDon());
        entity.setTrangThai(dto.getTrangThai());
    }

    private void saveProductsAndSkus(Integer kmId, List<Integer> productIds, List<Integer> skuIds) {
        List<KhuyenMaiSanPham> listToSave = new ArrayList<>();

        if (productIds != null && !productIds.isEmpty()) {
            List<Integer> validProductIds = sanPhamRepo.findAllById(productIds).stream()
                    .map(SanPham::getId).collect(Collectors.toList());
            for (Integer pId : validProductIds) {
                KhuyenMaiSanPham kmsp = new KhuyenMaiSanPham();
                kmsp.setIdKhuyenMai(kmId);
                kmsp.setIdSanPham(pId);
                kmsp.setIdSanPhamChiTiet(null);
                listToSave.add(kmsp);
            }
        }

        if (skuIds != null && !skuIds.isEmpty()) {
            List<Integer> validSkuIds = sanPhamChiTietRepo.findAllById(skuIds).stream()
                    .map(SanPhamChiTiet::getId).collect(Collectors.toList());
            for (Integer sId : validSkuIds) {
                KhuyenMaiSanPham kmsp = new KhuyenMaiSanPham();
                kmsp.setIdKhuyenMai(kmId);
                kmsp.setIdSanPham(null);
                kmsp.setIdSanPhamChiTiet(sId);
                listToSave.add(kmsp);
            }
        }

        if (!listToSave.isEmpty()) {
            khuyenMaiSanPhamRepository.saveAll(listToSave);
        }
    }

    @PatchMapping("/{id}/trang-thai")
    @Transactional
    public ResponseEntity<?> quickToggleStatus(@PathVariable Integer id) {
        KhuyenMai entity = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin chương trình yêu cầu."));
        boolean newStatus = !entity.getTrangThai();
        String message;

        if (newStatus) {
            if (entity.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message",
                        "Không thể kích hoạt chương trình đã quá hạn thời gian kết thúc."));
            }

            List<KhuyenMaiSanPham> mappings = khuyenMaiSanPhamRepository.findAllByIdKhuyenMai(id);
            List<Integer> currentProductIds = mappings.stream()
                    .filter(m -> m.getIdSanPham() != null).map(KhuyenMaiSanPham::getIdSanPham).collect(Collectors.toList());
            List<Integer> currentSkuIds = mappings.stream()
                    .filter(m -> m.getIdSanPhamChiTiet() != null).map(KhuyenMaiSanPham::getIdSanPhamChiTiet).collect(Collectors.toList());

            boolean hasProducts = !currentProductIds.isEmpty();
            boolean hasSkus = !currentSkuIds.isEmpty();

            if (!hasProducts && !hasSkus) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message",
                        "Kích hoạt thất bại: Đợt khuyến mãi hiện chưa có sản phẩm hoặc biến thể nào áp dụng. Vui lòng bổ sung trước khi vận hành."));
            }

            long validCount = khuyenMaiRepository.countValidProductsInPromotion(id);
            long totalCount = currentProductIds.size();

            if (hasProducts && validCount == 0 && !hasSkus) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message",
                        "Kích hoạt thất bại. Toàn bộ sản phẩm được chọn hiện đang ở trạng thái ngừng hoạt động."));
            }
            if (hasProducts && validCount < totalCount) {
                long diff = totalCount - validCount;
                message = "Kích hoạt thành công. Lưu ý: Có " + diff + " sản phẩm đang ngừng kinh doanh sẽ không được áp dụng chiết khấu.";
            } else {
                message = "Chiến dịch khuyến mãi đã được kích hoạt thành công trên danh mục áp dụng.";
            }

            try {
                PromotionRequest fakeRequest = new PromotionRequest();
                fakeRequest.setTen(entity.getTen());
                fakeRequest.setIdSanPhams(currentProductIds);
                fakeRequest.setIdSanPhamChiTiets(currentSkuIds);
                fakeRequest.setNgayBatDau(entity.getNgayBatDau());
                fakeRequest.setNgayKetThuc(entity.getNgayKetThuc());
                fakeRequest.setChoPhepCongDon(entity.getChoPhepCongDon());
                fakeRequest.setTrangThai(true);
                fakeRequest.setLoai(entity.getLoai());
                fakeRequest.setGiaTri(entity.getGiaTri());

                List<SanPham> danhSachSanPham = new ArrayList<>();
                if (!currentProductIds.isEmpty()) danhSachSanPham = sanPhamRepo.findAllById(currentProductIds);

                List<SanPhamChiTiet> danhSachSku = new ArrayList<>();
                if (!currentSkuIds.isEmpty()) danhSachSku = sanPhamChiTietRepo.findAllById(currentSkuIds);

                validateChongLo(fakeRequest, danhSachSanPham, danhSachSku);
                validateConflict(id, fakeRequest);
            } catch (ResponseStatusException e) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getReason()));
            }

        } else {
            message = "Chương trình đã được chuyển sang trạng thái ngừng áp dụng.";
        }
        entity.setTrangThai(newStatus);
        khuyenMaiRepository.save(entity);
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }
}