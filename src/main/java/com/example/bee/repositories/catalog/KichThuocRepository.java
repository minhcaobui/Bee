package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.KichThuoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KichThuocRepository extends JpaRepository<KichThuoc, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    @Query("SELECT k FROM KichThuoc k WHERE " +  // ✅ FIX: KichThuoc
            "(:q IS NULL OR LOWER(k.ma) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(k.ten) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:trangThai IS NULL OR k.trangThai = :trangThai)")
    Page<KichThuoc> search(@Param("q") String q,
                           @Param("trangThai") Boolean trangThai,
                           Pageable pageable);

    boolean existsByTenIgnoreCase(String newTen);

    List<KichThuoc> findByTrangThaiTrue();
}