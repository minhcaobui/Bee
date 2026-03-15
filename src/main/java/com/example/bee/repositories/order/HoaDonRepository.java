package com.example.bee.repositories.order;

import com.example.bee.entities.order.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {

    List<HoaDon> findByLoaiHoaDonAndTrangThaiHoaDonMa(Integer loai, String maTrangThai);

    List<HoaDon> findByLoaiHoaDonAndTrangThaiHoaDonMaInOrderByNgayTaoDesc(Integer loai, List<String> mas);

    HoaDon findByMa(String ma);

    @Query("SELECT h FROM HoaDon h WHERE h.loaiHoaDon = 0 OR (h.loaiHoaDon = 1 AND h.trangThaiHoaDon.ma IN ('HOAN_THANH', 'DA_HUY')) ORDER BY h.ngayTao DESC")
    List<HoaDon> findLichSuHoaDon();

    List<HoaDon> findByKhachHangIdOrderByNgayTaoDesc(Integer khachHangId);
}