package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.MauSac;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MauSacRepository extends JpaRepository<MauSac, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Integer id);

    Optional<MauSac> findByMa(String ma);
}