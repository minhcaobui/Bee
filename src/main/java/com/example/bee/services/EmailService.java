package com.example.bee.services; // Đổi package theo project của bạn

import com.example.bee.entities.order.HoaDon;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailKhachHang);
            message.setSubject("BEEMATE - Xác nhận đơn hàng #" + hoaDon.getMa());
            String text = "Xin chào " + hoaDon.getTenNguoiNhan() + ",\n\n"
                    + "Cảm ơn bạn đã mua sắm tại BeeMate! Đơn hàng của bạn đã được ghi nhận thành công.\n\n"
                    + "📦 MÃ ĐƠN HÀNG: " + hoaDon.getMa() + "\n"
                    + "📍 Địa chỉ giao hàng: " + hoaDon.getDiaChiGiaoHang() + "\n"
                    + "💵 Tổng thanh toán: " + String.format("%,.0f", hoaDon.getGiaTong()) + " VNĐ\n\n"
                    + "Chúng tôi sẽ sớm liên hệ để giao hàng cho bạn.\n\n"
                    + "Trân trọng,\nĐội ngũ BeeMate.";
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email: " + e.getMessage());
        }
    }
}