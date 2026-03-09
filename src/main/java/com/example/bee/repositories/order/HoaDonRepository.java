package com.example.bee.repositories.order;

import com.example.bee.entities.order.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {
    List<HoaDon> findByLoaiHoaDonAndTrangThaiHoaDonMa(Integer loai, String maTrangThai);

    // Tab 1: ĐƠN HÀNG (Online đang xử lý)
    // Lấy đơn Online và có mã trạng thái nằm trong list (CHO_XAC_NHAN, CHO_GIAO, DANG_GIAO)
    List<HoaDon> findByLoaiHoaDonAndTrangThaiHoaDonMaInOrderByNgayTaoDesc(Integer loai, List<String> mas);
    HoaDon findByMa(String ma);


    // Tab 2: HÓA ĐƠN (Lịch sử)
    // Lấy tất cả tại quầy (loai=0) HOẶC Online đã xong/hủy
    @Query("SELECT h FROM HoaDon h WHERE h.loaiHoaDon = 0 OR (h.loaiHoaDon = 1 AND h.trangThaiHoaDon.ma IN ('HOAN_THANH', 'DA_HUY')) ORDER BY h.ngayTao DESC")
    List<HoaDon> findLichSuHoaDon();
}