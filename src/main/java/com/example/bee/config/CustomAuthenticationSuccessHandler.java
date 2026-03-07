package com.example.bee.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component // Đánh dấu đây là một Bean để Spring quản lý
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // URL mặc định dành cho Khách hàng (Customer)
        String redirectUrl = "/";

        // Lấy danh sách quyền của người dùng vừa đăng nhập
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority grantedAuthority : authorities) {
            String role = grantedAuthority.getAuthority();

            // Nếu có quyền ADMIN hoặc STAFF thì đổi hướng sang trang Quản trị
            if (role.equals("ROLE_ADMIN") || role.equals("ROLE_STAFF")) {
                redirectUrl = "/admin";
                break; // Tìm thấy quyền cao rồi thì thoát vòng lặp
            }
        }

        // Thực hiện chuyển hướng
        response.sendRedirect(redirectUrl);
    }
}