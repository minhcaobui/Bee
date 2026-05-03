package com.example.bee.services;

import com.example.bee.entities.order.HoaDon;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOrderConfirmationEmail(HoaDon hoaDon, String emailKhachHang) {
        if (emailKhachHang == null || emailKhachHang.trim().isEmpty()) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailKhachHang);
            helper.setSubject("BEEMATE - Xác nhận đơn hàng #" + hoaDon.getMa());
            String tenNguoiNhan = "Khách hàng";
            if (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getTenNguoiNhan() != null) {
                tenNguoiNhan = hoaDon.getThongTinGiaoHang().getTenNguoiNhan();
            } else if (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getHoTen() != null) {
                tenNguoiNhan = hoaDon.getKhachHang().getHoTen();
            }
            String diaChiGiaoHang = "";
            if ("NHAN_TAI_CUA_HANG".equals(hoaDon.getHinhThucGiaoHang())) {
                diaChiGiaoHang = "Nhận tại cửa hàng BEEMATE (13 phố Phan Tây Nhạc, phường Xuân Phương, TP Hà Nội)";
            } else {
                if (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getDiaChiChiTiet() != null) {
                    diaChiGiaoHang = hoaDon.getThongTinGiaoHang().getDiaChiChiTiet();
                } else {
                    diaChiGiaoHang = "Chưa cập nhật";
                }
            }
            String tongTienFormated = String.format("%,.0f", hoaDon.getGiaTong());
            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 8px rgba(0,0,0,0.05);">
                        <div style="background-color: #fbd305; padding: 25px; text-align: center; color: #333;">
                            <h1 style="margin: 0; font-size: 28px; letter-spacing: 2px;">BEEMATE</h1>
                            <p style="margin: 5px 0 0 0; font-size: 16px;">Xác nhận đơn hàng thành công!</p>
                        </div>
                        <div style="padding: 30px; color: #444; line-height: 1.6;">
                            <p style="font-size: 16px;">Xin chào <strong>%s</strong>,</p>
                            <p>Cảm ơn bạn đã mua sắm tại <strong style="color: #f39c12;">BeeMate</strong>! Đơn hàng của bạn đã được hệ thống ghi nhận thành công.</p>
                    
                            <div style="background-color: #f9f9f9; padding: 20px; border-radius: 8px; border-left: 5px solid #fbd305; margin: 25px 0;">
                                <h3 style="margin-top: 0; border-bottom: 1px solid #ddd; padding-bottom: 10px; color: #333;">Chi tiết đơn hàng: #%s</h3>
                                <p style="margin-bottom: 8px;"><strong>📍 Địa chỉ nhận hàng:</strong> <br/> %s</p>
                                <p style="margin-top: 15px;"><strong>💵 Tổng thanh toán:</strong> <br/> 
                                    <span style="color: #e74c3c; font-weight: bold; font-size: 22px;">%s VNĐ</span>
                                </p>
                            </div>
                    
                            <p>Chúng tôi đang xử lý đơn hàng và sẽ sớm liên hệ lại với bạn để cập nhật tình trạng giao hàng.</p>
                            <p style="margin-top: 30px;">Trân trọng,<br><strong style="font-size: 16px;">Đội ngũ BeeMate</strong></p>
                        </div>
                        <div style="background-color: #f1f1f1; padding: 15px; text-align: center; font-size: 12px; color: #888;">
                            <p style="margin: 0;">© 2026 BeeMate Store. Tất cả quyền được bảo lưu.</p>
                            <p style="margin: 5px 0 0 0;">13 phố Phan Tây Nhạc, phường Xuân Phương, TP Hà Nội</p>
                        </div>
                    </div>
                    """.formatted(tenNguoiNhan, hoaDon.getMa(), diaChiGiaoHang, tongTienFormated);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email xác nhận đơn hàng: " + e.getMessage());
        }
    }
}