package com.example.bee.repositories.staff;

import com.example.bee.entities.staff.TK;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TKRepository extends JpaRepository<TK,Integer> {
    boolean existsByTenDangNhap(String username);
    Optional<TK> findByTenDangNhap(String tenDangNhap);
}
