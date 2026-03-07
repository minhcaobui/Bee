package com.example.bee.config;

import com.example.bee.entities.user.NhanVien;
import com.example.bee.repositories.role.NhanVienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private NhanVienRepository nhanVienRepository;

    // Biến "loggedInUser" sẽ được tự động thêm vào tất cả các file HTML
    @ModelAttribute("loggedInUser")
    public NhanVien getLoggedInUser(Authentication authentication) {
        // Kiểm tra xem người dùng đã đăng nhập chưa
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {

            // Lấy email/tên đăng nhập từ Spring Security
            String username = authentication.getName();

            // Tìm NhanVien tương ứng trong database
            return nhanVienRepository.findByTaiKhoan_TenDangNhap(username).orElse(null);
        }
        return null;
    }
}
