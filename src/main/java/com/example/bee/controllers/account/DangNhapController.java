package com.example.bee.controllers.account;

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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class DangNhapController {

    private static final Map<String, String> otpForgotStorage = new HashMap<>();
    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final VaiTroRepository vaiTroRepository;
    private final PasswordEncoder passwordEncoder;
    private final GioHangRepository gioHangRepository;
    private final JavaMailSender mailSender;

    private String generateMa() {
        return "KH" + System.currentTimeMillis();
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login/login";
    }

    @PostMapping("/register")
    @Transactional
    public String register(
            @RequestParam("hoTen") String hoTen,
            @RequestParam("soDienThoai") String soDienThoai,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes) {
        try {
            if (taiKhoanRepository.findByTenDangNhap(soDienThoai).isPresent()) {
                redirectAttributes.addAttribute("regError", "Số điện thoại này đã được đăng ký!");
                return "redirect:/login";
            }
            if (khachHangRepository.existsByEmail(email)) {
                redirectAttributes.addAttribute("regError", "Email này đã được sử dụng!");
                return "redirect:/login";
            }

            TaiKhoan newAccount = new TaiKhoan();
            newAccount.setTenDangNhap(soDienThoai);
            newAccount.setMatKhau(passwordEncoder.encode(password));
            VaiTro roleCustomer = vaiTroRepository.findById(3)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Role Khách Hàng"));
            newAccount.setVaiTro(roleCustomer);
            newAccount.setTrangThai(true);
            TaiKhoan savedAccount = taiKhoanRepository.save(newAccount);

            GioHang gioHang = new GioHang();
            gioHang.setTaiKhoan(savedAccount);
            gioHangRepository.save(gioHang);

            Optional<KhachHang> khachPos = khachHangRepository.findBySoDienThoai(soDienThoai);
            if (khachPos.isPresent()) {
                KhachHang existingKh = khachPos.get();
                existingKh.setTaiKhoan(savedAccount);
                existingKh.setHoTen(hoTen);
                existingKh.setEmail(email);
                khachHangRepository.save(existingKh);
            } else {
                KhachHang newCustomer = new KhachHang();
                newCustomer.setMa(generateMa());
                newCustomer.setHoTen(hoTen);
                newCustomer.setSoDienThoai(soDienThoai);
                newCustomer.setEmail(email);
                newCustomer.setTaiKhoan(savedAccount);
                newCustomer.setTrangThai(true);
                khachHangRepository.save(newCustomer);
            }
            redirectAttributes.addAttribute("registered", "true");
            return "redirect:/login";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addAttribute("regError", "Lỗi tạo tài khoản, vui lòng thử lại!");
            return "redirect:/login";
        }
    }

    @PostMapping("/forgot-password/send-otp")
    @ResponseBody
    public ResponseEntity<?> sendForgotOtp(@RequestParam String email) {
        try {
            Optional<KhachHang> khOpt = khachHangRepository.findByEmail(email);
            if (khOpt.isEmpty() || khOpt.get().getTaiKhoan() == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy tài khoản nào liên kết với Email này!");
            }

            String otp = String.format("%06d", new Random().nextInt(999999));
            otpForgotStorage.put(email, otp);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[BeeMate] MÃ OTP KHÔI PHỤC MẬT KHẨU");
            message.setText("Chào bạn,\n\nBạn đã yêu cầu khôi phục mật khẩu tại BeeMate.\n"
                    + "Mã xác thực OTP của bạn là: " + otp + "\n\n"
                    + "Tuyệt đối không chia sẻ mã này cho bất kỳ ai.\n"
                    + "Trân trọng,\nĐội ngũ BeeMate.");
            mailSender.send(message);

            return ResponseEntity.ok("Mã OTP đã được gửi đến email của bạn!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi gửi Email. Vui lòng thử lại sau!");
        }
    }

    @PostMapping("/forgot-password/reset")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestParam String email, @RequestParam String otp) {
        try {
            String savedOtp = otpForgotStorage.get(email);
            if (savedOtp == null || !savedOtp.equals(otp)) {
                return ResponseEntity.badRequest().body("Mã OTP không chính xác hoặc đã hết hạn!");
            }

            KhachHang kh = khachHangRepository.findByEmail(email).orElse(null);
            if (kh == null || kh.getTaiKhoan() == null) {
                return ResponseEntity.badRequest().body("Lỗi xác thực tài khoản!");
            }

            String newPw = UUID.randomUUID().toString().substring(0, 8);

            TaiKhoan tk = kh.getTaiKhoan();
            tk.setMatKhau(passwordEncoder.encode(newPw));
            taiKhoanRepository.save(tk);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[BeeMate] MẬT KHẨU MỚI CỦA BẠN");
            message.setText("Chào bạn,\n\nMật khẩu của bạn đã được đặt lại thành công.\n"
                    + "Mật khẩu mới của bạn là: " + newPw + "\n\n"
                    + "Vui lòng đăng nhập và đổi lại mật khẩu ngay lập tức để bảo đảm an toàn.\n"
                    + "Trân trọng,\nĐội ngũ BeeMate.");
            mailSender.send(message);
            otpForgotStorage.remove(email);
            return ResponseEntity.ok("Thành công! Mật khẩu mới đã được gửi vào Email của bạn.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hệ thống, không thể đặt lại mật khẩu!");
        }
    }
}