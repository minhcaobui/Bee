package com.example.bee.repositories.account;

import com.example.bee.entities.account.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {

    @Query("SELECT tk FROM TaiKhoan tk " +
            "LEFT JOIN NhanVien nv ON nv.taiKhoan.id = tk.id " +
            "LEFT JOIN KhachHang kh ON kh.taiKhoan.id = tk.id " +
            "WHERE tk.trangThai = true " +
            "AND (tk.tenDangNhap = :loginStr " +
            "OR nv.email = :loginStr OR nv.soDienThoai = :loginStr " +
            "OR kh.email = :loginStr OR kh.soDienThoai = :loginStr)")
    Optional<TaiKhoan> findByLoginIdentifier(@Param("loginStr") String loginStr);

    boolean existsByTenDangNhap(String tenDangNhap);

    Optional<TaiKhoan> findByTenDangNhap(String username);
}