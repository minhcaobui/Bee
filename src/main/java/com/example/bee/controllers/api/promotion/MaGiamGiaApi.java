package com.example.bee.controllers.api.promotion;

import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
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
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class MaGiamGiaApi {

    private final MaGiamGiaRepository voucherRepo;

    // --- 1. LẤY DANH SÁCH (CÓ LỌC) ---
    @GetMapping
    public Page<MaGiamGia> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        String keyword = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        return voucherRepo.searchVoucher(keyword, trangThai, from, to, pageable);
    }

    // --- 2. CHI TIẾT ---
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(voucherRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Voucher")));
    }

    // --- 3. TẠO MỚI ---
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody MaGiamGia body) {
        // Sinh mã nếu để trống
        if (body.getMaCode() == null || body.getMaCode().trim().isEmpty()) {
            body.setMaCode(generateCode());
        } else {
            body.setMaCode(body.getMaCode().trim().toUpperCase());
            // Check trùng mã
            if (voucherRepo.existsByMaCodeIgnoreCase(body.getMaCode())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã Voucher này đã tồn tại!");
            }
        }

        // Validate dữ liệu cực căng
        validateVoucher(body);

        body.setLuotSuDung(0);
        if (body.getTrangThai() == null) body.setTrangThai(true);
        if (body.getChoPhepCongDon() == null) body.setChoPhepCongDon(false);

        return ResponseEntity.ok(voucherRepo.save(body));
    }

    // --- 4. CẬP NHẬT ---
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody MaGiamGia body) {
        MaGiamGia entity = voucherRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Validate dữ liệu
        validateVoucher(body);

        // Không cho sửa Mã Code, không reset Lượt sử dụng
        entity.setTen(body.getTen().trim());
        entity.setLoaiGiamGia(body.getLoaiGiamGia());
        entity.setGiaTriGiamGia(body.getGiaTriGiamGia());
        entity.setGiaTriGiamGiaToiDa(body.getGiaTriGiamGiaToiDa());
        entity.setDieuKien(body.getDieuKien());
        entity.setSoLuong(body.getSoLuong());
        entity.setNgayBatDau(body.getNgayBatDau());
        entity.setNgayKetThuc(body.getNgayKetThuc());
        entity.setChoPhepCongDon(body.getChoPhepCongDon());
        entity.setTrangThai(body.getTrangThai());

        return ResponseEntity.ok(voucherRepo.save(entity));
    }

    // --- 5. ĐỔI TRẠNG THÁI NHANH (SỬA LẠI ĐỂ CHECK SỐ LƯỢNG) ---
    @PatchMapping("/{id}/trang-thai")
    @Transactional
    public ResponseEntity<?> quickToggle(@PathVariable Integer id) {
        MaGiamGia entity = voucherRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean newStatus = !entity.getTrangThai();

        // NẾU ĐANG ĐỊNH BẬT (ON)
        if (newStatus) {
            // 1. Check Hết hạn
            if (entity.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher đã hết hạn, không thể kích hoạt!");
            }

            // 2. CHECK SỐ LƯỢNG (Mới thêm)
            if (entity.getLuotSuDung() >= entity.getSoLuong()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Voucher này đã hết lượt sử dụng (" + entity.getLuotSuDung() + "/" + entity.getSoLuong() + "). Vui lòng tăng số lượng trước khi bật!");
            }
        }

        entity.setTrangThai(newStatus);
        voucherRepo.save(entity);

        return ResponseEntity.ok().build();
    }

    // --- HÀM VALIDATE RIÊNG (CHẶT CHẼ) ---
    private void validateVoucher(MaGiamGia body) {
        // 1. Tên
        if (body.getTen() == null || body.getTen().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên Voucher không được để trống!");
        }

        // 2. Thời gian (Ngày giờ phút giây)
        if (body.getNgayBatDau() == null || body.getNgayKetThuc() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian không được để trống!");
        }
        if (body.getNgayKetThuc().isBefore(body.getNgayBatDau().plusMinutes(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu ít nhất 5 phút!");
        }

        // 3. Số lượng
        if (body.getSoLuong() == null || body.getSoLuong() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng voucher phải lớn hơn 0!");
        }

        // 4. Giá trị giảm & Loại giảm
        if ("PERCENTAGE".equals(body.getLoaiGiamGia())) {
            // Nếu là %
            if (body.getGiaTriGiamGia().compareTo(BigDecimal.valueOf(80)) > 0 || body.getGiaTriGiamGia().compareTo(BigDecimal.ONE) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phần trăm giảm phải từ 1% đến 80%!");
            }
            if (body.getGiaTriGiamGiaToiDa() == null || body.getGiaTriGiamGiaToiDa().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Với loại giảm %, vui lòng nhập số tiền giảm Tối Đa hợp lệ!");
            }
        } else {
            // Nếu là Tiền mặt (FIXED)
            if (body.getGiaTriGiamGia().compareTo(BigDecimal.valueOf(1000)) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị giảm giá phải từ 1,000 VNĐ trở lên!");
            }
            // Reset max giảm về null cho đỡ rác
            body.setGiaTriGiamGiaToiDa(null);
        }

        // 5. Điều kiện đơn hàng
        if (body.getDieuKien() == null || body.getDieuKien().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị đơn hàng tối thiểu không hợp lệ!");
        }
    }

    private String generateCode() {
        int randomNum = 10000 + new Random().nextInt(90000);
        return "VC" + randomNum;
    }

    @Scheduled(fixedRate = 60000) // Chạy mỗi 60 giây (1 phút)
    @Transactional
    public void autoUpdateVoucherStatus() {
        // 1. Quét các Voucher đang BẬT nhưng đã HẾT HẠN hoặc HẾT SỐ LƯỢNG
        // Logic: (Trạng thái = True) VÀ (Ngày kết thúc < Giờ hiện tại HOẶC Số lượng <= Lượt sử dụng)
        List<MaGiamGia> expiredVouchers = voucherRepo.findAll().stream()
                .filter(v -> v.getTrangThai() && (
                        v.getNgayKetThuc().isBefore(LocalDateTime.now()) ||
                                v.getLuotSuDung() >= v.getSoLuong()
                ))
                .collect(Collectors.toList());

        if (!expiredVouchers.isEmpty()) {
            expiredVouchers.forEach(v -> v.setTrangThai(false));
            voucherRepo.saveAll(expiredVouchers);
            System.out.println("Auto-Scheduler: Đã tắt " + expiredVouchers.size() + " voucher hết hạn/hết số lượng.");
        }
    }

    @PostMapping("/use-test")
    @Transactional
    public ResponseEntity<?> useTestVoucher(@RequestParam String code, @RequestParam BigDecimal orderValue) {
        // 1. Tìm voucher
        MaGiamGia voucher = voucherRepo.findAll().stream()
                .filter(v -> v.getMaCode().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã không tồn tại!"));

        // --- CHECK TRƯỚC (QUAN TRỌNG) ---
        // Phải check xem nó có hợp lệ không đã rồi mới dám trừ

        // 2. Check Trạng thái
        if (!voucher.getTrangThai()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher này đang bị khóa!");
        }

        // 3. Check Thời gian
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher chưa đến giờ dùng!");
        }
        if (voucher.getNgayKetThuc().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher đã hết hạn!");
        }

        // 4. Check Số lượng (Phải check TRƯỚC khi cộng)
        if (voucher.getLuotSuDung() >= voucher.getSoLuong()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher đã hết lượt sử dụng!");
        }

        // 5. Check Điều kiện đơn hàng
        if (orderValue.compareTo(voucher.getDieuKien()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đơn hàng chưa đủ điều kiện! Cần tối thiểu " + voucher.getDieuKien() + "đ");
        }

        // --- SAU KHI QUA HẾT CÁC CỬA ẢI THÌ MỚI TRỪ ---
        int luotDungMoi = voucher.getLuotSuDung() + 1;
        voucher.setLuotSuDung(luotDungMoi);

        // Logic tự động tắt nếu full (như bạn muốn)
        if (luotDungMoi >= voucher.getSoLuong()) {
            voucher.setTrangThai(false);
        }

        voucherRepo.save(voucher);

        return ResponseEntity.ok("Áp dụng thành công! (Đã trừ 1 lượt dùng)");
    }
}