package com.example.bee.services;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.repositories.account.TaiKhoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Tìm tài khoản trong DB
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhapAndTrangThaiTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản hoặc tài khoản bị khóa"));

        // 2. Chuyển đổi Role từ DB sang GrantedAuthority của Spring Security
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(taiKhoan.getVaiTro().getMa());

        // 3. Trả về đối tượng User của Spring Security
        return new User(
                taiKhoan.getTenDangNhap(),
                taiKhoan.getMatKhau(), // Đây là mật khẩu đã mã hóa BCrypt
                Collections.singletonList(authority)
        );
    }
}