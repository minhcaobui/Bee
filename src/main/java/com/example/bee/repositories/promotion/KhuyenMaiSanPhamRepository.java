package com.example.bee.repositories.promotion;

import com.example.bee.entities.promotion.KhuyenMaiSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhuyenMaiSanPhamRepository extends JpaRepository<KhuyenMaiSanPham, Integer> {

    List<KhuyenMaiSanPham> findAllByIdKhuyenMai(Integer idKhuyenMai);

    @Modifying
    @Query("DELETE FROM KhuyenMaiSanPham k WHERE k.idKhuyenMai = :idKhuyenMai")
    void deleteByIdKhuyenMai(@Param("idKhuyenMai") Integer idKhuyenMai);
}