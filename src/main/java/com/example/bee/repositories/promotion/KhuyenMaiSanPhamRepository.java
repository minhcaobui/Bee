package com.example.bee.repositories.promotion;

import com.example.bee.entities.promotion.KhuyenMaiSanPham;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KhuyenMaiSanPhamRepository extends JpaRepository<KhuyenMaiSanPham, Integer> {

    @Query("SELECT kmsp FROM KhuyenMaiSanPham kmsp JOIN KhuyenMai km ON kmsp.idKhuyenMai = km.id " +
            "WHERE kmsp.idSanPham IN :spIds AND km.trangThai = true " +
            "AND ((km.ngayBatDau <= :end AND km.ngayKetThuc >= :start))")
    List<KhuyenMaiSanPham> findKhuyenMaiTrungLap(@Param("spIds") List<Integer> spIds,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    List<KhuyenMaiSanPham> findAllByIdKhuyenMai(Integer idKhuyenMai);

    @Modifying
    @Transactional
    @Query("DELETE FROM KhuyenMaiSanPham k WHERE k.idKhuyenMai = :idKhuyenMai")
    void deleteByIdKhuyenMai(@org.springframework.data.repository.query.Param("idKhuyenMai") Integer idKhuyenMai);
}