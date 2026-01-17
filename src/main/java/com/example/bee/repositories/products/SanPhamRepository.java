package com.example.bee.repositories.products;

import com.example.bee.entities.product.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Integer id);

    @Query("""
    select sp from SanPham sp
    where (:q is null or
           lower(sp.ma) like lower(concat('%', :q, '%')) or
           lower(sp.ten) like lower(concat('%', :q, '%')))
    and (:trangThai is null or sp.trangThai = :trangThai)
    and (:idDanhMuc is null or sp.danhMuc.id = :idDanhMuc)
    and (:idHang is null or sp.hang.id = :idHang)
    and (:idChatLieu is null or sp.chatLieu.id = :idChatLieu)
""")
    Page<SanPham> search(
            @Param("q") String q,
            @Param("trangThai") Boolean trangThai,
            @Param("idDanhMuc") Integer idDanhMuc,
            @Param("idHang") Integer idHang,
            @Param("idChatLieu") Integer idChatLieu,
            Pageable pageable
    );

}
