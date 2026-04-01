package com.example.bee.services;

import com.example.bee.repositories.products.SanPhamChiTietRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CronJobService {

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

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

    /* * MẸO TEST CODE: Nếu bạn muốn test thử xem nó có chạy không ngay bây giờ,
     * hãy comment dòng @Scheduled ở trên lại và mở dòng bên dưới ra.
     * Nó sẽ chạy dọn rác 1 phút / 1 lần để bạn xem log ở Console:
     * * @Scheduled(fixedRate = 60000)
     */
}