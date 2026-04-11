package com.example.bee.configs;

import com.example.bee.entities.staff.NhanVien;
import com.example.bee.repositories.staff.NhanVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final NhanVienRepository nhanVienRepository;

    @ModelAttribute("loggedInUser")
    public NhanVien getLoggedInUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            return nhanVienRepository.findByTaiKhoan_TenDangNhap(username).orElse(null);
        }
        return null;
    }
}
