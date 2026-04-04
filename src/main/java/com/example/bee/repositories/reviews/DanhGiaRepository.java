package com.example.bee.repositories.reviews;

import com.example.bee.entities.reviews.DanhGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Long> {
    List<DanhGia> findBySanPhamIdOrderByNgayTaoDesc(Integer sanPhamId);

    Optional<DanhGia> findByHoaDonChiTiet_Id(Integer hoaDonChiTietId);

    @Query("SELECT d FROM DanhGia d " +
            "WHERE (:soSao IS NULL OR d.soSao = :soSao) " +
            "AND (:trangThai IS NULL OR :trangThai = '' " +
            "     OR (:trangThai = 'CHUA_TRA_LOI' AND d.noiDungTraLoi IS NULL) " +
            "     OR (:trangThai = 'DA_TRA_LOI' AND d.noiDungTraLoi IS NOT NULL)) " +
            "AND (:q IS NULL OR :q = '' " +
            "     OR LOWER(d.noiDung) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(d.sanPham.ten) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<DanhGia> findAdminReviews(@Param("q") String q,
                                   @Param("soSao") Integer soSao,
                                   @Param("trangThai") String trangThai,
                                   Pageable pageable);
}
