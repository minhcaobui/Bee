package com.example.bee.repositories.product;

import com.example.bee.entities.product.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {
    @Query("SELECT s FROM SanPham s " +
            "WHERE s.trangThai = true " +
            "AND s.danhMuc.trangThai = true " +
            "AND s.hang.trangThai = true " +
            "AND s.chatLieu.trangThai = true")
        // Nếu có quan hệ MauSac/KichThuoc trực tiếp ở bảng SanPham thì thêm vào nốt
        // VD: AND s.mauSac.trangThai = true
    List<SanPham> getAllActiveProducts();
}