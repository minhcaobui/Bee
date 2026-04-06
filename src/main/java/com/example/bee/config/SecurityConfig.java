package com.example.bee.config;

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
                        // 1. TÀI NGUYÊN CÔNG KHAI & TRANG ĐĂNG NHẬP
                        .requestMatchers("/register", "/login", "/css/**", "/js/**", "/images/**", "/customer/**").permitAll()

                        // 2. API CÔNG KHAI (Khách vãng lai dùng để xem hàng, tra đơn)
                        .requestMatchers(
                                "/api/products/**",
                                "/api/danh-muc/**",
                                "/api/mau-sac/**",
                                "/api/kich-thuoc/**",
                                "/api/hoa-don/tra-cuu/**",
                                "/api/hoa-don/checkout",
                                "/api/hoa-don/check-employee",
                                "/api/thong-bao/**",
                                "/api/vouchers/active",
                                "/api/khuyen-mai/**"
                        ).permitAll()

                        // Ai cũng được XEM đánh giá
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/khach-hang/reviews/**").permitAll()

                        // 3. API RIÊNG CỦA KHÁCH HÀNG (Phải có tài khoản mới được xài)
                        .requestMatchers(
                                "/api/upload", // 🌟 FIX LỖI 403: Cấp quyền upload ảnh cho khách hàng
                                "/api/khach-hang/reviews/**", // 🌟 Yêu cầu đăng nhập để VIẾT đánh giá và check-eligibility
                                "/api/khach-hang/wishlist/**",
                                "/api/khach-hang/my-profile",
                                "/api/khach-hang/change-password",
                                "/api/hoa-don/my-orders",
                                "/api/vouchers/**"
                        ).hasAnyAuthority("ROLE_CUSTOMER", "ROLE_STAFF", "ROLE_ADMIN")

                        // 4. API DÀNH CHO ADMIN & NHÂN VIÊN
                        .requestMatchers(
                                "/api/nhan-vien/my-profile",
                                "/api/nhan-vien/change-password",
                                "/admin/**",
                                "/products/**",
                                "/pos/**",
                                "/api/khach-hang/**", // Quản lý khách hàng
                                "/api/hoa-don/**",    // Quản lý hóa đơn
                                "/api/reviews/**",    // Quản lý đánh giá tổng
                                "/api/**"             // Các API còn lại (Phải nằm dưới cùng)
                        ).hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

                        // 5. API ĐỘC QUYỀN CỦA ADMIN
                        .requestMatchers(
                                "/dashboards/**",
                                "/returns/**",
                                "/api/thong-ke/**",
                                "/api/nhan-vien/**",
                                "/staff/**"
                        ).hasAuthority("ROLE_ADMIN")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler((request, response, exception) -> {
                            System.out.println("LỖI ĐĂNG NHẬP: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                            if (exception instanceof org.springframework.security.authentication.DisabledException ||
                                    exception.getCause() instanceof org.springframework.security.authentication.DisabledException ||
                                    (exception.getMessage() != null && exception.getMessage().contains("bị khóa"))) {
                                response.sendRedirect("/login?error=disabled");
                            } else {
                                response.sendRedirect("/login?error=invalid");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            boolean isApi = request.getRequestURI().startsWith("/api/");
                            if (isApi) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.setCharacterEncoding("UTF-8");
                                response.getWriter().write("{\"message\": \"Vui lòng đăng nhập để thực hiện!\"}");
                            } else {
                                response.sendRedirect("/login");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            boolean isApi = request.getRequestURI().startsWith("/api/");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setCharacterEncoding("UTF-8");
                            if (isApi) {
                                response.setContentType("application/json");
                                response.getWriter().write("{\"message\": \"Bạn không có quyền thực hiện thao tác này!\"}");
                            } else {
                                response.setContentType("text/html; charset=UTF-8");
                                response.getWriter().write(
                                        "<div style=\"display:flex; flex-direction:column; align-items:center; justify-content:center; padding: 50px; text-align: center; height: 100%;\">" +
                                                "<h3 style=\"color: #ef4444; font-weight: 800;\">Truy Cập Bị Từ Chối</h3>" +
                                                "<p style=\"color: #666;\">Tài khoản của bạn không có quyền xem chức năng này.</p>" +
                                                "</div>"
                                );
                            }
                        })
                );
        return http.build();
    }
}