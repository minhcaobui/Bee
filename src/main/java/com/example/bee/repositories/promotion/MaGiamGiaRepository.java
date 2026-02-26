package com.example.bee.repositories.promotion;

import com.example.bee.entities.promotion.MaGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MaGiamGiaRepository extends JpaRepository<MaGiamGia, Integer> {

    @Query("SELECT v FROM MaGiamGia v WHERE " +
            "(:keyword IS NULL OR v.maCode LIKE %:keyword% OR v.ten LIKE %:keyword%) " +
            "AND (:trangThai IS NULL OR v.trangThai = :trangThai) " +
            "AND (:startDate IS NULL OR v.ngayBatDau >= :startDate) " +
            "AND (:endDate IS NULL OR v.ngayKetThuc <= :endDate)")
    Page<MaGiamGia> searchVoucher(String keyword, Boolean trangThai,
                                  LocalDateTime startDate, LocalDateTime endDate,
                                  Pageable pageable);

    boolean existsByMaCodeIgnoreCase(String maCode);

    Optional<MaGiamGia> findByMaCode(String code);
}