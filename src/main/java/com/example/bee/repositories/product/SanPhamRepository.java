package com.example.bee.repositories.product;

import com.example.bee.entities.product.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

}