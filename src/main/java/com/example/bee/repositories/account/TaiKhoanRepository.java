package com.example.bee.repositories.account;

import com.example.bee.entities.account.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {

    @Query("SELECT tk FROM TaiKhoan tk " +
            "LEFT JOIN NhanVien nv ON nv.taiKhoan.id = tk.id " +
            "LEFT JOIN KhachHang kh ON kh.taiKhoan.id = tk.id " +
            "WHERE (tk.tenDangNhap = :loginIdentifier " +
            "OR nv.email = :loginIdentifier OR nv.soDienThoai = :loginIdentifier " +
            "OR kh.email = :loginIdentifier OR kh.soDienThoai = :loginIdentifier)")
    Optional<TaiKhoan> findByLoginIdentifier(@Param("loginIdentifier") String loginIdentifier);

    boolean existsByTenDangNhap(String tenDangNhap);

    Optional<TaiKhoan> findByTenDangNhap(String username);

    List<TaiKhoan> findByVaiTro_Ma(String roleCustomer);


}