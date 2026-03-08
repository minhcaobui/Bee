package com.example.bee.repositories.staff;

import com.example.bee.entities.staff.NV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NVRepository extends JpaRepository<NV,Integer> {
    @Query("""
    SELECT n FROM NV n 
    LEFT JOIN FETCH n.chucVu 
    LEFT JOIN FETCH n.taiKhoan 
    WHERE (
        :kw IS NULL OR :kw = '' 
        OR n.ma LIKE %:kw% 
        OR n.hoTen LIKE %:kw% 
        OR n.soDienThoai LIKE %:kw%
        OR n.email LIKE %:kw%
    )
    AND (:status IS NULL OR n.trangThai = :status)
    AND n.chucVu.id != 1
    ORDER BY n.id DESC
""")
    List<NV> searchNhanVien(@Param("kw") String kw, @Param("status") Boolean status);

    @Query("SELECT n FROM NV n LEFT JOIN FETCH n.chucVu LEFT JOIN FETCH n.taiKhoan WHERE n.chucVu.id != 1 ORDER BY n.id DESC")
    List<NV> getAllNhanVienCustom();

    @Query("SELECT n FROM NV n LEFT JOIN FETCH n.taiKhoan LEFT JOIN FETCH n.chucVu WHERE n.id = :id")
    Optional<NV> findByIdWithTaiKhoan(@Param("id") Integer id);
    boolean existsBySoDienThoai(String sdt);
    boolean existsByEmail(String email);
    boolean existsBySoDienThoaiAndIdNot(String sdt, Integer id);
    boolean existsByEmailAndIdNot(String email, Integer id);
}
