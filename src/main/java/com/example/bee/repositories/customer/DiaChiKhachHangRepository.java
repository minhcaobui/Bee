package com.example.bee.repositories.customer;

import com.example.bee.entities.customer.DiaChiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiaChiKhachHangRepository extends JpaRepository<DiaChiKhachHang, Integer> {
    List<DiaChiKhachHang> findByKhachHangId(Integer khachHangId);

    Optional<DiaChiKhachHang> findByKhachHangIdAndLaMacDinhTrue(Integer khachHangId);
}