package com.example.bee.repositories.products;

import com.example.bee.entities.product.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    boolean existsByTenIgnoreCaseAndDanhMucIdAndHangIdAndChatLieuId(
            String ten, Integer idDanhMuc, Integer idHang, Integer idChatLieu
    );

    boolean existsByTenIgnoreCaseAndDanhMucIdAndHangIdAndChatLieuIdAndIdNot(
            String ten, Integer idDanhMuc, Integer idHang, Integer idChatLieu, Integer id
    );

    boolean existsByMaIgnoreCase(String ma);

    @Query("SELECT DISTINCT s FROM SanPham s " +
            "LEFT JOIN FETCH s.danhMuc " +
            "LEFT JOIN FETCH s.hang " +
            "LEFT JOIN FETCH s.chatLieu " +
            "LEFT JOIN FETCH s.hinhAnhs " +
            "WHERE (:q IS NULL OR s.ten LIKE %:q% OR s.ma LIKE %:q%) " +
            "AND (:trangThai IS NULL OR s.trangThai = :trangThai) " +
            "AND (:idDanhMuc IS NULL OR s.danhMuc.id = :idDanhMuc) " +
            "AND (:idHang IS NULL OR s.hang.id = :idHang) " +
            "AND (:idChatLieu IS NULL OR s.chatLieu.id = :idChatLieu)")
    Page<SanPham> search(String q, Boolean trangThai, Integer idDanhMuc,
                         Integer idHang, Integer idChatLieu, Pageable pageable);
}
