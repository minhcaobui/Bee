package com.example.bee.repositories.order;

import com.example.bee.entities.order.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {

    HoaDon findByMa(String ma);

    List<HoaDon> findByKhachHangIdOrderByNgayTaoDesc(Integer khachHangId);

    @EntityGraph(attributePaths = {"khachHang", "nhanVien", "trangThaiHoaDon"})
    @Query("SELECT h FROM HoaDon h " +
            "LEFT JOIN h.khachHang kh " +
            "LEFT JOIN h.nhanVien nv " +
            "LEFT JOIN h.trangThaiHoaDon tt " +
            "WHERE TRIM(tt.ma) NOT IN ('HOAN_THANH', 'DA_HUY', 'DA_DOI', 'DA_TRA') " +
            "AND (:q IS NULL OR LOWER(h.ma) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(kh.hoTen) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(CAST(h.thongTinGiaoHang AS string)) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:statusId IS NULL OR tt.id = :statusId) " +
            "AND (:loaiHoaDon IS NULL OR h.loaiHoaDon = :loaiHoaDon) " +
            "AND (:phuongThuc IS NULL OR EXISTS (SELECT t FROM ThanhToan t WHERE t.hoaDon = h AND t.phuongThuc = :phuongThuc)) " +
            "AND (cast(:startDate as date) IS NULL OR h.ngayTao >= :startDate) " +
            "AND (cast(:endDate as date) IS NULL OR h.ngayTao <= :endDate) " +
            "ORDER BY h.ngayTao DESC")
    Page<HoaDon> searchDonHangChoXuLy(
            @Param("q") String q, @Param("statusId") Integer statusId,
            @Param("loaiHoaDon") Integer loaiHoaDon, @Param("phuongThuc") String phuongThuc,
            @Param("startDate") Date startDate, @Param("endDate") Date endDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"khachHang", "nhanVien", "trangThaiHoaDon"})
    @Query("SELECT h FROM HoaDon h " +
            "LEFT JOIN h.khachHang kh " +
            "LEFT JOIN h.nhanVien nv " +
            "LEFT JOIN h.trangThaiHoaDon tt " +
            "WHERE TRIM(tt.ma) IN ('HOAN_THANH', 'DA_HUY', 'DA_DOI', 'DA_TRA') " +
            "AND (:q IS NULL OR LOWER(h.ma) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(kh.hoTen) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(CAST(h.thongTinGiaoHang AS string)) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:statusId IS NULL OR tt.id = :statusId) " +
            "AND (:nhanVienId IS NULL OR nv.id = :nhanVienId) " +
            "AND (:loaiHoaDon IS NULL OR h.loaiHoaDon = :loaiHoaDon) " +
            "AND (:phuongThuc IS NULL OR EXISTS (SELECT t FROM ThanhToan t WHERE t.hoaDon = h AND t.phuongThuc = :phuongThuc)) " +
            "AND (cast(:startDate as date) IS NULL OR h.ngayTao >= :startDate) " +
            "AND (cast(:endDate as date) IS NULL OR h.ngayTao <= :endDate) " +
            "ORDER BY h.ngayTao DESC")
    Page<HoaDon> searchLichSuHoaDon(
            @Param("q") String q, @Param("statusId") Integer statusId, @Param("nhanVienId") Integer nhanVienId,
            @Param("loaiHoaDon") Integer loaiHoaDon, @Param("phuongThuc") String phuongThuc,
            @Param("startDate") Date startDate, @Param("endDate") Date endDate,
            Pageable pageable
    );

    List<HoaDon> findTop5ByLoaiHoaDonAndTrangThaiHoaDon_MaInOrderByNgayTaoDesc(Integer loaiHoaDon, List<String> maTrangThais);

    boolean existsByKhachHangIdAndMaGiamGiaIdAndTrangThaiHoaDon_MaNot(Integer khachHangId, Integer maGiamGiaId, String trangThaiMa);
}