package com.example.bee.controllers.api.promotion;

import com.example.bee.constants.LoaiGiamGia;
import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.notification.ThongBao;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class MaGiamGiaApi {

    private final MaGiamGiaRepository voucherRepo;
    private final ThongBaoRepository thongBaoRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    // 🌟 ĐÃ CẤU TRÚC LẠI HÀM VALIDATE ĐỂ TRÁNH LỖI KHI NHẬP %
    private void validateVoucherLogic(MaGiamGia body) {
        // 1. Kiểm tra thông tin cơ bản
        if (body.getNgayBatDau() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn Ngày bắt đầu.");
        }
        if (body.getNgayKetThuc() != null && body.getNgayBatDau().isAfter(body.getNgayKetThuc())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lỗi: Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if (body.getNgayKetThuc() != null && body.getNgayKetThuc().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lỗi: Ngày kết thúc không được nằm trong quá khứ.");
        }
        if (body.getSoLuong() == null || body.getSoLuong() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lỗi: Số lượng mã giảm giá phải lớn hơn 0.");
        }
        if (body.getDieuKien() == null || body.getDieuKien().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lỗi: Điều kiện đơn hàng tối thiểu không hợp lệ.");
        }
        if (body.getGiaTriGiamGia() == null || body.getGiaTriGiamGia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lỗi: Giá trị giảm giá phải lớn hơn 0.");
        }

        // 2. Phân tách logic kiểm tra rủi ro theo loại giảm giá
        boolean isPercent = body.getLoaiGiamGia() != null &&
                (body.getLoaiGiamGia().equalsIgnoreCase(LoaiGiamGia.PHAN_TRAM) || body.getLoaiGiamGia().contains("%"));

        if (isPercent) {
            // ----- KIỂM TRA CHO GIẢM % -----
            if (body.getGiaTriGiamGia().compareTo(BigDecimal.ONE) < 0 || body.getGiaTriGiamGia().compareTo(new BigDecimal("100")) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lỗi: Mức giảm phần trăm chỉ được phép từ 1% đến 100% để tránh bán dưới giá vốn.");
            }
            if (body.getGiaTriGiamGiaToiDa() == null || body.getGiaTriGiamGiaToiDa().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lỗi rủi ro cực cao: Khuyến mãi theo % BẮT BUỘC phải thiết lập 'Mức giảm tối đa (VNĐ)' để tránh khách mua đơn hàng lớn và trừ âm tiền cửa hàng.");
            }
        } else {
            // ----- KIỂM TRA CHO GIẢM TIỀN MẶT -----
            if (body.getGiaTriGiamGia().compareTo(new BigDecimal("1000")) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lỗi: Mức giảm tiền mặt tối thiểu là 1.000 VNĐ.");
            }

            if (body.getDieuKien().compareTo(BigDecimal.ZERO) > 0) {
                // Tiền giảm không được vượt quá 50% của điều kiện đơn tối thiểu
                BigDecimal maxSafeDiscount = body.getDieuKien().multiply(new BigDecimal("0.5"));
                if (body.getGiaTriGiamGia().compareTo(maxSafeDiscount) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Lỗi rủi ro lỗ: Với đơn tối thiểu " + String.format("%,.0f", body.getDieuKien()) + "đ, mức giảm tiền mặt không được vượt quá 100% (Tối đa " + String.format("%,.0f", maxSafeDiscount) + "đ).");
                }
            } else {
                // Nếu là voucher 0đ (Ai cũng xài được) thì chỉ cho giảm tối đa 50k
                if (body.getGiaTriGiamGia().compareTo(new BigDecimal("50000")) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lỗi: Voucher áp dụng cho mọi đơn hàng (0đ) chỉ được phép giảm tối đa 50.000 VNĐ để chống gian lận bào mã.");
                }
            }

            // Gán lại GiaTriGiamGiaToiDa bằng đúng số tiền giảm mặt định (Vì giảm tiền mặt thì max cũng là số đó)
            body.setGiaTriGiamGiaToiDa(body.getGiaTriGiamGia());
        }
    }

    // Hàm dùng chung để gửi thông báo cho khách hàng
    private void sendVoucherNotification(MaGiamGia voucher) {
        try {
            List<TaiKhoan> khachHangs = taiKhoanRepository.findByVaiTro_Ma("ROLE_CUSTOMER");
            if (khachHangs != null && !khachHangs.isEmpty()) {
                List<ThongBao> thongBaos = new ArrayList<>();
                for (TaiKhoan tk : khachHangs) {
                    ThongBao tb = new ThongBao();
                    tb.setTaiKhoanId(tk.getId());
                    tb.setTieuDe("Mã giảm giá mới dành cho bạn!");
                    tb.setNoiDung("Mã ưu đãi " + voucher.getMaCode() + " đã có sẵn. Nhập mã ngay lúc thanh toán để được giảm giá nhé!");
                    tb.setLoaiThongBao("VOUCHER");
                    tb.setDaDoc(false);
                    tb.setDaXoa(false);
                    thongBaos.add(tb);
                }
                thongBaoRepository.saveAll(thongBaos);
            }
        } catch (Exception e) {
            System.out.println("Lỗi gửi thông báo Voucher: " + e.getMessage());
        }
    }

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
        // GỌI HÀM VALIDATE
        validateVoucherLogic(body);

        if (body.getMaCode() == null || body.getMaCode().trim().isEmpty()) {
            body.setMaCode(generateCode());
        } else {
            body.setMaCode(body.getMaCode().trim().toUpperCase());
            if (voucherRepo.existsByMaCodeIgnoreCase(body.getMaCode())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã Voucher này đã tồn tại!");
            }
        }

        body.setLuotSuDung(0);
        if (body.getTrangThai() == null) body.setTrangThai(true);
        if (body.getChoPhepCongDon() == null) body.setChoPhepCongDon(false);

        MaGiamGia savedVoucher = voucherRepo.save(body);

        // Chỉ gửi thông báo nếu voucher có trạng thái đang hoạt động (true)
        if (Boolean.TRUE.equals(savedVoucher.getTrangThai())) {
            sendVoucherNotification(savedVoucher);
        }

        return ResponseEntity.ok(savedVoucher);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody MaGiamGia body) {
        validateVoucherLogic(body);

        MaGiamGia entity = voucherRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin mã giảm giá yêu cầu."));

        if (entity.getLuotSuDung() > 0) {
            if (entity.getGiaTriGiamGia().compareTo(body.getGiaTriGiamGia()) != 0 ||
                    !entity.getLoaiGiamGia().equals(body.getLoaiGiamGia())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mã giảm giá đã phát sinh lượt sử dụng thực tế, không thể thay đổi loại hoặc định mức chiết khấu.");
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

        MaGiamGia updatedVoucher = voucherRepo.save(entity);

        // Gửi thông báo nếu voucher được cập nhật và có trạng thái đang hoạt động (true)
        if (Boolean.TRUE.equals(updatedVoucher.getTrangThai())) {
            sendVoucherNotification(updatedVoucher);
        }

        return ResponseEntity.ok(updatedVoucher);
    }

    @PatchMapping("/{id}/trang-thai")
    @Transactional
    public ResponseEntity<?> quickToggle(@PathVariable Integer id) {
        MaGiamGia entity = voucherRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin mã giảm giá yêu cầu."));
        boolean newStatus = !entity.getTrangThai();
        String message;

        if (newStatus) {
            if (entity.getNgayKetThuc() != null && entity.getNgayKetThuc().isBefore(LocalDateTime.now())) {
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


    private String generateCode() {
        // Lấy thời gian hiện tại thành chuỗi (13 chữ số)
        String timeStr = String.valueOf(System.currentTimeMillis());
        // Chỉ lấy 8 số cuối cùng ghép với chữ "KM" -> Tổng cộng vừa đúng 10 ký tự
        return "VOUCHER" + timeStr.substring(timeStr.length() - 5);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoUpdateVoucherStatus() {
        List<MaGiamGia> expiredVouchers = voucherRepo.findAll().stream()
                .filter(v -> v.getTrangThai() && (
                        (v.getNgayKetThuc() != null && v.getNgayKetThuc().isBefore(LocalDateTime.now())) ||
                                v.getLuotSuDung() >= v.getSoLuong()
                ))
                .collect(Collectors.toList());

        if (!expiredVouchers.isEmpty()) {
            expiredVouchers.forEach(v -> v.setTrangThai(false));
            voucherRepo.saveAll(expiredVouchers);
            System.out.println("Auto-Scheduler: Đã tắt " + expiredVouchers.size() + " voucher hết hạn/hết số lượng.");
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<MaGiamGia>> getActiveVouchers() {
        LocalDateTime now = LocalDateTime.now();

        List<MaGiamGia> activeVouchers = voucherRepo.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getTrangThai())
                        && v.getNgayBatDau() != null && !now.isBefore(v.getNgayBatDau())
                        && (v.getNgayKetThuc() == null || !now.isAfter(v.getNgayKetThuc()))
                        && v.getLuotSuDung() < v.getSoLuong())
                .collect(Collectors.toList());

        return ResponseEntity.ok(activeVouchers);
    }
}