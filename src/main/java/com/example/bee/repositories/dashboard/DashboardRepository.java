package com.example.bee.repositories.dashboard;

import com.example.bee.entities.order.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<HoaDon, Integer> {

    @Query(nativeQuery = true, value = """
        SELECT
            ISNULL(SUM(CASE WHEN tthd.ma = 'HOAN_THANH' THEN h.gia_tong ELSE 0 END), 0)  AS rev,
            COUNT(h.id)                                                                     AS ord,
            ISNULL(AVG(CASE WHEN tthd.ma = 'HOAN_THANH' THEN h.gia_tong END), 0)           AS avg_val,
            SUM(CASE WHEN tthd.ma = 'CHO_THANH_TOAN' THEN 1 ELSE 0 END)                    AS pend,
            SUM(CASE WHEN tthd.ma = 'DA_HUY'         THEN 1 ELSE 0 END)                    AS cancelled,
            SUM(CASE WHEN tthd.ma = 'HOAN_THANH'     THEN 1 ELSE 0 END)                    AS completed,
            (SELECT ISNULL(SUM(hdct.so_luong), 0)
             FROM hoa_don_chi_tiet hdct
             JOIN hoa_don h2 ON hdct.id_hoa_don = h2.id
             JOIN trang_thai_hoa_don t2 ON h2.id_trang_thai_hoa_don = t2.id
             WHERE h2.ngay_tao BETWEEN :start AND :end
               AND t2.ma = 'HOAN_THANH')                                                   AS sold,
            (SELECT COUNT(DISTINCT h3.id_khach_hang)
             FROM hoa_don h3
             WHERE h3.ngay_tao BETWEEN :start AND :end
               AND h3.id_khach_hang IS NOT NULL)                                            AS new_cust
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        WHERE h.ngay_tao BETWEEN :start AND :end
        """)
    List<Object[]> getStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT DATEPART(HOUR, h.ngay_tao) AS label,
               ISNULL(SUM(h.gia_tong), 0) AS revenue,
               COUNT(h.id)                AS orders
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        WHERE h.ngay_tao BETWEEN :start AND :end
          AND tthd.ma != 'DA_HUY'
        GROUP BY DATEPART(HOUR, h.ngay_tao)
        ORDER BY DATEPART(HOUR, h.ngay_tao)
        """)
    List<Object[]> getChartHour(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT DATEPART(WEEKDAY, h.ngay_tao) AS label,
               ISNULL(SUM(h.gia_tong), 0) AS revenue,
               COUNT(h.id)                AS orders
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        WHERE h.ngay_tao BETWEEN :start AND :end
          AND tthd.ma != 'DA_HUY'
        GROUP BY DATEPART(WEEKDAY, h.ngay_tao)
        ORDER BY DATEPART(WEEKDAY, h.ngay_tao)
        """)
    List<Object[]> getChartWeek(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT DAY(h.ngay_tao)            AS label,
               ISNULL(SUM(h.gia_tong), 0) AS revenue,
               COUNT(h.id)                AS orders
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        WHERE h.ngay_tao BETWEEN :start AND :end
          AND tthd.ma != 'DA_HUY'
        GROUP BY DAY(h.ngay_tao)
        ORDER BY DAY(h.ngay_tao)
        """)
    List<Object[]> getChartDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT MONTH(h.ngay_tao)          AS label,
               ISNULL(SUM(h.gia_tong), 0) AS revenue,
               COUNT(h.id)                AS orders
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        WHERE h.ngay_tao BETWEEN :start AND :end
          AND tthd.ma != 'DA_HUY'
        GROUP BY MONTH(h.ngay_tao)
        ORDER BY MONTH(h.ngay_tao)
        """)
    List<Object[]> getChartMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT TOP 12
            spct.id                         AS id,
            sp.ten                          AS name,
            spct.sku                        AS sku,
            CONCAT(ms.ten, ' · ', kt.ten)   AS attr,
            SUM(hdct.so_luong)              AS sold,
            SUM(hdct.so_luong * hdct.gia_tien) AS rev
        FROM hoa_don_chi_tiet hdct
        JOIN hoa_don h         ON hdct.id_hoa_don          = h.id
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        JOIN san_pham_chi_tiet spct ON hdct.id_san_pham_chi_tiet = spct.id
        JOIN san_pham sp        ON spct.id_san_pham          = sp.id
        JOIN mau_sac ms         ON spct.id_mau_sac           = ms.id
        JOIN kich_thuoc kt      ON spct.id_kich_thuoc        = kt.id
        WHERE h.ngay_tao BETWEEN :start AND :end
          AND tthd.ma = 'HOAN_THANH'
        GROUP BY spct.id, sp.ten, spct.sku, ms.ten, kt.ten
        ORDER BY sold DESC
        """)
    List<Object[]> getTopProducts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT TOP 12
            h.id, h.ma,
            ISNULL(kh.ho_ten, N'Khách vãng lai')     AS ten_kh,
            (SELECT COUNT(*) FROM hoa_don_chi_tiet WHERE id_hoa_don = h.id) AS so_sp,
            h.gia_tong,
            ISNULL(h.phuong_thuc_thanh_toan, N'Tiền mặt') AS phuong_thuc,
            tthd.ma AS trang_thai
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        LEFT JOIN khach_hang kh ON h.id_khach_hang = kh.id
        ORDER BY h.ngay_tao DESC
        """)
    List<Object[]> getRecentOrders();

    @Query(nativeQuery = true, value = """
        SELECT
            ISNULL(h.phuong_thuc_thanh_toan, N'Tiền mặt') AS method,
            COUNT(h.id) AS cnt
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        WHERE h.ngay_tao BETWEEN :start AND :end
          AND tthd.ma = 'HOAN_THANH'
        GROUP BY ISNULL(h.phuong_thuc_thanh_toan, N'Tiền mặt')
        """)
    List<Object[]> getPaymentMethods(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT TOP 10
            spct.id, sp.ten, spct.sku,
            CONCAT(ms.ten, ' · ', kt.ten) AS attr,
            spct.so_luong
        FROM san_pham_chi_tiet spct
        JOIN san_pham sp    ON spct.id_san_pham   = sp.id
        JOIN mau_sac ms     ON spct.id_mau_sac    = ms.id
        JOIN kich_thuoc kt  ON spct.id_kich_thuoc = kt.id
        WHERE spct.so_luong <= 10 AND spct.trang_thai = 1
        ORDER BY spct.so_luong ASC
        """)
    List<Object[]> getLowStock();

    @Query(nativeQuery = true, value = """
    SELECT ISNULL(AVG(h.gia_tong), 0)
    FROM hoa_don h
    JOIN trang_thai_hoa_don t ON h.id_trang_thai_hoa_don = t.id
    WHERE h.ngay_tao BETWEEN :start AND :end
      AND t.ma = 'HOAN_THANH'
    """)
    Double getAvgOrderValue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
    SELECT COUNT(*) FROM (
        SELECT h.id_khach_hang
        FROM hoa_don h
        JOIN khach_hang kh ON h.id_khach_hang = kh.id
        WHERE kh.id_tai_khoan IS NOT NULL
        GROUP BY h.id_khach_hang
        HAVING MIN(h.ngay_tao) BETWEEN :start AND :end
    ) AS new_custs
    """)
    Long getNewCustomers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
    SELECT
        DATEPART(WEEKDAY, h.ngay_tao) AS day_of_week,
        DATEPART(HOUR, h.ngay_tao) AS hour_of_day,
        ISNULL(SUM(h.gia_tong), 0)    AS revenue
    FROM hoa_don h
    JOIN trang_thai_hoa_don t ON h.id_trang_thai_hoa_don = t.id
    WHERE h.ngay_tao BETWEEN :start AND :end
      AND t.ma = 'HOAN_THANH'
    GROUP BY DATEPART(WEEKDAY, h.ngay_tao), DATEPART(HOUR, h.ngay_tao)
    """)
    List<Object[]> getHeatmap(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}