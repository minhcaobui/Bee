package com.example.bee.repositories.notification;

import com.example.bee.entities.notification.ThongBao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {

    List<ThongBao> findByTaiKhoanIdOrderByNgayTaoDesc(Integer taiKhoanId);
}