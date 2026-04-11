package com.example.bee.services;

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
    public void guiEmailXacNhanDonHang(HoaDon hoaDon, String emailKhachHang) {
        if (emailKhachHang == null || emailKhachHang.trim().isEmpty()) {
            return;
        }
        try {
            SimpleMailMessage tinNhan = new SimpleMailMessage();
            tinNhan.setTo(emailKhachHang);
            tinNhan.setSubject("BEEMATE - Xác nhận đơn hàng #" + hoaDon.getMa());

            // 1. Lấy tên người nhận từ ThongTinGiaoHang (JSON) hoặc KhachHang
            String tenNguoiNhan = "Khách hàng";
            if (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getTenNguoiNhan() != null) {
                tenNguoiNhan = hoaDon.getThongTinGiaoHang().getTenNguoiNhan();
            } else if (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getHoTen() != null) {
                tenNguoiNhan = hoaDon.getKhachHang().getHoTen();
            }

            // 2. Xử lý địa chỉ nhận hàng linh hoạt theo hình thức giao hàng
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

            // 3. Xây dựng nội dung Email
            String noiDung = "Xin chào " + tenNguoiNhan + ",\n\n"
                    + "Cảm ơn bạn đã mua sắm tại BeeMate! Đơn hàng của bạn đã được ghi nhận thành công.\n\n"
                    + "📦 MÃ ĐƠN HÀNG: " + hoaDon.getMa() + "\n"
                    + "📍 Địa chỉ nhận hàng: " + diaChiGiaoHang + "\n"
                    + "💵 Tổng thanh toán: " + String.format("%,.0f", hoaDon.getGiaTong()) + " VNĐ\n\n"
                    + "Chúng tôi sẽ sớm liên hệ để cập nhật thông tin và giao hàng cho bạn.\n\n"
                    + "Trân trọng,\nĐội ngũ BeeMate.";

            tinNhan.setText(noiDung);
            mailSender.send(tinNhan);

        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email xác nhận đơn hàng: " + e.getMessage());
        }
    }
}