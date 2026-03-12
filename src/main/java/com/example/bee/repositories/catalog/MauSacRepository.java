package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.MauSac;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MauSacRepository extends JpaRepository<MauSac, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Integer id);

    Optional<MauSac> findByMa(String ma);

    @Query("SELECT m FROM MauSac m WHERE " +  // ✅ FIX: MauSac thay vì ChatLieu
            "(:q IS NULL OR LOWER(m.ma) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(m.ten) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:trangThai IS NULL OR m.trangThai = :trangThai)")
    Page<MauSac> search(@Param("q") String q,
                        @Param("trangThai") Boolean trangThai,
                        Pageable pageable);

    boolean existsByTenIgnoreCase(String newTen);
    List<MauSac> findByTrangThaiTrue();
}