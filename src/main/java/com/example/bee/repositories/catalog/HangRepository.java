package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.Hang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HangRepository extends JpaRepository<Hang, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Integer id);

    Optional<Hang> findByMa(String ma);
}
