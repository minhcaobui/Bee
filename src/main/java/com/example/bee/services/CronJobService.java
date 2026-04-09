package com.example.bee.services;

import com.example.bee.entities.order.HoaDon;
import com.example.bee.entities.order.HoaDonChiTiet;
import com.example.bee.entities.order.LichSuHoaDon;
import com.example.bee.entities.order.TrangThaiHoaDon;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.repositories.order.HoaDonChiTietRepository;
import com.example.bee.repositories.order.HoaDonRepository;
import com.example.bee.repositories.order.LichSuHoaDonRepository;
import com.example.bee.repositories.order.TrangThaiHoaDonRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CronJobService {

    private final MaGiamGiaRepository maGiamGiaRepo;

    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    private final HoaDonRepository hoaDonRepository;

    private final HoaDonChiTietRepository hoaDonChiTietRepository;

    private final TrangThaiHoaDonRepository trangThaiHoaDonRepository;

    private final LichSuHoaDonRepository lichSuHoaDonRepository;

    /**
     * Cú pháp Cron: Giây - Phút - Giờ - Ngày - Tháng - Thứ
     * "0 0 2 * * ?" nghĩa là: Chạy vào đúng 02:00:00 sáng, mỗi ngày.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void donDepTonKhoTamGiu() {
        System.out.println("[CRONJOB - " + LocalDateTime.now() + "] Đang dọn dẹp hàng bị giam tại POS...");
        sanPhamChiTietRepository.resetAllSoLuongTamGiu();
        System.out.println("[CRONJOB] Hoàn tất dọn dẹp! Toàn bộ kho đã được giải phóng.");
    }

    /**
     * Chạy mỗi 5 phút (300.000 ms) để tìm và hủy các đơn hàng Online bị khách thoát ngang
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void huyDonHangTreo() {
        System.out.println("[CRONJOB - " + LocalDateTime.now() + "] Đang quét các đơn hàng treo (quá 15 phút chưa thanh toán)...");

        long fifteenMinutesInMillis = 15 * 60 * 1000;
        Date timeLimit = new Date(System.currentTimeMillis() - fifteenMinutesInMillis);

        TrangThaiHoaDon trangThaiHuy = trangThaiHoaDonRepository.findByMa("DA_HUY");

        if (trangThaiHuy == null) return;

        List<HoaDon> danhSachHoaDon = hoaDonRepository.findAll();
        int count = 0;

        for (HoaDon hd : danhSachHoaDon) {
            if (hd.getTrangThaiHoaDon() != null
                    && "CHO_THANH_TOAN".equals(hd.getTrangThaiHoaDon().getMa())
                    && hd.getNgayTao() != null
                    && hd.getNgayTao().before(timeLimit)) {

                List<HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());
                for (HoaDonChiTiet ct : chiTiets) {
                    SanPhamChiTiet spct = ct.getSanPhamChiTiet();
                    if (spct != null) {
                        spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong());
                        sanPhamChiTietRepository.save(spct);
                    }
                }
                if (hd.getMaGiamGia() != null) {
                    com.example.bee.entities.promotion.MaGiamGia voucher = hd.getMaGiamGia();
                    int luotMoi = voucher.getLuotSuDung() - 1;
                    if (luotMoi >= 0) {
                        voucher.setLuotSuDung(luotMoi);
                        if (!voucher.getTrangThai() && voucher.getNgayKetThuc().isAfter(java.time.LocalDateTime.now())) {
                            voucher.setTrangThai(true);
                        }
                        maGiamGiaRepo.save(voucher);
                    }
                }

                hd.setTrangThaiHoaDon(trangThaiHuy);
                String ghiChuCu = hd.getGhiChu() != null ? hd.getGhiChu() : "";
                hd.setGhiChu(ghiChuCu + " [Hệ thống tự động hủy do quá hạn thanh toán 15 phút]");
                hoaDonRepository.save(hd);

                LichSuHoaDon ls = new LichSuHoaDon();
                ls.setHoaDon(hd);
                ls.setTrangThaiHoaDon(trangThaiHuy);
                ls.setGhiChu("Hệ thống tự động hủy đơn hàng và hoàn tồn kho do quá 15 phút không thanh toán.");
                ls.setNgayTao(new java.util.Date());
                lichSuHoaDonRepository.save(ls);
                count++;
            }
        }

        if (count > 0) {
            System.out.println("[CRONJOB] Đã hủy tự động và hoàn kho thành công " + count + " đơn hàng.");
        }
    }
}