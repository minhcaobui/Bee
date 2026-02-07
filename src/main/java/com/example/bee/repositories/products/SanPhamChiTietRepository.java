package com.example.bee.repositories.products;

import com.example.bee.entities.product.SanPhamChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {
    List<SanPhamChiTiet> findBySanPhamId(Integer sanPhamId);
    boolean existsBySanPhamIdAndMauSacIdAndKichThuocId(Integer spId, Integer msId, Integer ktId);
    Optional<SanPhamChiTiet> findBySanPhamIdAndMauSacIdAndKichThuocId(Integer spId, Integer msId, Integer ktId);
}

