package com.example.bee.repositories.products;

import com.example.bee.entities.product.SanPhamChiTiet;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {

    @EntityGraph(attributePaths = {"sanPham", "mauSac", "kichThuoc"})
    List<SanPhamChiTiet> findBySanPhamId(Integer sanPhamId);

    boolean existsBySanPhamIdAndMauSacIdAndKichThuocId(Integer spId, Integer msId, Integer ktId);

    boolean existsBySku(String sku);

    @EntityGraph(attributePaths = {"sanPham", "mauSac", "kichThuoc"})
    @Query("SELECT s FROM SanPhamChiTiet s WHERE " +
            "(s.soLuong > 0) AND " +
            "(:q IS NULL OR s.sku LIKE %:q% OR s.sanPham.ten LIKE %:q%) AND " +
            "(:color IS NULL OR s.mauSac.id = :color) AND " +
            "(:size IS NULL OR s.kichThuoc.id = :size)")
    List<SanPhamChiTiet> findAvailableProducts(
            @Param("q") String q,
            @Param("color") Integer color,
            @Param("size") Integer size
    );

    boolean existsByMauSac_IdAndTrangThaiTrue(Integer mauSacId);

    boolean existsByKichThuoc_IdAndTrangThaiTrue(Integer kichThuocId);

    @Modifying
    @Transactional
    @Query("UPDATE SanPhamChiTiet s SET s.soLuongTamGiu = 0 WHERE s.soLuongTamGiu > 0")
    void resetAllSoLuongTamGiu();
}