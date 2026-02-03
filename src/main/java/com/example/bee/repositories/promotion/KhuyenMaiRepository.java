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
            "(:hinhThuc IS NULL OR k.hinhThuc = :hinhThuc) AND " + // Lọc theo Tab (Voucher/Sale)
            "(:keyword IS NULL OR LOWER(k.ten) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(k.ma) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:trangThai IS NULL OR k.trangThai = :trangThai) AND " +
            "(:from IS NULL OR k.ngayBatDau >= :from) AND " +
            "(:to IS NULL OR k.ngayKetThuc <= :to)")
    Page<KhuyenMai> searchEverything(
            @Param("keyword") String keyword,
            @Param("trangThai") Boolean trangThai,
            @Param("hinhThuc") Boolean hinhThuc,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}