package com.example.bee.repositories.cart;

import com.example.bee.entities.cart.GioHangChiTiet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GioHangChiTietRepository extends JpaRepository<GioHangChiTiet, Integer> {

    @EntityGraph(attributePaths = {"sanPhamChiTiet", "sanPhamChiTiet.sanPham", "sanPhamChiTiet.mauSac", "sanPhamChiTiet.kichThuoc"})
    List<GioHangChiTiet> findByGioHang_Id(Integer gioHangId);

    GioHangChiTiet findByGioHang_IdAndSanPhamChiTiet_Id(Integer gioHangId, Integer sanPhamChiTietId);
}