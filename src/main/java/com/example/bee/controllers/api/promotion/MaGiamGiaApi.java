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
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(voucherRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Voucher")));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody MaGiamGia body) {
        if (body.getMaCode() == null || body.getMaCode().trim().isEmpty()) {
            body.setMaCode(generateCode());
        } else {
            body.setMaCode(body.getMaCode().trim().toUpperCase());
            if (voucherRepo.existsByMaCodeIgnoreCase(body.getMaCode())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã Voucher này đã tồn tại!");
            }
        }
        validateVoucher(body);
        body.setLuotSuDung(0);
        if (body.getTrangThai() == null) body.setTrangThai(true);
        if (body.getChoPhepCongDon() == null) body.setChoPhepCongDon(false);
        return ResponseEntity.ok(voucherRepo.save(body));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody MaGiamGia body) {
        MaGiamGia entity = voucherRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin mã giảm giá yêu cầu."));
        validateVoucher(body);
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

    @PatchMapping("/{id}/trang-thai")
    @Transactional
    public ResponseEntity<?> quickToggle(@PathVariable Integer id) {
        MaGiamGia entity = voucherRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin mã giảm giá yêu cầu."));
        boolean newStatus = !entity.getTrangThai();
        String message;
        if (newStatus) {
            if (entity.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích hoạt thất bại. Mã giảm giá đã quá hạn thời gian sử dụng.");
            }
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

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoUpdateVoucherStatus() {
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
}