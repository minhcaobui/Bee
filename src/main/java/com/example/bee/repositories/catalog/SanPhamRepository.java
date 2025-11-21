package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.MauSac;
import com.example.bee.entities.catalog.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Integer id);

    Optional<SanPham> findByMa(String ma);

    boolean existsByMa(String ma);
}