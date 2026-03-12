package com.example.bee.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Cho phép tất cả truy cập trang đăng nhập và tài nguyên tĩnh
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()

                        // 2. QUYỀN ADMIN: Các request nhạy cảm chỉ Giám đốc/Quản lý mới được vào
                        .requestMatchers("/staff", "/dashboards", "/promotions").hasAuthority("ROLE_ADMIN")

                        // 3. QUYỀN STAFF + ADMIN: Các chức năng bán hàng, sản phẩm, khách hàng
                        .requestMatchers("/admin", "/catalogs", "/products", "/pos", "/orders", "/customers").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

                        // 4. Các request còn lại trong hệ thống đều yêu cầu phải đăng nhập
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // Cấu hình không redirect để tránh lỗi AJAX lồng layout
                            boolean isApi = request.getRequestURI().startsWith("/api/");

                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setCharacterEncoding("UTF-8");

                            if (isApi) {
                                // Nếu là lời gọi API (như đổi trạng thái) bị chặn
                                response.setContentType("application/json");
                                response.getWriter().write("{\"message\": \"Bạn không có quyền thực hiện thao tác này!\"}");
                            } else {
                                // Nếu là lời gọi load giao diện HTML bị chặn
                                response.setContentType("text/html; charset=UTF-8");
                                response.getWriter().write(
                                        "<div style=\"display:flex; flex-direction:column; align-items:center; justify-content:center; padding: 50px; text-align: center; height: 100%;\">" +
                                                "<h3 style=\"color: #ef4444; font-weight: 800; font-family: 'Inter', sans-serif; text-transform: uppercase;\">Truy Cập Bị Từ Chối</h3>" +
                                                "<p style=\"color: #666; font-family: 'Inter', sans-serif;\">Tài khoản của bạn không được cấp quyền để xem chức năng này.</p>" +
                                                "</div>" +
                                                "<script>" +
                                                "if(typeof window.toast === 'function') { " +
                                                "window.toast('Bạn không có quyền truy cập vào chức năng này!', 'error'); " +
                                                "} else { " +
                                                "alert('Bạn không có quyền truy cập!'); " +
                                                "}" +
                                                "</script>"
                                );
                            }
                        })
                );

        return http.build();
    }
}