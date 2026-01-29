package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.KichThuoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface KichThuocRepository extends JpaRepository<KichThuoc, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Integer id);

    Optional<KichThuoc> findByMa(String ma);

    @Query("SELECT k FROM KichThuoc k WHERE " +  // ✅ FIX: KichThuoc
            "(:q IS NULL OR LOWER(k.ma) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(k.ten) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:trangThai IS NULL OR k.trangThai = :trangThai)")
    Page<KichThuoc> search(@Param("q") String q,
                           @Param("trangThai") Boolean trangThai,
                           Pageable pageable);

    boolean existsByTenIgnoreCase(String newTen);
}