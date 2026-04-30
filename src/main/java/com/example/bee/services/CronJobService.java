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
     * Chạy mỗi 5 phút (300.000 ms) để tìm và hủy:
     * 1. Đơn thanh toán Online bị treo (quá 15 phút).
     * 2. Đơn nhận tại cửa hàng quá hạn lịch hẹn (quá 24 giờ).
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void huyDonHangTreo() {
        System.out.println("[CRONJOB - " + LocalDateTime.now() + "] Đang quét kiểm tra đơn hàng quá hạn...");

        TrangThaiHoaDon trangThaiHuy = trangThaiHoaDonRepository.findByMa("DA_HUY");
        if (trangThaiHuy == null) {
            System.out.println("[CRONJOB] Lỗi: Không tìm thấy trạng thái DA_HUY trong hệ thống.");
            return;
        }

        int count = 0;
        long currentMillis = System.currentTimeMillis();

        // ==============================================================================
        // 1. XỬ LÝ ĐƠN THANH TOÁN ONLINE (MOMO/VNPAY/CHUYỂN KHOẢN) QUÁ 15 PHÚT
        // ==============================================================================
        long fifteenMinutesInMillis = 15 * 60 * 1000;
        Date timeLimit15m = new Date(currentMillis - fifteenMinutesInMillis);

        List<HoaDon> danhSachDonOnlineTreo = hoaDonRepository.findByTrangThaiHoaDon_MaAndNgayTaoBefore("CHO_THANH_TOAN", timeLimit15m);
        for (HoaDon hd : danhSachDonOnlineTreo) {
            hoanKhoVaHuyDon(hd, trangThaiHuy, "Hệ thống tự động hủy do quá hạn thanh toán 15 phút");
            count++;
        }

        // ==============================================================================
        // 2. XỬ LÝ ĐƠN NHẬN TẠI CỬA HÀNG KHÁCH KHÔNG ĐẾN LẤY QUÁ HẠN LỊCH HẸN
        // ==============================================================================
        // Lấy tất cả đơn đang ở trạng thái CHỜ KHÁCH LẤY
        // (Dùng stream filter vì số lượng đơn chờ lấy tại một thời điểm thường nhỏ, xử lý bằng Java rất nhanh và dễ tuỳ biến logic)
        List<HoaDon> danhSachChoKhachLay = hoaDonRepository.findAll().stream()
                .filter(hd -> "CHO_KHACH_LAY".equals(hd.getTrangThaiHoaDon().getMa()) && "NHAN_TAI_CUA_HANG".equals(hd.getHinhThucGiaoHang()))
                .toList();

        long thoiGianChoPhepTreHan = 24L * 60 * 60 * 1000; // Cho phép khách lấy trễ tối đa 24 tiếng so với lịch hẹn

        for (HoaDon hd : danhSachChoKhachLay) {
            // Lấy thời gian từ các mốc (nếu null thì fallback về thời gian tạo đơn)
            long mHenLay = hd.getNgayHenLayHang() != null ? hd.getNgayHenLayHang().getTime() : hd.getNgayTao().getTime();
            long mSanSang = hd.getNgayHangSanSang() != null ? hd.getNgayHangSanSang().getTime() : hd.getNgayTao().getTime();

            // Lấy mốc thời gian lớn hơn để làm chuẩn
            // (Đề phòng cửa hàng bận, đóng gói hàng trễ hơn so với giờ khách hẹn ban đầu)
            long mocThoiGianChuan = Math.max(mHenLay, mSanSang);

            // Nếu thời điểm hiện tại đã vượt qua (Mốc chuẩn + 24 giờ) -> Hủy
            if (currentMillis > (mocThoiGianChuan + thoiGianChoPhepTreHan)) {
                hoanKhoVaHuyDon(hd, trangThaiHuy, "Tự động hủy do khách không đến lấy sau 24h kể từ lịch hẹn/chuẩn bị");
                count++;
            }
        }

        if (count > 0) {
            System.out.println("[CRONJOB] Đã hủy tự động và hoàn kho thành công " + count + " đơn hàng vi phạm.");
        } else {
            System.out.println("[CRONJOB] Không có đơn hàng treo nào cần xử lý.");
        }
    }

    /**
     * Hàm dùng chung để Hủy đơn, hoàn lại Tồn kho và Voucher
     */
    private void hoanKhoVaHuyDon(HoaDon hd, TrangThaiHoaDon trangThaiHuy, String lyDoHuy) {
        // 1. Hoàn lại số lượng tồn kho
        List<HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());
        for (HoaDonChiTiet ct : chiTiets) {
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            if (spct != null) {
                spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong());
                sanPhamChiTietRepository.save(spct);
            }
        }

        // 2. Hoàn lại lượt sử dụng Mã giảm giá (nếu có)
        if (hd.getMaGiamGia() != null) {
            com.example.bee.entities.promotion.MaGiamGia voucher = hd.getMaGiamGia();
            int luotMoi = voucher.getLuotSuDung() - 1;
            if (luotMoi >= 0) {
                voucher.setLuotSuDung(luotMoi);
                // Kích hoạt lại Voucher nếu nó bị tắt do vừa hết lượt dùng
                if (!voucher.getTrangThai() && voucher.getNgayKetThuc().isAfter(java.time.LocalDateTime.now())) {
                    voucher.setTrangThai(true);
                }
                maGiamGiaRepo.save(voucher);
            }
        }

        // 3. Đổi trạng thái hóa đơn thành ĐÃ HỦY
        hd.setTrangThaiHoaDon(trangThaiHuy);
        String ghiChuCu = hd.getGhiChu() != null ? hd.getGhiChu() : "";
        hd.setGhiChu(ghiChuCu + " [" + lyDoHuy + "]");
        hoaDonRepository.save(hd);

        // 4. Lưu lịch sử hóa đơn
        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setTrangThaiHoaDon(trangThaiHuy);
        ls.setGhiChu(lyDoHuy);
        ls.setNgayTao(new java.util.Date());
        lichSuHoaDonRepository.save(ls);
    }
}