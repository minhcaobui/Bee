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
import java.time.Duration;
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

    private boolean checkDaSuDung(Integer khuyenMaiId) {
        // TẠM THỜI: Trả về false
        return false;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoUpdateStatus() {
        List<KhuyenMai> expiredList = khuyenMaiRepository.findByTrangThaiAndNgayKetThucBefore(true, LocalDateTime.now());
        if (!expiredList.isEmpty()) {
            for (KhuyenMai km : expiredList) {
                km.setTrangThai(false);
            }
            khuyenMaiRepository.saveAll(expiredList);
            System.out.println("Auto-Scheduler: Đã hủy " + expiredList.size() + " khuyến mãi hết hạn.");
        }
    }

    private String generateCode() {
        int randomNum = 100000 + new Random().nextInt(900000);
        return "KM" + randomNum; // Ra mã KM183618
    }

    @GetMapping("/san-pham")
    public ResponseEntity<?> getAllProducts() {
        List<SanPham> list = sanPhamRepo.findAll();
        List<Map<String, Object>> result = list.stream().map(sp -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", sp.getId());
            item.put("ma", sp.getMa());
            item.put("ten", sp.getTen());
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // --- 1. ĐÃ FIX: TRUYỀN ĐỦ THAM SỐ hinhThuc XUỐNG REPO ---
    @GetMapping
    public Page<KhuyenMai> promoList(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) Boolean hinhThuc, // <--- Param lọc tab
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        String keyword = (q != null && !q.trim().isEmpty()) ? q.trim() : null;

        // Gọi hàm searchEverything chuẩn bên Repo
        return khuyenMaiRepository.searchEverything(keyword, trangThai, hinhThuc, from, to, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        KhuyenMai khuyenMai = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Integer> productIds = khuyenMaiSanPhamRepository.findAllByIdKhuyenMai(id)
                .stream().map(KhuyenMaiSanPham::getIdSanPham).collect(Collectors.toList());
        boolean isUsed = checkDaSuDung(id);
        Map<String, Object> response = new HashMap<>();
        response.put("data", khuyenMai);
        response.put("productIds", productIds);
        response.put("canUpdateStartDate", !isUsed);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody PromotionRequest body) {
        validate(body);

        String codeToCheck = body.getMa();
        if (codeToCheck != null && !codeToCheck.trim().isEmpty()) {
            codeToCheck = codeToCheck.trim();
            if (codeToCheck.length() > 20)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã không được quá 20 ký tự!");
            if (khuyenMaiRepository.existsByMa(codeToCheck)) {
                // Sửa thành BAD_REQUEST để dễ bắt lỗi bên Frontend
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi này đã tồn tại!");
            }
        } else {
            codeToCheck = generateCode();
        }

        KhuyenMai entity = new KhuyenMai();
        entity.setMa(codeToCheck);
        entity.setDaSuDung(0);
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
        // 1. Validate Tên
        if (body.getTen() == null || body.getTen().trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên chương trình không được để trống!");
        if (body.getTen().trim().length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên không được quá 100 ký tự!");

        // 2. Validate Mã (Nếu admin tự nhập)
        if (body.getMa() != null && !body.getMa().trim().isEmpty()) {
            String code = body.getMa().trim();
            if (!code.matches("^[A-Z0-9]+$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã chỉ được dùng chữ cái không dấu và số!");
            }
            if (code.length() > 20)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã không được quá 20 ký tự!");
        }

        // 3. Validate Số lượng
        if (body.getSoLuong() == null || body.getSoLuong() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng phải lớn hơn 0!");


        // 5. Validate Giá trị và Giảm tối đa
        if ("PERCENT".equals(body.getLoai())) {
            if (body.getGiaTri() == null || body.getGiaTri().doubleValue() <= 0 || body.getGiaTri().doubleValue() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phần trăm giảm phải từ 1 đến 100%!");
            }
            if (body.getGiamToiDa() == null || body.getGiamToiDa().compareTo(new BigDecimal("1000")) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị giảm tối đa phải từ 1,000 VND!");
            }
        } else { // Loại AMOUNT (Tiền mặt)
            if (body.getGiaTri() == null || body.getGiaTri().compareTo(new BigDecimal("1000")) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền giảm phải từ 1,000 VND!");
            }
        }

        // 6. Validate Ngày giờ
        LocalDateTime start = body.getNgayBatDau();
        LocalDateTime end = body.getNgayKetThuc();
        LocalDateTime now = LocalDateTime.now();

        if (start == null || end == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn đầy đủ ngày bắt đầu và kết thúc!");

        // Chỉ check ngày bắt đầu khi tạo mới (id == null)
        // Nếu là update, có thể bỏ qua để tránh lỗi khi khuyến mãi đang diễn ra
        // if (body.getId() == null && start.isBefore(now.minusSeconds(60))) {
        //    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu không được nhỏ hơn thời gian hiện tại!");
        // }

        if (!end.isAfter(start))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu!");

        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 5)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian khuyến mãi phải kéo dài ít nhất 5 phút!");

        // Check kích hoạt khuyến mãi đã hết hạn
        if (Boolean.TRUE.equals(body.getTrangThai()) && end.isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể kích hoạt khuyến mãi đã kết hạn!");
        }
    }

    // --- 2. ĐÃ FIX: MAP THÊM CÁC TRƯỜNG MỚI (DieuKien, GiamToiDa) ---
    private void mapToEntity(PromotionRequest dto, KhuyenMai entity) {
        entity.setTen(dto.getTen().trim());
        entity.setLoai(dto.getLoai());
        entity.setGiaTri(dto.getGiaTri());
        entity.setSoLuong(dto.getSoLuong());
        entity.setHinhThuc(dto.getHinhThuc());
        entity.setNgayBatDau(dto.getNgayBatDau());
        entity.setNgayKetThuc(dto.getNgayKetThuc());
        entity.setTrangThai(dto.getTrangThai());
        entity.setChoPhepCongDon(false);

        // Map 2 trường mới này vào Entity
        entity.setDieuKienToiThieu(dto.getDieuKienToiThieu() != null ? dto.getDieuKienToiThieu() : BigDecimal.ZERO);

        if ("PERCENT".equals(dto.getLoai())) {
            entity.setGiamToiDa(dto.getGiamToiDa());
        } else {
            entity.setGiamToiDa(null); // Nếu là AMOUNT thì xóa Max đi
        }
    }

    private void saveProducts(Integer kmId, List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        List<Integer> validIds = sanPhamRepo.findAllById(productIds).stream()
                .map(SanPham::getId).collect(Collectors.toList());
        List<KhuyenMaiSanPham> list = validIds.stream()
                .map(spId -> new KhuyenMaiSanPham(null, kmId, spId))
                .collect(Collectors.toList());
        if (!list.isEmpty()) khuyenMaiSanPhamRepository.saveAll(list);
    }
}