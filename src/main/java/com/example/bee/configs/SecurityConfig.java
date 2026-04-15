package com.example.bee.configs;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                        .requestMatchers("/register", "/login", "/forgot-password/**", "/css/**", "/js/**", "/images/**", "/customer/**", "/", "/api/payment/**").permitAll()

                        .requestMatchers(
                                "/api/products/**",
                                "/api/danh-muc/**",
                                "/api/mau-sac/**",
                                "/api/kich-thuoc/**",
                                "/api/hoa-don/tra-cuu/**",
                                "/api/hoa-don/checkout",
                                "/api/hoa-don/check-employee",
                                "/api/vouchers/active",
                                "/api/khuyen-mai/**",
                                "/api/chatbot/**",
                                "/api/gio-hang/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.PATCH, "/api/hoa-don/*/huy").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/hoa-don/momo-payment/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/hoa-don/vnpay-payment/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/hoa-don/*/confirm-transfer").permitAll()

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/khach-hang/reviews/**").permitAll()

                        .requestMatchers(
                                "/dashboards/**",
                                "/returns/**",
                                "/api/thong-ke/**",
                                "/staff/**"
                        ).hasAuthority("ROLE_ADMIN")

                        .requestMatchers(
                                "/api/nhan-vien/my-profile",
                                "/api/nhan-vien/change-password"
                        ).hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

                        .requestMatchers("/api/nhan-vien/**").hasAuthority("ROLE_ADMIN")

                        .requestMatchers(
                                "/api/upload",
                                "/api/khach-hang/reviews/**",
                                "/api/khach-hang/wishlist/**",
                                "/api/khach-hang/my-profile",
                                "/api/khach-hang/change-password",
                                "/api/hoa-don/my-orders",
                                "/api/vouchers/**",
                                "/api/khach-hang/addresses/**",
                                "/api/khach-hang/my-reviews/**",
                                "/api/hoa-don/my-used-vouchers/**",
                                "/api/hoa-don/**",
                                "/api/thong-bao/**"
                        ).hasAnyAuthority("ROLE_CUSTOMER", "ROLE_STAFF", "ROLE_ADMIN")

                        .requestMatchers(
                                "/admin/**",
                                "/products/**",
                                "/pos/**",
                                "/api/khach-hang/**",
                                "/api/reviews/**",
                                "/api/**"
                        ).hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")

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