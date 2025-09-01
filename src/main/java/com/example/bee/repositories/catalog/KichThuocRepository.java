package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.KichThuoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KichThuocRepository extends JpaRepository<KichThuoc, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Integer id);

    Optional<KichThuoc> findByMa(String ma);
}
