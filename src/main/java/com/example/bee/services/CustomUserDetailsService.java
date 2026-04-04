package com.example.bee.services;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.repositories.account.TaiKhoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException; // 🌟 IMPORT THÊM CÁI NÀY
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
    public UserDetails loadUserByUsername(String loginIdentifier) throws UsernameNotFoundException {
        TaiKhoan taiKhoan = taiKhoanRepository.findByLoginIdentifier(loginIdentifier)
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));

        // 🌟 TỰ TAY BẮT VÀ NÉM LỖI KHÓA TÀI KHOẢN (Chặn ngay từ vòng gửi xe)
        if (taiKhoan.getTrangThai() != null && !taiKhoan.getTrangThai()) {
            throw new DisabledException("Tài khoản của bạn đã bị khóa!");
        }

        String roleCode = taiKhoan.getVaiTro().getMa();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleCode);

        // Trả về User bình thường vì đã chặn người bị khóa ở trên rồi
        return new User(
                taiKhoan.getTenDangNhap(),
                taiKhoan.getMatKhau(),
                true, true, true, true,
                Collections.singletonList(authority)
        );
    }
}