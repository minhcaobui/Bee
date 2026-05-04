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
            "LEFT JOIN d.taiKhoan tk " +
            "LEFT JOIN KhachHang kh ON kh.taiKhoan.id = tk.id " + // Join để lấy thông tin KH
            "LEFT JOIN d.sanPham sp " + // Join để lấy thông tin SP
            "WHERE (:keyword IS NULL OR " +
            "       LOWER(kh.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "       kh.soDienThoai LIKE CONCAT('%', :keyword, '%') OR " +
            "       LOWER(sp.ten) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "       LOWER(sp.ma) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "       LOWER(d.noiDung) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "  AND (:soSao IS NULL OR d.soSao = :soSao) " +
            "  AND (:trangThai IS NULL OR " +
            "      (:trangThai = 'DA_TRA_LOI' AND d.noiDungTraLoi IS NOT NULL) OR " +
            "      (:trangThai = 'CHUA_TRA_LOI' AND d.noiDungTraLoi IS NULL))")
    Page<DanhGia> findAdminReviews(
            @Param("keyword") String keyword,
            @Param("soSao") Integer soSao,
            @Param("trangThai") String trangThai,
            Pageable pageable);
}
