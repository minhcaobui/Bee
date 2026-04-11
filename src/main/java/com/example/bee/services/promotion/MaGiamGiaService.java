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

    private final MaGiamGiaRepository maGiamGiaRepo;
    private final ThongBaoRepository thongBaoRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    private void kiemTraLogicMaGiamGia(MaGiamGia duLieu) {
        if (duLieu.getNgayBatDau() == null) {
            throw new IllegalArgumentException("Vui lòng chọn Ngày bắt đầu.");
        }
        if (duLieu.getNgayKetThuc() != null && duLieu.getNgayBatDau().isAfter(duLieu.getNgayKetThuc())) {
            throw new IllegalArgumentException("Lỗi: Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if (duLieu.getNgayKetThuc() != null && duLieu.getNgayKetThuc().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Lỗi: Ngày kết thúc không được nằm trong quá khứ.");
        }
        if (duLieu.getSoLuong() == null || duLieu.getSoLuong() <= 0) {
            throw new IllegalArgumentException("Lỗi: Số lượng mã giảm giá phải lớn hơn 0.");
        }
        if (duLieu.getDieuKien() == null || duLieu.getDieuKien().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Lỗi: Điều kiện đơn hàng tối thiểu không hợp lệ.");
        }
        if (duLieu.getGiaTriGiamGia() == null || duLieu.getGiaTriGiamGia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Lỗi: Giá trị giảm giá phải lớn hơn 0.");
        }

        boolean laPhanTram = duLieu.getLoaiGiamGia() != null &&
                (duLieu.getLoaiGiamGia().equalsIgnoreCase("PERCENT") || duLieu.getLoaiGiamGia().contains("%"));

        if (laPhanTram) {
            if (duLieu.getGiaTriGiamGia().compareTo(BigDecimal.ONE) < 0 || duLieu.getGiaTriGiamGia().compareTo(new BigDecimal("50")) > 0) {
                throw new IllegalArgumentException("Lỗi: Mức giảm phần trăm chỉ được phép từ 1% đến 50% để tránh bán dưới giá vốn.");
            }
            if (duLieu.getGiaTriGiamGiaToiDa() == null || duLieu.getGiaTriGiamGiaToiDa().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Lỗi rủi ro cực cao: Khuyến mãi theo % BẮT BUỘC phải thiết lập 'Mức giảm tối đa (VNĐ)' để tránh khách mua đơn hàng lớn và trừ âm tiền cửa hàng.");
            }
        } else {
            if (duLieu.getGiaTriGiamGia().compareTo(new BigDecimal("1000")) < 0) {
                throw new IllegalArgumentException("Lỗi: Mức giảm tiền mặt tối thiểu là 1.000 VNĐ.");
            }

            if (duLieu.getDieuKien().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal mucGiamToiDaAnToan = duLieu.getDieuKien().multiply(new BigDecimal("0.5"));
                if (duLieu.getGiaTriGiamGia().compareTo(mucGiamToiDaAnToan) > 0) {
                    throw new IllegalArgumentException("Lỗi rủi ro lỗ: Với đơn tối thiểu " + String.format("%,.0f", duLieu.getDieuKien()) + "đ, mức giảm tiền mặt không được vượt quá 50% (Tối đa " + String.format("%,.0f", mucGiamToiDaAnToan) + "đ).");
                }
            } else {
                if (duLieu.getGiaTriGiamGia().compareTo(new BigDecimal("50000")) > 0) {
                    throw new IllegalArgumentException("Lỗi: Voucher áp dụng cho mọi đơn hàng (0đ) chỉ được phép giảm tối đa 50.000 VNĐ để chống gian lận bào mã.");
                }
            }
            duLieu.setGiaTriGiamGiaToiDa(duLieu.getGiaTriGiamGia());
        }
    }

    private String taoMaTuDong() {
        String timeStr = String.valueOf(System.currentTimeMillis());
        return "VOUCHER" + timeStr.substring(timeStr.length() - 5);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void tuDongCapNhatTrangThaiMa() {
        List<MaGiamGia> danhSachHetHan = maGiamGiaRepo.findAll().stream()
                .filter(v -> v.getTrangThai() && (
                        (v.getNgayKetThuc() != null && v.getNgayKetThuc().isBefore(LocalDateTime.now())) ||
                                v.getLuotSuDung() >= v.getSoLuong()
                ))
                .collect(Collectors.toList());

        if (!danhSachHetHan.isEmpty()) {
            danhSachHetHan.forEach(v -> v.setTrangThai(false));
            maGiamGiaRepo.saveAll(danhSachHetHan);
            System.out.println("Auto-Scheduler: Đã tắt " + danhSachHetHan.size() + " voucher hết hạn/hết số lượng.");
        }
    }

    public Page<MaGiamGia> layDanhSach(String tuKhoa, Boolean trangThai, LocalDateTime tuNgay, LocalDateTime denNgay, Pageable pageable) {
        String keyword = (tuKhoa != null && !tuKhoa.trim().isEmpty()) ? tuKhoa.trim() : null;
        return maGiamGiaRepo.searchVoucher(keyword, trangThai, tuNgay, denNgay, pageable);
    }

    public MaGiamGia layChiTiet(Integer id) {
        return maGiamGiaRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Voucher"));
    }

    @Transactional
    public MaGiamGia taoMoi(MaGiamGia duLieu) {
        kiemTraLogicMaGiamGia(duLieu);

        if (duLieu.getMaCode() == null || duLieu.getMaCode().trim().isEmpty()) {
            duLieu.setMaCode(taoMaTuDong());
        } else {
            duLieu.setMaCode(duLieu.getMaCode().trim().toUpperCase());
            if (maGiamGiaRepo.existsByMaCodeIgnoreCase(duLieu.getMaCode())) {
                throw new IllegalArgumentException("Mã Voucher này đã tồn tại!");
            }
        }

        duLieu.setLuotSuDung(0);
        if (duLieu.getTrangThai() == null) duLieu.setTrangThai(true);
        if (duLieu.getChoPhepCongDon() == null) duLieu.setChoPhepCongDon(false);

        MaGiamGia maGiamGiaDaLuu = maGiamGiaRepo.save(duLieu);

        try {
            List<TaiKhoan> khachHangs = taiKhoanRepository.findByVaiTro_Ma("ROLE_CUSTOMER");
            if (khachHangs != null && !khachHangs.isEmpty()) {
                List<ThongBao> thongBaos = new ArrayList<>();
                for (TaiKhoan tk : khachHangs) {
                    ThongBao tb = new ThongBao();
                    tb.setTaiKhoanId(tk.getId());
                    tb.setTieuDe("Mã giảm giá mới dành cho bạn!");
                    tb.setNoiDung("Mã ưu đãi " + maGiamGiaDaLuu.getMaCode() + " đã có sẵn. Nhập mã ngay lúc thanh toán để được giảm giá nhé!");
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

        return maGiamGiaDaLuu;
    }

    @Transactional
    public MaGiamGia capNhat(Integer id, MaGiamGia duLieu) {
        kiemTraLogicMaGiamGia(duLieu);

        MaGiamGia entity = maGiamGiaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin mã giảm giá yêu cầu."));

        if (entity.getLuotSuDung() > 0) {
            if (entity.getGiaTriGiamGia().compareTo(duLieu.getGiaTriGiamGia()) != 0 ||
                    !entity.getLoaiGiamGia().equals(duLieu.getLoaiGiamGia())) {
                throw new IllegalArgumentException("Mã giảm giá đã phát sinh lượt sử dụng thực tế, không thể thay đổi loại hoặc định mức chiết khấu.");
            }
        }

        entity.setTen(duLieu.getTen().trim());
        entity.setLoaiGiamGia(duLieu.getLoaiGiamGia());
        entity.setGiaTriGiamGia(duLieu.getGiaTriGiamGia());
        entity.setGiaTriGiamGiaToiDa(duLieu.getGiaTriGiamGiaToiDa());
        entity.setDieuKien(duLieu.getDieuKien());
        entity.setSoLuong(duLieu.getSoLuong());
        entity.setNgayBatDau(duLieu.getNgayBatDau());
        entity.setNgayKetThuc(duLieu.getNgayKetThuc());
        entity.setChoPhepCongDon(duLieu.getChoPhepCongDon());
        entity.setTrangThai(duLieu.getTrangThai());

        return maGiamGiaRepo.save(entity);
    }

    @Transactional
    public String doiTrangThaiNhanh(Integer id) {
        MaGiamGia entity = maGiamGiaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin mã giảm giá yêu cầu."));
        boolean trangThaiMoi = !entity.getTrangThai();
        String thongBao;

        if (trangThaiMoi) {
            if (entity.getNgayKetThuc() != null && entity.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Kích hoạt thất bại. Mã giảm giá đã quá hạn thời gian sử dụng.");
            }
            if (entity.getLuotSuDung() >= entity.getSoLuong()) {
                throw new IllegalArgumentException("Kích hoạt thất bại. Mã giảm giá đã đạt giới hạn định mức phát hành (" + entity.getLuotSuDung() + "/" + entity.getSoLuong() + ").");
            }
            thongBao = "Mã giảm giá đã được đưa vào trạng thái hoạt động.";
        } else {
            thongBao = "Mã giảm giá đã được chuyển sang trạng thái tạm ngừng áp dụng.";
        }

        entity.setTrangThai(trangThaiMoi);
        maGiamGiaRepo.save(entity);
        return thongBao;
    }

    public List<MaGiamGia> layMaGiamGiaHoatDong() {
        LocalDateTime hienTai = LocalDateTime.now();
        return maGiamGiaRepo.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getTrangThai())
                        && v.getNgayBatDau() != null && !hienTai.isBefore(v.getNgayBatDau())
                        && (v.getNgayKetThuc() == null || !hienTai.isAfter(v.getNgayKetThuc()))
                        && v.getLuotSuDung() < v.getSoLuong())
                .collect(Collectors.toList());
    }
}