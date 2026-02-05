package com.example.bee.repositories.customer;

import com.example.bee.entities.customer.DiaChiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaChiKhachHangRepository extends JpaRepository<DiaChiKhachHang, Integer> {

    List<DiaChiKhachHang> findByKhachHangChiTietIdAndTrangThaiTrue(Integer khachHangChiTietId);

    List<DiaChiKhachHang> findByKhachHangChiTietId(Integer khachHangChiTietId);

    @Modifying
    @Query("UPDATE DiaChiKhachHang d SET d.laMacDinh = false " +
            "WHERE d.khachHangChiTiet.id = :khachHangChiTietId " +
            "AND (:excludeId IS NULL OR d.id != :excludeId)")
    void updateLaMacDinhToFalseByKhachHangChiTietId(
            @Param("khachHangChiTietId") Integer khachHangChiTietId,
            @Param("excludeId") Integer excludeId);
}