package com.example.bee.repositories.staff;

import com.example.bee.entities.staff.ChucVu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChucVuRepository extends JpaRepository<ChucVu, Integer> {

    @Query("SELECT c FROM ChucVu c WHERE (:q IS NULL OR :q = '' OR c.ma LIKE %:q% OR c.ten LIKE %:q%) ORDER BY c.id DESC")
    Page<ChucVu> searchChucVu(@Param("q") String q, Pageable pageable);

    boolean existsByMa(String ma);

    boolean existsByMaAndIdNot(String ma, Integer id);
}