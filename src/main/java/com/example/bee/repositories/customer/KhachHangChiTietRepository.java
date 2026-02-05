package com.example.bee.repositories.customer;

import com.example.bee.entities.customer.KhachHangChiTiet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KhachHangChiTietRepository extends JpaRepository<KhachHangChiTiet, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    boolean existsBySoDienThoai(String soDienThoai);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT k FROM KhachHangChiTiet k WHERE " +
            "(:q IS NULL OR " +
            " LOWER(k.ma) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%')) OR " +
            " LOWER(k.hoTen) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%')) OR " +
            " LOWER(k.soDienThoai) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%')) OR " +
            " LOWER(k.email) LIKE LOWER(CONCAT('%', COALESCE(:q, ''), '%'))) " +
            "AND (:trangThai IS NULL OR k.trangThai = :trangThai)")
    Page<KhachHangChiTiet> search(@Param("q") String q,
                                  @Param("trangThai") Boolean trangThai,
                                  Pageable pageable);
}