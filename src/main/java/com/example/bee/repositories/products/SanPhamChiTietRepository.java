package com.example.bee.repositories.products;

import com.example.bee.entities.product.SanPhamChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {
    List<SanPhamChiTiet> findBySanPhamId(Integer sanPhamId);
    boolean existsBySanPhamIdAndMauSacIdAndKichThuocId(Integer spId, Integer msId, Integer ktId);
    Optional<SanPhamChiTiet> findBySanPhamIdAndMauSacIdAndKichThuocId(Integer spId, Integer msId, Integer ktId);

    @Query("SELECT MAX(v.id) FROM SanPhamChiTiet v")
    Long findMaxId();

    boolean existsBySku(String sku);

    @Query("SELECT s FROM SanPhamChiTiet s WHERE s.sku LIKE %:q% OR s.sanPham.ten LIKE %:q%")
    List<SanPhamChiTiet> findByTenOrSku(@Param("q") String q);

    @Query("SELECT s FROM SanPhamChiTiet s WHERE " +
            "(s.soLuong > 0) AND " + // Chỉ lấy hàng còn trong kho
            "(:q IS NULL OR s.sku LIKE %:q% OR s.sanPham.ten LIKE %:q%) AND " +
            "(:color IS NULL OR s.mauSac.id = :color) AND " +
            "(:size IS NULL OR s.kichThuoc.id = :size)")
    List<SanPhamChiTiet> findAvailableProducts(
            @Param("q") String q,
            @Param("color") Integer color,
            @Param("size") Integer size
    );

    // Kiểm tra xem có Sản phẩm chi tiết nào đang HOẠT ĐỘNG dùng Màu sắc này không?
    boolean existsByMauSac_IdAndTrangThaiTrue(Integer mauSacId);

    // Kiểm tra xem có Sản phẩm chi tiết nào đang HOẠT ĐỘNG dùng Kích thước này không?
    boolean existsByKichThuoc_IdAndTrangThaiTrue(Integer kichThuocId);
}

