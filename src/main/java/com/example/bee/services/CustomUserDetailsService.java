package com.example.bee.services;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.repositories.account.TaiKhoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final TaiKhoanRepository taiKhoanRepository;

    @Override
    public UserDetails loadUserByUsername(String loginIdentifier) throws UsernameNotFoundException {
        TaiKhoan taiKhoan = taiKhoanRepository.findByLoginIdentifier(loginIdentifier)
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));
        if (taiKhoan.getTrangThai() != null && !taiKhoan.getTrangThai()) {
            throw new DisabledException("Tài khoản của bạn đã bị khóa!");
        }
        String roleCode = taiKhoan.getVaiTro().getMa();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleCode);
        return new User(
                taiKhoan.getTenDangNhap(),
                taiKhoan.getMatKhau(),
                true, true, true, true,
                Collections.singletonList(authority)
        );
    }
}