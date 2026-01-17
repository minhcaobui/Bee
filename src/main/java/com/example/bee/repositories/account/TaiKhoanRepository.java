package com.example.bee.repositories.account;

import com.example.bee.entities.account.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {
    // Tìm kiếm theo username hoặc email
    Optional<TaiKhoan> findByTenDangNhapOrEmail(String tenDangNhap, String email);

    boolean existsByTenDangNhap(String tenDangNhap);
}