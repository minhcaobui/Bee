package com.example.bee.repositories.promotion;

import com.example.bee.entities.promotion.KhuyenMaiSanPham;
import io.micrometer.common.KeyValues;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KhuyenMaiSanPhamChiTietRepository extends JpaRepository<KhuyenMaiSanPham, Integer> {
    KeyValues findAllByIdKhuyenMai(Integer id);

    void deleteByIdKhuyenMai(Integer id);
}
