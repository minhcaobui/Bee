package com.example.bee.repositories.customer;

import com.example.bee.entities.customer.KhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    boolean existsBySoDienThoai(String sdt);

    boolean existsByEmail(String email);

    boolean existsBySoDienThoaiAndIdNot(String sdt, Integer id);

    boolean existsByEmailAndIdNot(String email, Integer id);

    @Query("SELECT k FROM KhachHang k WHERE " +
            "(:q IS NULL OR k.hoTen LIKE %:q% OR k.soDienThoai LIKE %:q% OR k.email LIKE %:q% OR k.ma LIKE %:q%) " +
            "AND (:trangThai IS NULL OR k.trangThai = :trangThai)")
    Page<KhachHang> search(@Param("q") String q, @Param("trangThai") Boolean trangThai, Pageable pageable);

    Optional<KhachHang> findBySoDienThoai(String sdt);

    List<KhachHang> findBySoDienThoaiContainingOrHoTenContaining(String q, String q1);

    Optional<KhachHang> findByTaiKhoan_TenDangNhap(String name);
}