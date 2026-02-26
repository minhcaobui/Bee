package com.example.bee.repositories.order;

import com.example.bee.entities.order.TrangThaiHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrangThaiHoaDonRepository extends JpaRepository<TrangThaiHoaDon, Integer> {
    TrangThaiHoaDon findByMa(String ma);

}
