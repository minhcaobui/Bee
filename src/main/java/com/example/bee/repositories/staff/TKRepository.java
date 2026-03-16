package com.example.bee.repositories.staff;

import com.example.bee.entities.staff.TK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TKRepository extends JpaRepository<TK, Integer> {
    Optional<Object> findByTenDangNhap(String username);
}
