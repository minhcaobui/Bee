package com.example.bee.repositories.staff;

import com.example.bee.entities.staff.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {

    Optional<NhanVien> findByTaiKhoan_TenDangNhap(String tenDangNhap);

    boolean existsBySoDienThoaiAndTrangThaiTrue(String soDienThoai);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Integer id);

    boolean existsBySoDienThoai(String phone);

    boolean existsBySoDienThoaiAndIdNot(String phone, Integer id);

    @Query("SELECT nv FROM NhanVien nv LEFT JOIN FETCH nv.taiKhoan WHERE nv.id = :id")
    Optional<NhanVien> findByIdWithTaiKhoan(@Param("id") Integer id);

    @Query("SELECT nv FROM NhanVien nv WHERE " +
            "(:keyword IS NULL OR nv.ma LIKE %:keyword% OR nv.hoTen LIKE %:keyword% OR nv.soDienThoai LIKE %:keyword% OR nv.email LIKE %:keyword%) " +
            "AND (:trangThai IS NULL OR nv.trangThai = :trangThai)")
    List<NhanVien> searchNhanVien(@Param("keyword") String keyword, @Param("trangThai") Boolean trangThai);

    @Query("SELECT n FROM NhanVien n")
    List<NhanVien> getAllNhanVienCustom();
}
