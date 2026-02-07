package com.example.bee.controllers.api.promotion;

import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin mã giảm giá yêu cầu."));

        validateVoucher(body);

        // LOGIC KHÓA FIELD CHUYÊN NGHIỆP: Nếu đã có lượt dùng, không cho sửa giá trị và loại
        if (entity.getLuotSuDung() > 0) {
            if (!entity.getGiaTriGiamGia().equals(body.getGiaTriGiamGia()) ||
                    !entity.getLoaiGiamGia().equals(body.getLoaiGiamGia())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mã giảm giá đã phát sinh lượt sử dụng thực tế, không thể thay đổi định mức chiết khấu.");
            }
        }

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin mã giảm giá yêu cầu."));

        boolean newStatus = !entity.getTrangThai();
        String message;

        if (newStatus) {
            // Kiểm tra thời hạn hiệu lực
            if (entity.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích hoạt thất bại. Mã giảm giá đã quá hạn thời gian sử dụng.");
            }

            // Kiểm tra định mức sử dụng
            if (entity.getLuotSuDung() >= entity.getSoLuong()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Kích hoạt thất bại. Mã giảm giá đã đạt giới hạn định mức phát hành (" + entity.getLuotSuDung() + "/" + entity.getSoLuong() + ").");
            }
            message = "Mã giảm giá đã được đưa vào trạng thái hoạt động.";
        } else {
            message = "Mã giảm giá đã được chuyển sang trạng thái tạm ngừng áp dụng.";
        }

        entity.setTrangThai(newStatus);
        voucherRepo.save(entity);

        return ResponseEntity.ok(Collections.singletonMap("message", message));
    }

    // --- HÀM VALIDATE RIÊNG (CHẶT CHẼ) ---
    private void validateVoucher(MaGiamGia body) {
        if (body.getTen() == null || body.getTen().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên hiển thị mã giảm giá không được để trống.");
        }

        if (body.getNgayBatDau() == null || body.getNgayKetThuc() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng thiết lập đầy đủ thời gian bắt đầu và kết thúc.");
        }

        if (body.getNgayKetThuc().isBefore(body.getNgayBatDau().plusMinutes(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời hạn sử dụng mã phải có độ dài tối thiểu 5 phút.");
        }

        if (body.getSoLuong() == null || body.getSoLuong() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng phát hành tối thiểu phải từ 1 mã trở lên.");
        }

        if ("PERCENTAGE".equals(body.getLoaiGiamGia())) {
            if (body.getGiaTriGiamGia().compareTo(BigDecimal.valueOf(80)) > 0 || body.getGiaTriGiamGia().compareTo(BigDecimal.ONE) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tỷ lệ chiết khấu không hợp lệ (Yêu cầu từ 1% - 80%).");
            }
            if (body.getGiaTriGiamGiaToiDa() == null || body.getGiaTriGiamGiaToiDa().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng thiết lập hạn mức giảm tối đa cho loại hình chiết khấu theo %.");
            }
        } else {
            if (body.getGiaTriGiamGia().compareTo(BigDecimal.valueOf(1000)) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị chiết khấu tối thiểu phải từ 1.000 VNĐ.");
            }

            // Chống xung đột: Giá trị giảm không được vượt quá 50% đơn tối thiểu
            if (body.getDieuKien() != null && body.getDieuKien().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal limit = body.getDieuKien().multiply(new BigDecimal("0.5"));
                if (body.getGiaTriGiamGia().compareTo(limit) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Để đảm bảo ngân sách, mức giảm không được vượt quá 50% giá trị đơn tối thiểu.");
                }
            }
            body.setGiaTriGiamGiaToiDa(null);
        }

        if (body.getDieuKien() == null || body.getDieuKien().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị đơn hàng tối thiểu không hợp lệ.");
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

//    @PostMapping("/use-test")
//    @Transactional
//    public ResponseEntity<?> useTestVoucher(@RequestParam String code, @RequestParam BigDecimal orderValue) {
//        // 1. Tìm voucher
//        MaGiamGia voucher = voucherRepo.findAll().stream()
//                .filter(v -> v.getMaCode().equalsIgnoreCase(code))
//                .findFirst()
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mã không tồn tại!"));
//
//        // --- CHECK TRƯỚC (QUAN TRỌNG) ---
//        // Phải check xem nó có hợp lệ không đã rồi mới dám trừ
//
//        // 2. Check Trạng thái
//        if (!voucher.getTrangThai()) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher này đang bị khóa!");
//        }
//
//        // 3. Check Thời gian
//        LocalDateTime now = LocalDateTime.now();
//        if (voucher.getNgayBatDau().isAfter(now)) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher chưa đến giờ dùng!");
//        }
//        if (voucher.getNgayKetThuc().isBefore(now)) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher đã hết hạn!");
//        }
//
//        // 4. Check Số lượng (Phải check TRƯỚC khi cộng)
//        if (voucher.getLuotSuDung() >= voucher.getSoLuong()) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher đã hết lượt sử dụng!");
//        }
//
//        // 5. Check Điều kiện đơn hàng
//        if (orderValue.compareTo(voucher.getDieuKien()) < 0) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                    "Đơn hàng chưa đủ điều kiện! Cần tối thiểu " + voucher.getDieuKien() + "đ");
//        }
//
//        // --- SAU KHI QUA HẾT CÁC CỬA ẢI THÌ MỚI TRỪ ---
//        int luotDungMoi = voucher.getLuotSuDung() + 1;
//        voucher.setLuotSuDung(luotDungMoi);
//
//        // Logic tự động tắt nếu full (như bạn muốn)
//        if (luotDungMoi >= voucher.getSoLuong()) {
//            voucher.setTrangThai(false);
//        }
//
//        voucherRepo.save(voucher);
//
//        return ResponseEntity.ok("Áp dụng thành công! (Đã trừ 1 lượt dùng)");
//    }
}