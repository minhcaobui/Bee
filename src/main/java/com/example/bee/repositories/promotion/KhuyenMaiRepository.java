package com.example.bee.repositories.promotion;

import com.example.bee.entities.promotion.KhuyenMai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {

    // Check trùng mã
    boolean existsByMa(String ma);

    boolean existsByMaIgnoreCase(String ma);

    // Tìm các khuyến mãi hết hạn để Scheduler quét
    List<KhuyenMai> findByTrangThaiAndNgayKetThucBefore(Boolean trangThai, LocalDateTime now);

    // --- HÀM SEARCH TỔNG HỢP (Chỉ giữ lại 1 hàm này thôi) ---
    @Query("SELECT k FROM KhuyenMai k WHERE " +
            "(:keyword IS NULL OR LOWER(k.ten) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(k.ma) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:trangThai IS NULL OR k.trangThai = :trangThai) AND " +
            "(:from IS NULL OR k.ngayBatDau >= :from) AND " +
            "(:to IS NULL OR k.ngayKetThuc <= :to)")
    Page<KhuyenMai> searchEverything(
            @Param("keyword") String keyword,
            @Param("trangThai") Boolean trangThai,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("SELECT DISTINCT km FROM KhuyenMai km " +
            "JOIN km.sanPhams sp " +
            "WHERE sp.id IN :listIdSp " +
            "AND km.trangThai = true " +
            "AND km.id <> :idHienTai " + // Loại trừ chính nó (khi update)
            "AND ( " +
            "   (km.ngayBatDau <= :ketThuc AND km.ngayKetThuc >= :batDau) " + // Logic giao nhau thời gian
            ")")
    List<KhuyenMai> checkTrungLich(
            @Param("listIdSp") List<Integer> listIdSp,
            @Param("batDau") LocalDateTime batDau,
            @Param("ketThuc") LocalDateTime ketThuc,
            @Param("idHienTai") Integer idHienTai
    );

    // 1. Check cho DANH MỤC (Thêm DISTINCT)
    @Query("SELECT COUNT(DISTINCT km) FROM KhuyenMai km " + // <--- Thêm DISTINCT ở đây
            "JOIN KhuyenMaiSanPham kmsp ON km.id = kmsp.idKhuyenMai " +
            "JOIN SanPham sp ON kmsp.idSanPham = sp.id " +
            "WHERE sp.danhMuc.id = :id " +
            "AND km.trangThai = true AND km.ngayKetThuc > CURRENT_TIMESTAMP")
    long countByDanhMuc(Integer id);

    // 2. Check cho HÃNG (Thêm DISTINCT)
    @Query("SELECT COUNT(DISTINCT km) FROM KhuyenMai km " + // <--- Thêm DISTINCT
            "JOIN KhuyenMaiSanPham kmsp ON km.id = kmsp.idKhuyenMai " +
            "JOIN SanPham sp ON kmsp.idSanPham = sp.id " +
            "WHERE sp.hang.id = :id " +
            "AND km.trangThai = true AND km.ngayKetThuc > CURRENT_TIMESTAMP")
    long countByHang(Integer id);

    // 3. Check cho CHẤT LIỆU (Thêm DISTINCT)
    @Query("SELECT COUNT(DISTINCT km) FROM KhuyenMai km " + // <--- Thêm DISTINCT
            "JOIN KhuyenMaiSanPham kmsp ON km.id = kmsp.idKhuyenMai " +
            "JOIN SanPham sp ON kmsp.idSanPham = sp.id " +
            "WHERE sp.chatLieu.id = :id " +
            "AND km.trangThai = true AND km.ngayKetThuc > CURRENT_TIMESTAMP")
    long countByChatLieu(Integer id);

    @Query("SELECT COUNT(sp) FROM SanPham sp " +
            "JOIN KhuyenMaiSanPham kmsp ON sp.id = kmsp.idSanPham " +
            "WHERE kmsp.idKhuyenMai = :kmId " +
            "AND sp.trangThai = true " +
            "AND sp.danhMuc.trangThai = true " +
            "AND sp.hang.trangThai = true " +
            "AND sp.chatLieu.trangThai = true") // Thêm các điều kiện khác nếu cần
    long countValidProductsInPromotion(@Param("kmId") Integer kmId);
}