package com.example.bee.services;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.account.VaiTro;
import com.example.bee.entities.cart.GioHang;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.account.VaiTroRepository;
import com.example.bee.repositories.cart.GioHangRepository;
import com.example.bee.repositories.customer.KhachHangRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DangNhapService {

    private static final Map<String, String> boNhoLuuOtpQuenMatKhau = new HashMap<>();

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final VaiTroRepository vaiTroRepository;
    private final PasswordEncoder passwordEncoder;
    private final GioHangRepository gioHangRepository;
    private final JavaMailSender mailSender;

    private String taoMaTuDong() {
        return "KH" + System.currentTimeMillis();
    }

    @Transactional
    public String dangKy(String hoTen, String soDienThoai, String email, String matKhau, RedirectAttributes redirectAttributes) {
        try {
            if (taiKhoanRepository.findByTenDangNhap(soDienThoai).isPresent()) {
                redirectAttributes.addAttribute("regError", "Số điện thoại này đã được đăng ký!");
                return "redirect:/login";
            }
            if (khachHangRepository.existsByEmail(email)) {
                redirectAttributes.addAttribute("regError", "Email này đã được sử dụng!");
                return "redirect:/login";
            }

            TaiKhoan taiKhoanMoi = new TaiKhoan();
            taiKhoanMoi.setTenDangNhap(soDienThoai);
            taiKhoanMoi.setMatKhau(passwordEncoder.encode(matKhau));
            VaiTro quyenKhachHang = vaiTroRepository.findById(3)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Role Khách Hàng"));
            taiKhoanMoi.setVaiTro(quyenKhachHang);
            taiKhoanMoi.setTrangThai(true);
            TaiKhoan taiKhoanDaLuu = taiKhoanRepository.save(taiKhoanMoi);

            GioHang gioHang = new GioHang();
            gioHang.setTaiKhoan(taiKhoanDaLuu);
            gioHangRepository.save(gioHang);

            Optional<KhachHang> khachHangTonTai = khachHangRepository.findBySoDienThoai(soDienThoai);
            if (khachHangTonTai.isPresent()) {
                KhachHang khachHangHienTai = khachHangTonTai.get();
                khachHangHienTai.setTaiKhoan(taiKhoanDaLuu);
                khachHangHienTai.setHoTen(hoTen);
                khachHangHienTai.setEmail(email);
                khachHangRepository.save(khachHangHienTai);
            } else {
                KhachHang khachHangMoi = new KhachHang();
                khachHangMoi.setMa(taoMaTuDong());
                khachHangMoi.setHoTen(hoTen);
                khachHangMoi.setSoDienThoai(soDienThoai);
                khachHangMoi.setEmail(email);
                khachHangMoi.setTaiKhoan(taiKhoanDaLuu);
                khachHangMoi.setTrangThai(true);
                khachHangRepository.save(khachHangMoi);
            }
            redirectAttributes.addAttribute("registered", "true");
            return "redirect:/login";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addAttribute("regError", "Lỗi tạo tài khoản, vui lòng thử lại!");
            return "redirect:/login";
        }
    }

    public ResponseEntity<?> guiOtpQuenMatKhau(String email) {
        try {
            Optional<KhachHang> khachHangOpt = khachHangRepository.findByEmail(email);
            if (khachHangOpt.isEmpty() || khachHangOpt.get().getTaiKhoan() == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy tài khoản nào liên kết với Email này!");
            }

            String maXacThuc = String.format("%06d", new Random().nextInt(999999));
            boNhoLuuOtpQuenMatKhau.put(email, maXacThuc);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[BeeMate] MÃ OTP KHÔI PHỤC MẬT KHẨU");
            message.setText("Chào bạn,\n\nBạn đã yêu cầu khôi phục mật khẩu tại BeeMate.\n"
                    + "Mã xác thực OTP của bạn là: " + maXacThuc + "\n\n"
                    + "Tuyệt đối không chia sẻ mã này cho bất kỳ ai.\n"
                    + "Trân trọng,\nĐội ngũ BeeMate.");
            mailSender.send(message);

            return ResponseEntity.ok("Mã OTP đã được gửi đến email của bạn!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi gửi Email. Vui lòng thử lại sau!");
        }
    }

    @Transactional
    public ResponseEntity<?> datLaiMatKhau(String email, String maXacThuc) {
        try {
            String maXacThucDaLuu = boNhoLuuOtpQuenMatKhau.get(email);
            if (maXacThucDaLuu == null || !maXacThucDaLuu.equals(maXacThuc)) {
                return ResponseEntity.badRequest().body("Mã OTP không chính xác hoặc đã hết hạn!");
            }

            KhachHang kh = khachHangRepository.findByEmail(email).orElse(null);
            if (kh == null || kh.getTaiKhoan() == null) {
                return ResponseEntity.badRequest().body("Lỗi xác thực tài khoản!");
            }

            String matKhauMoi = UUID.randomUUID().toString().substring(0, 8);

            TaiKhoan tk = kh.getTaiKhoan();
            tk.setMatKhau(passwordEncoder.encode(matKhauMoi));
            taiKhoanRepository.save(tk);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[BeeMate] MẬT KHẨU MỚI CỦA BẠN");
            message.setText("Chào bạn,\n\nMật khẩu của bạn đã được đặt lại thành công.\n"
                    + "Mật khẩu mới của bạn là: " + matKhauMoi + "\n\n"
                    + "Vui lòng đăng nhập và đổi lại mật khẩu ngay lập tức để bảo đảm an toàn.\n"
                    + "Trân trọng,\nĐội ngũ BeeMate.");
            mailSender.send(message);

            boNhoLuuOtpQuenMatKhau.remove(email);
            return ResponseEntity.ok("Thành công! Mật khẩu mới đã được gửi vào Email của bạn.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hệ thống, không thể đặt lại mật khẩu!");
        }
    }
}