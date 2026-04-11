package com.example.bee.controllers.account;

import com.example.bee.services.DangNhapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class DangNhapController {

    private final DangNhapService dangNhapService;

    @GetMapping("/login")
    public String loginPage() {
        return "login/login";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam("hoTen") String hoTen,
            @RequestParam("soDienThoai") String soDienThoai,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes) {
        return dangNhapService.dangKy(hoTen, soDienThoai, email, password, redirectAttributes);
    }

    @PostMapping("/forgot-password/send-otp")
    @ResponseBody
    public ResponseEntity<?> sendForgotOtp(@RequestParam String email) {
        return dangNhapService.guiOtpQuenMatKhau(email);
    }

    @PostMapping("/forgot-password/reset")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestParam String email, @RequestParam String otp) {
        return dangNhapService.datLaiMatKhau(email, otp);
    }
}