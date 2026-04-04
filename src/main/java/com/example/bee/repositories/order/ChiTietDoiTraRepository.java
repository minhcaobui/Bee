package com.example.bee.repositories.order;

import com.example.bee.entities.order.ChiTietDoiTra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDoiTraRepository extends JpaRepository<ChiTietDoiTra, Integer> {
    List<ChiTietDoiTra> findByYeuCauDoiTraId(Integer yeuCauId);
}