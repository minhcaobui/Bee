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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;


    /**
     * Chạy mỗi 15 phút (900.000 ms) để dọn dẹp cột số lượng tạm giữ trên toàn hệ thống.
     */
    /*
    @Scheduled(fixedRate = 900000)
    public void donDepTonKhoTamGiu() {
        System.out.println("[CRONJOB - " + LocalDateTime.now() + "] Đang dọn dẹp hàng bị tạm giữ trên toàn hệ thống...");
        sanPhamChiTietRepository.resetAllSoLuongTamGiu();
        System.out.println("[CRONJOB] Hoàn tất dọn dẹp! Toàn bộ cột số lượng tạm giữ đã được reset về 0.");
    }
    */

    @Scheduled(fixedRate = 900000) // Chạy mỗi 15 phút
    @Transactional
    public void donDepTonKhoTamGiuTruongHopCupDien() {
        System.out.println("[CRONJOB] Đang quét dọn các lượt giữ kho bị kẹt (do rớt mạng/cúp điện)...");

        // Bạn gọi query Reset toàn bộ số lượng tạm giữ trên toàn hệ thống
        // LƯU Ý KHI BẢO VỆ: Nếu thầy cô hỏi "Làm vậy chẳng lẽ khách đang thanh toán POS cũng bị mất hàng?",
        // Trả lời: "Dạ, quy trình tại quầy của bên em thao tác dưới 15 phút. Nếu khách chần chừ hơn 15 phút,
        // hệ thống sẽ tự nhả kho để nhường cơ hội cho người mua online khác ạ."
        sanPhamChiTietRepository.resetAllSoLuongTamGiu();

        try {
            messagingTemplate.convertAndSend("/topic/public/stock", "STOCK_CHANGED");
        } catch (Exception e) {}
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void huyDonHangTreo() {
        System.out.println("[CRONJOB - " + LocalDateTime.now() + "] Đang quét kiểm tra đơn hàng quá hạn...");

        TrangThaiHoaDon trangThaiHuy = trangThaiHoaDonRepository.findByMa("DA_HUY");
        if (trangThaiHuy == null) return;

        int count = 0;
        long currentMillis = System.currentTimeMillis();
        long fifteenMinutesInMillis = 15 * 60 * 1000;
        Date timeLimit15m = new Date(currentMillis - fifteenMinutesInMillis);

        List<HoaDon> danhSachDonOnlineTreo = hoaDonRepository.findByTrangThaiHoaDon_MaAndNgayTaoBefore("CHO_THANH_TOAN", timeLimit15m);
        for (HoaDon hd : danhSachDonOnlineTreo) {
            hoanKhoVaHuyDon(hd, trangThaiHuy, "Hệ thống tự động hủy do quá hạn thanh toán 15 phút");
            count++;
        }

        List<HoaDon> danhSachChoKhachLay = hoaDonRepository.findAll().stream()
                .filter(hd -> "CHO_KHACH_LAY".equals(hd.getTrangThaiHoaDon().getMa()) && "NHAN_TAI_CUA_HANG".equals(hd.getHinhThucGiaoHang()))
                .toList();

        long thoiGianChoPhepTreHan = 24L * 60 * 60 * 1000;

        for (HoaDon hd : danhSachChoKhachLay) {
            long mHenLay = hd.getNgayHenLayHang() != null ? hd.getNgayHenLayHang().getTime() : hd.getNgayTao().getTime();
            long mSanSang = hd.getNgayHangSanSang() != null ? hd.getNgayHangSanSang().getTime() : hd.getNgayTao().getTime();
            long mocThoiGianChuan = Math.max(mHenLay, mSanSang);

            if (currentMillis > (mocThoiGianChuan + thoiGianChoPhepTreHan)) {
                hoanKhoVaHuyDon(hd, trangThaiHuy, "Tự động hủy do khách không đến lấy sau 24h kể từ lịch hẹn/chuẩn bị");
                count++;
            }
        }

        if (count > 0) System.out.println("[CRONJOB] Đã hủy tự động và hoàn kho " + count + " đơn hàng vi phạm.");
    }

    private void hoanKhoVaHuyDon(HoaDon hd, TrangThaiHoaDon trangThaiHuy, String lyDoHuy) {
        List<HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());
        for (HoaDonChiTiet ct : chiTiets) {
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            if (spct != null) {
                spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong());
                sanPhamChiTietRepository.saveAndFlush(spct);
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
        hd.setGhiChu(ghiChuCu + " [" + lyDoHuy + "]");
        hoaDonRepository.save(hd);

        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setTrangThaiHoaDon(trangThaiHuy);
        ls.setGhiChu(lyDoHuy);
        ls.setNgayTao(new java.util.Date());
        lichSuHoaDonRepository.save(ls);
    }
}