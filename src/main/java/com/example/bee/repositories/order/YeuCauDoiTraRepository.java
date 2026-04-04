package com.example.bee.repositories.order;

import com.example.bee.entities.order.YeuCauDoiTra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface YeuCauDoiTraRepository extends JpaRepository<YeuCauDoiTra, Integer> {
    List<YeuCauDoiTra> findAllByOrderByNgayTaoDesc();
}