package com.example.bee.repositories.order;

import com.example.bee.entities.order.HoaDon;
import com.example.bee.entities.order.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {

    @Query("SELECT ct FROM HoaDonChiTiet ct " +
            "JOIN FETCH ct.sanPhamChiTiet spct " +
            "JOIN FETCH spct.sanPham " +
            "JOIN FETCH spct.mauSac " +
            "JOIN FETCH spct.kichThuoc " +
            "WHERE ct.hoaDon.id = :idHoaDon")
    List<HoaDonChiTiet> findByHoaDonId(@Param("idHoaDon") Integer idHoaDon);

    List<HoaDonChiTiet> findByHoaDon(HoaDon hoaDon);

    List<HoaDonChiTiet> findByHoaDon_Id(Integer id);

    @Query("SELECT spct.sanPham.id, SUM(ct.soLuong) " +
            "FROM HoaDonChiTiet ct " +
            "JOIN ct.sanPhamChiTiet spct " +
            "JOIN ct.hoaDon hd " +
            "WHERE TRIM(hd.trangThaiHoaDon.ma) = 'HOAN_THANH' " +
            "GROUP BY spct.sanPham.id")
    List<Object[]> countTotalSoldPerProduct();
}