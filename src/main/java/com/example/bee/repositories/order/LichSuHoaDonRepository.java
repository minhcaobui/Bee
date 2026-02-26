package com.example.bee.repositories.order;

import com.example.bee.entities.order.LichSuHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LichSuHoaDonRepository extends JpaRepository<LichSuHoaDon, Integer> {
    // Để sau này mày làm trang Chi tiết hóa đơn, hiện dòng thời gian (Timeline)
    List<LichSuHoaDon> findByHoaDonIdOrderByNgayTaoDesc(Integer hoaDonId);
}