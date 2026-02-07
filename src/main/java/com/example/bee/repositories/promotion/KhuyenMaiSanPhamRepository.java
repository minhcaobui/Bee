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
    // 1. Lấy danh sách SP theo ID khuyến mãi (để tích sẵn checkbox khi sửa)
    List<KhuyenMaiSanPham> findAllByIdKhuyenMai(Integer idKhuyenMai);

    // 2. Xóa hết SP cũ khi cập nhật khuyến mãi
    @Modifying
    @Query("DELETE FROM KhuyenMaiSanPham k WHERE k.idKhuyenMai = :idKhuyenMai")
    void deleteByIdKhuyenMai(@Param("idKhuyenMai") Integer idKhuyenMai);
}