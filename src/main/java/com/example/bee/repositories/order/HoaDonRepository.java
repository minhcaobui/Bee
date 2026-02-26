package com.example.bee.repositories.order;

import com.example.bee.entities.order.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {
    List<HoaDon> findByLoaiHoaDonAndTrangThaiHoaDonMa(Integer loai, String maTrangThai);
}