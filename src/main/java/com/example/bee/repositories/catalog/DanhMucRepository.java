package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.DanhMuc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DanhMucRepository extends JpaRepository<DanhMuc, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Integer id);

    Optional<DanhMuc> findByMa(String ma);
}
