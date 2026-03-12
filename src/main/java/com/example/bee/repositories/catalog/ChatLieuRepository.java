package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.ChatLieu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatLieuRepository extends JpaRepository<ChatLieu, Integer> {

    boolean existsByMaIgnoreCase(String ma);


    @Query("SELECT c FROM ChatLieu c WHERE " +
            "(:q IS NULL OR LOWER(c.ma) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(c.ten) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:trangThai IS NULL OR c.trangThai = :trangThai)")
    Page<ChatLieu> search(@Param("q") String q,
                          @Param("trangThai") Boolean trangThai,
                          Pageable pageable);

    boolean existsByTenIgnoreCase(String ten);
    List<ChatLieu> findByTrangThaiTrue();
}

