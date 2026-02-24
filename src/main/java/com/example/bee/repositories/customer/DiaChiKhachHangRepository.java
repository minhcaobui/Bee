package com.example.bee.repositories.customer;

import com.example.bee.entities.customer.DiaChiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaChiKhachHangRepository extends JpaRepository<DiaChiKhachHang, Integer> {
    // Lấy list địa chỉ để hiển thị ở Tab 2
    List<DiaChiKhachHang> findByKhachHangId(Integer khachHangId);

    // Tìm xem địa chỉ nào đang là mặc định của khách này (để bỏ chọn khi set cái mới)
    Optional<DiaChiKhachHang> findByKhachHangIdAndLaMacDinhTrue(Integer khachHangId);
}