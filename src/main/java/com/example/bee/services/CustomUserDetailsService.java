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
    public UserDetails loadUserByUsername(String loginIdentifier) throws UsernameNotFoundException {
        // 1. Tìm tài khoản bằng Tên đăng nhập / Email / SĐT (thông qua hàm custom trong Repo)
        TaiKhoan taiKhoan = taiKhoanRepository.findByLoginIdentifier(loginIdentifier)
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản, Email hoặc Số điện thoại không tồn tại hoặc bị khóa"));

        // 2. Lấy mã vai trò (ví dụ: ROLE_ADMIN, ROLE_CUSTOMER)
        String roleCode = taiKhoan.getVaiTro().getMa();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleCode);

        // 3. Trả về đối tượng User của Spring Security
        // Lưu ý: Dù đăng nhập bằng Email hay SĐT, ta vẫn nên trả về tenDangNhap làm username chính của phiên làm việc
        return new User(
                taiKhoan.getTenDangNhap(),
                taiKhoan.getMatKhau(),
                Collections.singletonList(authority)
        );
    }
}