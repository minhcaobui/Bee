package com.example.bee.repositories.products;

import com.example.bee.entities.product.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    @EntityGraph(attributePaths = {"danhMuc", "hang", "chatLieu"})
    @Query("SELECT s FROM SanPham s " +
            "WHERE (:q IS NULL OR s.ten LIKE %:q% OR s.ma LIKE %:q%) " +
            "AND (:trangThai IS NULL OR s.trangThai = :trangThai) " +
            "AND (:idDanhMuc IS NULL OR s.danhMuc.id = :idDanhMuc) " +
            "AND (:idHang IS NULL OR s.hang.id = :idHang) " +
            "AND (:idChatLieu IS NULL OR s.chatLieu.id = :idChatLieu)")
    Page<SanPham> search(@Param("q") String q,
                         @Param("trangThai") Boolean trangThai,
                         @Param("idDanhMuc") Integer idDanhMuc,
                         @Param("idHang") Integer idHang,
                         @Param("idChatLieu") Integer idChatLieu,
                         Pageable pageable);

    @EntityGraph(attributePaths = {"danhMuc", "hang", "chatLieu"})
    @Query("SELECT s FROM SanPham s WHERE s.trangThai = true")
    List<SanPham> getAllActiveProducts();

    boolean existsByDanhMuc_IdAndTrangThaiTrue(Integer danhMucId);

    boolean existsByHang_IdAndTrangThaiTrue(Integer hangId);

    boolean existsByChatLieu_IdAndTrangThaiTrue(Integer chatLieuId);

    boolean existsByTenIgnoreCaseAndDanhMuc_IdAndHang_IdAndChatLieu_Id(String ten, Integer idDanhMuc, Integer idHang, Integer idChatLieu);

    boolean existsByTenIgnoreCaseAndDanhMuc_IdAndHang_IdAndChatLieu_IdAndIdNot(String ten, Integer idDanhMuc, Integer idHang, Integer idChatLieu, Integer id);
}