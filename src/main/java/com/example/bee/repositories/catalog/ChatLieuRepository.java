package com.example.bee.repositories.catalog;

import com.example.bee.entities.catalog.ChatLieu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatLieuRepository extends JpaRepository<ChatLieu, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Integer id);

    Optional<ChatLieu> findByMa(String ma);
}

