package com.example.bee.repositories.reviews;

import com.example.bee.entities.reviews.DanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Long> {
    List<DanhGia> findBySanPhamIdOrderByNgayTaoDesc(Integer sanPhamId);
}
