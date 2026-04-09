package com.example.bee.repositories.products;

import com.example.bee.entities.product.SanPhamYeuThich;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SanPhamYeuThichRepository extends JpaRepository<SanPhamYeuThich, Long> {
    Optional<SanPhamYeuThich> findFirstByTaiKhoanIdAndSanPhamId(Integer taiKhoanId, Integer sanPhamId);}