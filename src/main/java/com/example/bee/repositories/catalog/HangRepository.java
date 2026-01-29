package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.entities.catalog.Hang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HangRepository extends JpaRepository<Hang, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    @Query("SELECT h FROM Hang h WHERE " +
            "(:q IS NULL OR LOWER(h.ma) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(h.ten) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:trangThai IS NULL OR h.trangThai = :trangThai)")
    Page<Hang> search(@Param("q") String q,
                          @Param("trangThai") Boolean trangThai,
                          Pageable pageable);

    boolean existsByTenIgnoreCase(String newTen);
}
