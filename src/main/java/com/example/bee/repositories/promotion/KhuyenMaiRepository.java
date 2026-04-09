package com.example.bee.repositories.promotion;

import com.example.bee.entities.promotion.KhuyenMai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {

    boolean existsByMa(String ma);

    boolean existsByMaIgnoreCase(String ma);

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
            "AND km.id <> :idHienTai " +
            "AND ( " +
            "   (km.ngayBatDau <= :ketThuc AND km.ngayKetThuc >= :batDau) " +
            ")")
    List<KhuyenMai> checkTrungLich(
            @Param("listIdSp") List<Integer> listIdSp,
            @Param("batDau") LocalDateTime batDau,
            @Param("ketThuc") LocalDateTime ketThuc,
            @Param("idHienTai") Integer idHienTai
    );

    @Query("SELECT COUNT(sp) FROM SanPham sp " +
            "JOIN KhuyenMaiSanPham kmsp ON sp.id = kmsp.idSanPham " +
            "WHERE kmsp.idKhuyenMai = :kmId " +
            "AND sp.trangThai = true")
    long countValidProductsInPromotion(@Param("kmId") Integer kmId);

    @Modifying
    @Transactional
    @Query("UPDATE KhuyenMai k SET k.trangThai = false " +
            "WHERE k.trangThai = true AND k.ngayKetThuc < :now")
    void autoDeactivateExpiredPromotions(@Param("now") LocalDateTime now);

    @Query("SELECT km FROM KhuyenMai km JOIN KhuyenMaiSanPham kmsp ON km.id = kmsp.idKhuyenMai " +
            "WHERE kmsp.idSanPham = :spId " +
            "AND km.trangThai = true " +
            "AND km.ngayBatDau <= CURRENT_TIMESTAMP " +
            "AND km.ngayKetThuc >= CURRENT_TIMESTAMP")
    List<KhuyenMai> findActiveKhuyenMaiBySanPhamId(@org.springframework.data.repository.query.Param("spId") Integer spId);
}