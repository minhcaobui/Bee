package com.example.bee.controllers.api;

import com.example.bee.entities.order.HoaDon;
import com.example.bee.entities.order.LichSuHoaDon;
import com.example.bee.entities.order.ThanhToan;
import com.example.bee.entities.order.TrangThaiHoaDon;
import com.example.bee.repositories.order.HoaDonRepository;
import com.example.bee.repositories.order.LichSuHoaDonRepository;
import com.example.bee.repositories.order.ThanhToanRepository;
import com.example.bee.repositories.order.TrangThaiHoaDonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/payment/webhook")
public class BankWebhookApi {

    @Autowired
    private HoaDonRepository hdRepo;
    @Autowired
    private ThanhToanRepository thanhToanRepo;
    @Autowired
    private TrangThaiHoaDonRepository ttRepo;
    @Autowired
    private LichSuHoaDonRepository lsRepo;

    // API này sẽ hứng dữ liệu do SePay bắn sang mỗi khi có tiền vào tài khoản
    @PostMapping("/sepay")
    @Transactional
    public ResponseEntity<?> handleSePayWebhook(@RequestBody Map<String, Object> payload) {
        try {
            // 1. Lấy thông tin từ payload (Gói tin JSON của SePay)
            String content = (String) payload.get("content"); // Nội dung CK: "ThanhToan HD123..."

            // Xử lý an toàn vì số tiền có thể ép kiểu Integer hoặc Double tùy SePay
            Number transferAmountNum = (Number) payload.get("transferAmount");
            BigDecimal transferAmount = transferAmountNum != null ? new BigDecimal(transferAmountNum.toString()) : BigDecimal.ZERO;

            String referenceCode = (String) payload.get("referenceCode"); // Mã GD ngân hàng (Mã tham chiếu)

            if (content == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Dữ liệu không hợp lệ hoặc số tiền = 0"));
            }

            // 2. Dùng Regex (Biểu thức chính quy) để bóc tách Mã Hóa Đơn ra khỏi nội dung
            String maHD = extractMaHoaDon(content);
            if (maHD == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Nội dung CK không chứa mã hóa đơn hợp lệ"));
            }

            // 3. Tìm hóa đơn trong Database
            // 3. Tìm hóa đơn trong Database (Tìm theo Mã)
            HoaDon hd = hdRepo.findByMa(maHD);

            // 🌟 NẾU TÌM THEO MÃ KHÔNG THẤY -> TỰ ĐỘNG TÌM THEO ID (Ví dụ: HD19 -> Cắt lấy số 19)
            if (hd == null) {
                try {
                    // Cắt bỏ chữ "HD" để lấy số
                    Integer idHoaDon = Integer.parseInt(maHD.toUpperCase().replace("HD", ""));
                    hd = hdRepo.findById(idHoaDon).orElse(null);
                } catch (Exception e) {
                    System.out.println("Không thể ép kiểu mã " + maHD + " sang ID số.");
                }
            }

            // Nếu vẫn không thấy thì mới báo lỗi
            if (hd == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Không tìm thấy hóa đơn: " + maHD));
            }

            // 4. Kiểm tra hóa đơn đã thanh toán chưa (chống spam trùng lặp Webhook)
            if (hd.getNgayThanhToan() != null) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Đơn hàng này đã được thanh toán trước đó"));
            }

            // 5. Kiểm tra số tiền khách chuyển có đủ không
            // (Bạn có thể bỏ qua bước này nếu muốn linh động cho khách nạp thiếu 1-2k, nhưng nên chặn)
            if (transferAmount.compareTo(hd.getGiaTong()) < 0) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Khách chuyển thiếu tiền: " + transferAmount));
            }

            // ==========================================
            // 6. XỬ LÝ THÀNH CÔNG: CẬP NHẬT DATABASE
            // ==========================================
            hd.setNgayThanhToan(new Date());

            // Nếu đơn đang "Chờ thanh toán", tự động đẩy sang "Chờ xác nhận"
            if (hd.getTrangThaiHoaDon() != null && "CHO_THANH_TOAN".equals(hd.getTrangThaiHoaDon().getMa())) {
                TrangThaiHoaDon ttXacNhan = ttRepo.findByMa("CHO_XAC_NHAN");
                hd.setTrangThaiHoaDon(ttXacNhan);

                // Lưu lịch sử hóa đơn
                LichSuHoaDon ls = new LichSuHoaDon();
                ls.setHoaDon(hd);
                ls.setTrangThaiHoaDon(ttXacNhan);
                ls.setGhiChu("Hệ thống tự động xác nhận thanh toán thành công qua Ngân hàng");
                ls.setNgayTao(new Date());
                lsRepo.save(ls);
            }
            hdRepo.save(hd);

            // 7. Lưu vào bảng thanh_toan để hiển thị ở giao diện Lịch sử GD
            ThanhToan tt = new ThanhToan();
            tt.setHoaDon(hd);
            tt.setSoTien(transferAmount);
            tt.setPhuongThuc("CHUYEN_KHOAN");
            tt.setLoaiThanhToan("THANH_TOAN");
            tt.setTrangThai("THANH_CONG");
            tt.setMaGiaoDich(referenceCode); // Lưu lại mã giao dịch ngân hàng để đối soát
            tt.setGhiChu("Nội dung CK: " + content);
            tt.setNgayThanhToan(new Date());
            thanhToanRepo.save(tt);

            System.out.println("✅ WEBHOOK THÀNH CÔNG: Đã duyệt đơn " + maHD + " - Số tiền: " + transferAmount);
            return ResponseEntity.ok(Map.of("success", true, "message", "Xác nhận thanh toán thành công"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lỗi Webhook: " + e.getMessage()));
        }
    }

    /**
     * Hàm phụ trợ: Trích xuất chuỗi có định dạng "HD" theo sau là các chữ số.
     * Ví dụ: Từ chuỗi "Nguyen Van A ck ThanhToan_HD1775985383270 mua ao"
     * Nó sẽ tách ra chuẩn xác: "HD1775985383270"
     */
    private String extractMaHoaDon(String content) {
        Pattern pattern = Pattern.compile("(HD\\d+)");
        Matcher matcher = pattern.matcher(content.toUpperCase());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}