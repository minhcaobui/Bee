package com.example.bee.configs;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. PUBLIC ROUTES (Ai cũng truy cập được)
                        .requestMatchers("/", "/register", "/login", "/forgot-password/**", "/css/**", "/js/**", "/images/**", "/customer/**").permitAll()

                        // Các API công khai
                        .requestMatchers(
                                "/api/san-pham/**",
                                "/api/danh-muc/**",
                                "/api/hang/**",
                                "/api/chat-lieu/**",
                                "/api/mau-sac/**",
                                "/api/kich-thuoc/**",
                                "/api/hoa-don/tra-cuu/**",
                                "/api/hoa-don/thanh-toan",
                                "/api/ma-giam-gia/hoat-dong",
                                "/api/khuyen-mai/**",
                                "/api/tro-ly-ao/**",
                                "/api/gio-hang/**",
                                "/api/chung/**",        // Cho phép gọi các hàm tiện ích, gửi OTP chung
                                "/api/thanh-toan/**"    // Cực kỳ quan trọng để Momo/VNPay gọi Callback thành công
                        ).permitAll()

                        // Cho phép xem đánh giá sản phẩm mà không cần đăng nhập
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/khach-hang/danh-gia/**").permitAll()

                        // 2. CHỈ DÀNH CHO ADMIN
                        .requestMatchers(
                                "/dashboards/**",
                                "/returns/**",
                                "/api/thong-ke/**",
                                "/api/chuc-vu/**",      // Quản lý chức vụ
                                "/staff/**"
                        ).hasAuthority("ROLE_ADMIN")

                        // Quyền riêng cho Admin thao tác trên toàn bộ nhân viên
                        .requestMatchers("/api/nhan-vien/**").hasAuthority("ROLE_ADMIN")

                        // 3. ADMIN VÀ STAFF CÓ THỂ XEM/SỬA HỒ SƠ CỦA CHÍNH MÌNH
                        .requestMatchers(
                                "/api/nhan-vien/ho-so-cua-toi",
                                "/api/nhan-vien/doi-mat-khau"
                        ).hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

                        // 4. API CHO NGƯỜI DÙNG ĐÃ ĐĂNG NHẬP (Customer, Staff, Admin đều dùng được)
                        .requestMatchers(
                                "/api/tai-len",
                                "/api/khach-hang/danh-gia/**",
                                "/api/khach-hang/yeu-thich/**",
                                "/api/khach-hang/ho-so-cua-toi",
                                "/api/khach-hang/doi-mat-khau-ca-nhan",
                                "/api/hoa-don/cua-toi",
                                "/api/ma-giam-gia/**",
                                "/api/khach-hang/dia-chi-ca-nhan/**",
                                "/api/khach-hang/danh-gia-cua-toi/**",
                                "/api/hoa-don/voucher-da-dung",
                                "/api/hoa-don/**",
                                "/api/thong-bao/**"
                        ).hasAnyAuthority("ROLE_CUSTOMER", "ROLE_STAFF", "ROLE_ADMIN")

                        // 5. CÁC API QUẢN LÝ (Chỉ Admin và Staff mới được thao tác)
                        .requestMatchers(
                                "/admin/**",
                                "/products/**",
                                "/pos/**",
                                "/api/ban-hang/**",         // API POS bán hàng
                                "/api/doi-tra/**",          // API xử lý đổi trả
                                "/api/khach-hang/**",       // Quản lý khách hàng từ phía Admin
                                "/api/quan-ly-danh-gia/**", // Quản lý review
                                "/api/**"                   // Quét toàn bộ các API quản lý còn lại
                        ).hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

                        // Mọi request khác đều phải đăng nhập
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