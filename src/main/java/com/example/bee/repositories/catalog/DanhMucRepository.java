package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.DanhMuc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DanhMucRepository extends JpaRepository<DanhMuc, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    @Query("SELECT d FROM DanhMuc d WHERE " +
            "(:q IS NULL OR LOWER(d.ma) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(d.ten) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:trangThai IS NULL OR d.trangThai = :trangThai)")
    Page<DanhMuc> search(@Param("q") String q,
                          @Param("trangThai") Boolean trangThai,
                          Pageable pageable);

    boolean existsByTenIgnoreCase(String ten);
    boolean existsByTenIgnoreCaseAndIdNot(String ten, Integer id);
}
