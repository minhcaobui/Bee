package com.example.bee.repositories.cart;

import com.example.bee.entities.cart.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GioHangRepository extends JpaRepository<GioHang, Integer> {
    Optional<GioHang> findByTaiKhoan_Id(Integer taiKhoanId);
    Optional<GioHang> findByTaiKhoan_TenDangNhap(String tenDangNhap);
}