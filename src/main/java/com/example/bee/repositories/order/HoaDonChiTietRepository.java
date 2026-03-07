package com.example.bee.repositories.order;

import com.example.bee.entities.order.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {

    @Query("SELECT ct FROM HoaDonChiTiet ct " +
            "JOIN FETCH ct.sanPhamChiTiet spct " +
            "JOIN FETCH spct.sanPham " +
            "JOIN FETCH spct.mauSac " +
            "JOIN FETCH spct.kichThuoc " +
            "WHERE ct.hoaDon.id = :idHoaDon")
    List<HoaDonChiTiet> findByHoaDonId(Integer idHoaDon);

    void deleteByHoaDonId(Integer id);

    // THÊM DÒNG NÀY ĐỂ HẾT LỖI:
    // Tìm chính xác 1 dòng dựa trên ID hóa đơn và ID sản phẩm chi tiết
    Optional<HoaDonChiTiet> findByHoaDonIdAndSanPhamChiTietId(Integer hoaDonId, Integer sanPhamChiTietId);

}