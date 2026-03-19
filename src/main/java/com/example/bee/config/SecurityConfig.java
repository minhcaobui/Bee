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
                        .requestMatchers("/register", "/login", "/css/**", "/js/**", "/images/**", "/api/**", "/api/products/**", "/api/hoa-don/tra-cuu/**", "/api/hoa-don/checkout", "/customer/**").permitAll()
                        .requestMatchers("/customer/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers("/api/khach-hang/my-profile", "/api/khach-hang/change-password", "/api/khach-hang/*/dia-chi/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers("/dashboards/**", "/api/thong-ke/**", "/api/nhan-vien/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/nhan-vien/my-profile", "/api/nhan-vien/change-password").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/hoa-don/my-orders").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers("/staff/**", "/dashboards/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/admin/**", "/products/**", "/pos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/khach-hang", "/api/khach-hang/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/hoa-don", "/api/hoa-don/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
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