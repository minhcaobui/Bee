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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CronJobService {

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private TrangThaiHoaDonRepository trangThaiHoaDonRepository;

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    /**
     * Cú pháp Cron: Giây - Phút - Giờ - Ngày - Tháng - Thứ
     * "0 0 2 * * ?" nghĩa là: Chạy vào đúng 02:00:00 sáng, mỗi ngày.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void donDepTonKhoTamGiu() {
        System.out.println("[CRONJOB - " + LocalDateTime.now() + "] Đang dọn dẹp hàng bị giam tại POS...");

        // Chạy lệnh reset kho tạm giữ về 0
        sanPhamChiTietRepository.resetAllSoLuongTamGiu();

        System.out.println("[CRONJOB] Hoàn tất dọn dẹp! Toàn bộ kho đã được giải phóng.");
    }

    /**
     * Chạy mỗi 5 phút (300.000 ms) để tìm và hủy các đơn hàng Online bị khách thoát ngang
     */
    @Scheduled(fixedRate = 300000)
    @Transactional // Đảm bảo nếu bị lỗi giữa chừng thì rollback lại toàn bộ
    public void huyDonHangTreo() {
        System.out.println("[CRONJOB - " + LocalDateTime.now() + "] Đang quét các đơn hàng treo (quá 15 phút chưa thanh toán)...");

        // 1. Xác định mốc thời gian: Trước hiện tại 15 phút (Dùng java.util.Date cho đồng bộ)
        long fifteenMinutesInMillis = 15 * 60 * 1000;
        java.util.Date timeLimit = new java.util.Date(System.currentTimeMillis() - fifteenMinutesInMillis);

        // 2. Lấy trạng thái Hủy để chuẩn bị cập nhật
        TrangThaiHoaDon trangThaiHuy = trangThaiHoaDonRepository.findByMa("DA_HUY");

        if (trangThaiHuy == null) return; // Nếu DB không có trạng thái hủy thì bỏ qua

        // Lấy tất cả hóa đơn lên để kiểm tra
        List<HoaDon> danhSachHoaDon = hoaDonRepository.findAll();
        int count = 0;

        for (HoaDon hd : danhSachHoaDon) {
            // Kiểm tra: Đơn hàng "CHO_THANH_TOAN" và đã tạo quá 15 phút
            if (hd.getTrangThaiHoaDon() != null
                    && "CHO_THANH_TOAN".equals(hd.getTrangThaiHoaDon().getMa())
                    && hd.getNgayTao() != null
                    && hd.getNgayTao().before(timeLimit)) { // SỬA: Dùng hàm .before() của java.util.Date

                // A. Hoàn lại số lượng sản phẩm vào kho
                List<HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());
                for (HoaDonChiTiet ct : chiTiets) {
                    SanPhamChiTiet spct = ct.getSanPhamChiTiet();
                    if (spct != null) {
                        spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong()); // Cộng trả lại kho
                        sanPhamChiTietRepository.save(spct);
                    }
                }

                // B. Cập nhật hóa đơn thành Đã Hủy
                hd.setTrangThaiHoaDon(trangThaiHuy);
                String ghiChuCu = hd.getGhiChu() != null ? hd.getGhiChu() : "";
                hd.setGhiChu(ghiChuCu + " [Hệ thống tự động hủy do quá hạn thanh toán 15 phút]");
                hoaDonRepository.save(hd);

                // C. Lưu lịch sử hóa đơn
                LichSuHoaDon ls = new LichSuHoaDon();
                ls.setHoaDon(hd);
                ls.setTrangThaiHoaDon(trangThaiHuy);
                ls.setGhiChu("Hệ thống tự động hủy đơn hàng và hoàn tồn kho do quá 15 phút không thanh toán.");

                // Đồng bộ kiểu dữ liệu cho Lịch sử hóa đơn
                // NẾU BÁO LỖI Ở DÒNG NÀY, hãy đổi 'new java.util.Date()' thành 'LocalDateTime.now()'
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