package com.example.bee.services;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.notification.ThongBao;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaGiamGiaService {

    private final MaGiamGiaRepository voucherRepo;
    private final ThongBaoRepository thongBaoRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    private void validateVoucherLogic(MaGiamGia body) {
        if (body.getNgayBatDau() == null) {
            throw new IllegalArgumentException("Vui lòng chọn Ngày bắt đầu.");
        }
        if (body.getNgayKetThuc() != null && body.getNgayBatDau().isAfter(body.getNgayKetThuc())) {
            throw new IllegalArgumentException("Lỗi: Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if (body.getNgayKetThuc() != null && body.getNgayKetThuc().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Lỗi: Ngày kết thúc không được nằm trong quá khứ.");
        }
        if (body.getSoLuong() == null || body.getSoLuong() <= 0) {
            throw new IllegalArgumentException("Lỗi: Số lượng mã giảm giá phải lớn hơn 0.");
        }
        if (body.getDieuKien() == null || body.getDieuKien().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Lỗi: Điều kiện đơn hàng tối thiểu không hợp lệ.");
        }
        if (body.getGiaTriGiamGia() == null || body.getGiaTriGiamGia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Lỗi: Giá trị giảm giá phải lớn hơn 0.");
        }

        boolean isPercent = body.getLoaiGiamGia() != null &&
                (body.getLoaiGiamGia().equalsIgnoreCase("PERCENT") || body.getLoaiGiamGia().contains("%"));

        if (isPercent) {
            if (body.getGiaTriGiamGia().compareTo(BigDecimal.ONE) < 0 || body.getGiaTriGiamGia().compareTo(new BigDecimal("50")) > 0) {
                throw new IllegalArgumentException("Lỗi: Mức giảm phần trăm chỉ được phép từ 1% đến 50% để tránh bán dưới giá vốn.");
            }
            if (body.getGiaTriGiamGiaToiDa() == null || body.getGiaTriGiamGiaToiDa().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Lỗi rủi ro cực cao: Khuyến mãi theo % BẮT BUỘC phải thiết lập 'Mức giảm tối đa (VNĐ)' để tránh khách mua đơn hàng lớn và trừ âm tiền cửa hàng.");
            }
        } else {
            if (body.getGiaTriGiamGia().compareTo(new BigDecimal("1000")) < 0) {
                throw new IllegalArgumentException("Lỗi: Mức giảm tiền mặt tối thiểu là 1.000 VNĐ.");
            }

            if (body.getDieuKien().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal maxSafeDiscount = body.getDieuKien().multiply(new BigDecimal("0.5"));
                if (body.getGiaTriGiamGia().compareTo(maxSafeDiscount) > 0) {
                    throw new IllegalArgumentException("Lỗi rủi ro lỗ: Với đơn tối thiểu " + String.format("%,.0f", body.getDieuKien()) + "đ, mức giảm tiền mặt không được vượt quá 50% (Tối đa " + String.format("%,.0f", maxSafeDiscount) + "đ).");
                }
            } else {
                if (body.getGiaTriGiamGia().compareTo(new BigDecimal("50000")) > 0) {
                    throw new IllegalArgumentException("Lỗi: Voucher áp dụng cho mọi đơn hàng (0đ) chỉ được phép giảm tối đa 50.000 VNĐ để chống gian lận bào mã.");
                }
            }
            body.setGiaTriGiamGiaToiDa(body.getGiaTriGiamGia());
        }
    }

    private String generateCode() {
        String timeStr = String.valueOf(System.currentTimeMillis());
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

    public Page<MaGiamGia> getAll(String q, Boolean trangThai, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        String keyword = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        return voucherRepo.searchVoucher(keyword, trangThai, from, to, pageable);
    }

    public MaGiamGia getDetail(Integer id) {
        return voucherRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Voucher"));
    }

    @Transactional
    public MaGiamGia create(MaGiamGia body) {
        validateVoucherLogic(body);

        if (body.getMaCode() == null || body.getMaCode().trim().isEmpty()) {
            body.setMaCode(generateCode());
        } else {
            body.setMaCode(body.getMaCode().trim().toUpperCase());
            if (voucherRepo.existsByMaCodeIgnoreCase(body.getMaCode())) {
                throw new IllegalArgumentException("Mã Voucher này đã tồn tại!");
            }
        }

        body.setLuotSuDung(0);
        if (body.getTrangThai() == null) body.setTrangThai(true);
        if (body.getChoPhepCongDon() == null) body.setChoPhepCongDon(false);

        MaGiamGia savedVoucher = voucherRepo.save(body);

        try {
            List<TaiKhoan> khachHangs = taiKhoanRepository.findByVaiTro_Ma("ROLE_CUSTOMER");
            if (khachHangs != null && !khachHangs.isEmpty()) {
                List<ThongBao> thongBaos = new ArrayList<>();
                for (TaiKhoan tk : khachHangs) {
                    ThongBao tb = new ThongBao();
                    tb.setTaiKhoanId(tk.getId());
                    tb.setTieuDe("Mã giảm giá mới dành cho bạn!");
                    tb.setNoiDung("Mã ưu đãi " + savedVoucher.getMaCode() + " đã có sẵn. Nhập mã ngay lúc thanh toán để được giảm giá nhé!");
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

        return savedVoucher;
    }

    @Transactional
    public MaGiamGia update(Integer id, MaGiamGia body) {
        validateVoucherLogic(body);

        MaGiamGia entity = voucherRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin mã giảm giá yêu cầu."));

        if (entity.getLuotSuDung() > 0) {
            if (entity.getGiaTriGiamGia().compareTo(body.getGiaTriGiamGia()) != 0 ||
                    !entity.getLoaiGiamGia().equals(body.getLoaiGiamGia())) {
                throw new IllegalArgumentException("Mã giảm giá đã phát sinh lượt sử dụng thực tế, không thể thay đổi loại hoặc định mức chiết khấu.");
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

        return voucherRepo.save(entity);
    }

    @Transactional
    public String quickToggle(Integer id) {
        MaGiamGia entity = voucherRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin mã giảm giá yêu cầu."));
        boolean newStatus = !entity.getTrangThai();
        String message;

        if (newStatus) {
            if (entity.getNgayKetThuc() != null && entity.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Kích hoạt thất bại. Mã giảm giá đã quá hạn thời gian sử dụng.");
            }
            if (entity.getLuotSuDung() >= entity.getSoLuong()) {
                throw new IllegalArgumentException("Kích hoạt thất bại. Mã giảm giá đã đạt giới hạn định mức phát hành (" + entity.getLuotSuDung() + "/" + entity.getSoLuong() + ").");
            }
            message = "Mã giảm giá đã được đưa vào trạng thái hoạt động.";
        } else {
            message = "Mã giảm giá đã được chuyển sang trạng thái tạm ngừng áp dụng.";
        }

        entity.setTrangThai(newStatus);
        voucherRepo.save(entity);
        return message;
    }

    public List<MaGiamGia> getActiveVouchers() {
        LocalDateTime now = LocalDateTime.now();
        return voucherRepo.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getTrangThai())
                        && v.getNgayBatDau() != null && !now.isBefore(v.getNgayBatDau())
                        && (v.getNgayKetThuc() == null || !now.isAfter(v.getNgayKetThuc()))
                        && v.getLuotSuDung() < v.getSoLuong())
                .collect(Collectors.toList());
    }
}