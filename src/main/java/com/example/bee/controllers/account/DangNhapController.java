package com.example.bee.controllers.account;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.account.VaiTro;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.account.VaiTroRepository;
import com.example.bee.repositories.customer.KhachHangRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DangNhapController {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final VaiTroRepository vaiTroRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @Transactional
    public String registerCustomer(
            @RequestParam("hoTen") String hoTen,
            @RequestParam("soDienThoai") String soDienThoai,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes) {
        try {
            if (taiKhoanRepository.existsByTenDangNhap(soDienThoai)) {
                redirectAttributes.addAttribute("regError", "Số điện thoại này đã được đăng ký tài khoản!");
                return "redirect:/login";
            }
            VaiTro roleCustomer = vaiTroRepository.findByMa("ROLE_CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Chưa cấu hình Vai trò"));
            TaiKhoan newAccount = new TaiKhoan();
            newAccount.setTenDangNhap(soDienThoai);
            newAccount.setMatKhau(passwordEncoder.encode(password));
            newAccount.setVaiTro(roleCustomer);
            newAccount.setTrangThai(true);
            TaiKhoan savedAccount = taiKhoanRepository.save(newAccount);
            Optional<KhachHang> khachPos = khachHangRepository.findBySoDienThoai(soDienThoai);
            if (khachPos.isPresent()) {
                KhachHang existingKh = khachPos.get();
                existingKh.setTaiKhoan(savedAccount);
                existingKh.setHoTen(hoTen);
                khachHangRepository.save(existingKh);
            } else {
                // TRƯỜNG HỢP B: Khách mới toanh -> Tạo mới 100%
                KhachHang newCustomer = new KhachHang();
                newCustomer.setMa("KH" + System.currentTimeMillis());
                newCustomer.setHoTen(hoTen);
                newCustomer.setSoDienThoai(soDienThoai);
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

    @GetMapping("/login")
    public String login() {
        return "login/login";
    }
}
