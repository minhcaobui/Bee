package com.example.bee.repositories.account;

import com.example.bee.entities.account.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VaiTroRepository extends JpaRepository<VaiTro, Integer> {

    Optional<VaiTro> findByMa(String ma);
}