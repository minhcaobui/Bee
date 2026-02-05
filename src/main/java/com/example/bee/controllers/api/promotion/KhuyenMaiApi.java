package com.example.bee.controllers.api.promotion;

import com.example.bee.dto.PromotionRequest;
import com.example.bee.entities.product.SanPham;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.entities.promotion.KhuyenMaiSanPham;
import com.example.bee.repositories.product.SanPhamRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import com.example.bee.repositories.promotion.KhuyenMaiSanPhamRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
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

    // --- LOGIC VALIDATE TRÙNG LỊCH & CỘNG DỒN (MỚI THÊM) ---
    private void validateConflict(Integer currentId, PromotionRequest request) {
        // Nếu tạo mới thì ID là -1 để không trùng ai
        Integer safeId = (currentId == null) ? -1 : currentId;

        // Gọi Repo kiểm tra xem có KM nào đang chạy đè lên không
        // LƯU Ý: Mày phải chắc chắn đã thêm hàm checkTrungLich vào KhuyenMaiRepository như tao bảo lúc nãy nhé!
        List<KhuyenMai> conflicts = khuyenMaiRepository.checkTrungLich(
                request.getIdSanPhams(),
                request.getNgayBatDau(),
                request.getNgayKetThuc(),
                safeId
        );

        for (KhuyenMai km : conflicts) {
            // Logic: Cả 2 phải cùng bật Cộng Dồn thì mới được
            boolean isBothCumulative = km.getChoPhepCongDon() && request.getChoPhepCongDon();

            if (!isBothCumulative) {
                // Lọc ra tên các sản phẩm bị trùng để báo lỗi
                // Đoạn này giả định trong Entity KhuyenMai mày có list SanPham (OneToMany/ManyToMany)
                // Nếu chưa map thì nó chỉ báo tên KM thôi.
                String tenSpTrung = "các sản phẩm đã chọn";
                if (km.getSanPhams() != null && !km.getSanPhams().isEmpty()) {
                    tenSpTrung = km.getSanPhams().stream()
                            .filter(sp -> request.getIdSanPhams().contains(sp.getId()))
                            .map(SanPham::getTen)
                            .collect(Collectors.joining(", "));
                }

                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sản phẩm [" + tenSpTrung + "] đang thuộc đợt KM: '" + km.getTen() +
                                "'. Yêu cầu cả 2 đợt phải bật 'Cộng dồn' mới được áp dụng!");
            }
        }
    }

    private boolean checkDaSuDung(Integer khuyenMaiId) {
        return false; // TẠM THỜI
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoUpdateStatus() {
        List<KhuyenMai> expiredList = khuyenMaiRepository.findByTrangThaiAndNgayKetThucBefore(true, LocalDateTime.now());
        if (!expiredList.isEmpty()) {
            expiredList.forEach(km -> km.setTrangThai(false));
            khuyenMaiRepository.saveAll(expiredList);
            System.out.println("Auto-Scheduler: Đã hủy " + expiredList.size() + " khuyến mãi hết hạn.");
        }
    }

    private String generateCode() {
        int randomNum = 100000 + new Random().nextInt(900000);
        return "KM" + randomNum;
    }

    @GetMapping("/san-pham")
    public ResponseEntity<?> getAllProducts() {
        List<SanPham> list = sanPhamRepo.getAllActiveProducts();

        List<Map<String, Object>> result = list.stream().map(sp -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", sp.getId());
            item.put("ma", sp.getMa());
            item.put("ten", sp.getTen());
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
        List<Integer> productIds = khuyenMaiSanPhamRepository.findAllByIdKhuyenMai(id)
                .stream().map(KhuyenMaiSanPham::getIdSanPham).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", khuyenMai);
        response.put("productIds", productIds);
        response.put("canUpdateStartDate", !checkDaSuDung(id));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody PromotionRequest body) {
        validate(body);

        // 1. Check Trùng Lịch & Cộng Dồn (GỌI HÀM VALIDATE Ở ĐÂY)
        validateConflict(null, body);

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
        saveProducts(saved.getId(), body.getIdSanPhams());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody PromotionRequest body) {
        KhuyenMai entity = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        validate(body);

        // 2. Check Trùng Lịch & Cộng Dồn (GỌI HÀM VALIDATE Ở ĐÂY)
        validateConflict(id, body);

        if (!entity.getNgayBatDau().isEqual(body.getNgayBatDau())) {
            if (checkDaSuDung(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mã này đã có người sử dụng, không thể sửa Ngày Bắt Đầu!");
            }
        }
        mapToEntity(body, entity);
        KhuyenMai saved = khuyenMaiRepository.save(entity);
        khuyenMaiSanPhamRepository.deleteByIdKhuyenMai(id);
        saveProducts(saved.getId(), body.getIdSanPhams());
        return ResponseEntity.ok(saved);
    }

    private void validate(PromotionRequest body) {
        if (body.getTen() == null || body.getTen().trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên không được để trống!");

        if ("PERCENT".equals(body.getLoai())) {
            if (body.getGiaTri() == null || body.getGiaTri().doubleValue() <= 0 || body.getGiaTri().doubleValue() > 80)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phần trăm phải từ 1-80%!");
        } else {
            if (body.getGiaTri() == null || body.getGiaTri().compareTo(new BigDecimal("1000")) < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền giảm phải từ 1,000đ!");
        }

        if (body.getNgayKetThuc().isBefore(body.getNgayBatDau().plusMinutes(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian khuyến mãi phải dài ít nhất 5 phút!");
        }
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

    private void saveProducts(Integer kmId, List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        // Lọc sản phẩm tồn tại để tránh lỗi Foreign Key
        List<Integer> validIds = sanPhamRepo.findAllById(productIds).stream()
                .map(SanPham::getId).collect(Collectors.toList());

        List<KhuyenMaiSanPham> list = validIds.stream()
                .map(spId -> new KhuyenMaiSanPham(null, kmId, spId))
                .collect(Collectors.toList());
        if (!list.isEmpty()) khuyenMaiSanPhamRepository.saveAll(list);
    }

    @PatchMapping("/{id}/trang-thai")
    @Transactional
    public ResponseEntity<?> quickToggleStatus(@PathVariable Integer id) {
        KhuyenMai entity = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean newStatus = !entity.getTrangThai();
        String message = newStatus ? "Kích hoạt chương trình thành công!" : "Đã ngưng kích hoạt chương trình!";

        // NẾU ĐANG ĐỊNH BẬT (ON)
        if (newStatus) {
            // ... (Giữ nguyên các đoạn check Hết hạn & Valid == 0 ở trên) ...
            if (entity.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chương trình đã hết hạn!");
            }

            long validCount = khuyenMaiRepository.countValidProductsInPromotion(id);
            long totalCount = entity.getSanPhams().size();

            // Check 1: Chết sạch sành sanh -> Cấm bật
            if (validCount == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không thể kích hoạt! Tất cả sản phẩm trong chương trình này đều không khả dụng do Danh mục, Hãng hoặc Chất liệu đang bị tắt.");
            }

            // Check 2: Chết một vài em (A, B chết, C sống) -> Cho bật nhưng CẢNH BÁO
            if (validCount < totalCount) {
                long diff = totalCount - validCount;
                message = "Kích hoạt thành công! ⚠️ Lưu ý: Có " + diff + " sản phẩm sẽ không được áp dụng do Danh mục, Hãng hoặc Chất liệu đang bị tắt.";
            }

            // ... (Giữ nguyên đoạn check trùng đợt validateConflict) ...
            PromotionRequest fakeRequest = new PromotionRequest();
            fakeRequest.setIdSanPhams(entity.getSanPhams().stream().map(SanPham::getId).collect(Collectors.toList()));
            fakeRequest.setNgayBatDau(entity.getNgayBatDau());
            fakeRequest.setNgayKetThuc(entity.getNgayKetThuc());
            fakeRequest.setChoPhepCongDon(entity.getChoPhepCongDon());
            fakeRequest.setTrangThai(true);
            validateConflict(id, fakeRequest);
        }

        entity.setTrangThai(newStatus);
        khuyenMaiRepository.save(entity);

        // --- QUAN TRỌNG: Trả về cục JSON chứa message ---
        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }
    @Scheduled(fixedRate = 60000) // Chạy mỗi phút
    @Transactional
    public void autoUpdateSaleStatus() {
        // Tìm các đợt Sale đang Bật mà Ngày kết thúc < Hiện tại
        List<KhuyenMai> expiredSales = khuyenMaiRepository.findAll().stream()
                .filter(km -> km.getTrangThai() && km.getNgayKetThuc().isBefore(LocalDateTime.now()))
                .collect(Collectors.toList());

        if (!expiredSales.isEmpty()) {
            expiredSales.forEach(km -> km.setTrangThai(false));
            khuyenMaiRepository.saveAll(expiredSales);
            System.out.println("Auto-Scheduler: Đã tắt " + expiredSales.size() + " đợt Sale hết hạn.");
        }
    }
}