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
            ISNULL(SUM(CASE WHEN tthd.ma IN ('HOAN_THANH', 'DA_DOI', 'DA_TRA') THEN (h.gia_tong - ISNULL(h.phi_van_chuyen, 0) - ISNULL(yc.hoan, 0)) ELSE 0 END), 0)  AS rev,
            COUNT(h.id)                                                                     AS ord,
            ISNULL(AVG(CASE WHEN tthd.ma IN ('HOAN_THANH', 'DA_DOI', 'DA_TRA') THEN (h.gia_tong - ISNULL(h.phi_van_chuyen, 0) - ISNULL(yc.hoan, 0)) END), 0)           AS avg_val,

            (SELECT COUNT(h_pend.id) FROM hoa_don h_pend
             JOIN trang_thai_hoa_don t_pend ON h_pend.id_trang_thai_hoa_don = t_pend.id
             WHERE t_pend.ma IN ('CHO_THANH_TOAN', 'CHO_XAC_NHAN', 'CHO_GIAO', 'DANG_GIAO')) AS pend,

            SUM(CASE WHEN tthd.ma = 'DA_HUY' THEN 1 ELSE 0 END) AS cancelled,
            SUM(CASE WHEN tthd.ma IN ('HOAN_THANH', 'DA_DOI', 'DA_TRA') THEN 1 ELSE 0 END) AS completed,
            
            (SELECT ISNULL(SUM(hdct.so_luong - ISNULL(ctdt.sl_tra, 0)), 0)
             FROM hoa_don_chi_tiet hdct
             JOIN hoa_don h2 ON hdct.id_hoa_don = h2.id
             JOIN trang_thai_hoa_don t2 ON h2.id_trang_thai_hoa_don = t2.id
             LEFT JOIN (
                SELECT id_hoa_don_chi_tiet, SUM(so_luong) as sl_tra
                FROM chi_tiet_doi_tra c 
                JOIN yeu_cau_doi_tra y ON c.id_yeu_cau_doi_tra = y.id
                WHERE y.trang_thai = 'HOAN_THANH'
                GROUP BY id_hoa_don_chi_tiet
             ) ctdt ON ctdt.id_hoa_don_chi_tiet = hdct.id
             WHERE h2.ngay_tao BETWEEN :start AND :end
               AND t2.ma IN ('HOAN_THANH', 'DA_DOI', 'DA_TRA')) AS sold,
               
            (SELECT COUNT(DISTINCT h3.id_khach_hang)
             FROM hoa_don h3
             WHERE h3.ngay_tao BETWEEN :start AND :end
               AND h3.id_khach_hang IS NOT NULL) AS new_cust,
               
            SUM(CASE WHEN tthd.ma = 'HOAN_THANH' THEN 1 ELSE 0 END) AS pure_completed,
            SUM(CASE WHEN tthd.ma = 'DA_DOI' THEN 1 ELSE 0 END) AS exchanged,
            SUM(CASE WHEN tthd.ma = 'DA_TRA' THEN 1 ELSE 0 END) AS returned
               
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        LEFT JOIN (SELECT id_hoa_don, SUM(so_tien_hoan) AS hoan FROM yeu_cau_doi_tra WHERE trang_thai = 'HOAN_THANH' GROUP BY id_hoa_don) yc ON yc.id_hoa_don = h.id
        WHERE h.ngay_tao BETWEEN :start AND :end
        """)
    List<Object[]> getStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT DATEPART(HOUR, h.ngay_tao) AS label,
               ISNULL(SUM(h.gia_tong - ISNULL(h.phi_van_chuyen, 0) - ISNULL(yc.hoan, 0)), 0) AS revenue,
               COUNT(h.id)                AS orders
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        LEFT JOIN (SELECT id_hoa_don, SUM(so_tien_hoan) AS hoan FROM yeu_cau_doi_tra WHERE trang_thai = 'HOAN_THANH' GROUP BY id_hoa_don) yc ON yc.id_hoa_don = h.id
        WHERE h.ngay_tao BETWEEN :start AND :end
          AND tthd.ma IN ('HOAN_THANH', 'DA_DOI', 'DA_TRA')
        GROUP BY DATEPART(HOUR, h.ngay_tao)
        ORDER BY DATEPART(HOUR, h.ngay_tao)
        """)
    List<Object[]> getChartHour(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT DATEPART(WEEKDAY, h.ngay_tao) AS label,
               ISNULL(SUM(h.gia_tong - ISNULL(h.phi_van_chuyen, 0) - ISNULL(yc.hoan, 0)), 0) AS revenue,
               COUNT(h.id)                AS orders
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        LEFT JOIN (SELECT id_hoa_don, SUM(so_tien_hoan) AS hoan FROM yeu_cau_doi_tra WHERE trang_thai = 'HOAN_THANH' GROUP BY id_hoa_don) yc ON yc.id_hoa_don = h.id
        WHERE h.ngay_tao BETWEEN :start AND :end
          AND tthd.ma IN ('HOAN_THANH', 'DA_DOI', 'DA_TRA')
        GROUP BY DATEPART(WEEKDAY, h.ngay_tao)
        ORDER BY DATEPART(WEEKDAY, h.ngay_tao)
        """)
    List<Object[]> getChartWeek(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT DAY(h.ngay_tao)            AS label,
               ISNULL(SUM(h.gia_tong - ISNULL(h.phi_van_chuyen, 0) - ISNULL(yc.hoan, 0)), 0) AS revenue,
               COUNT(h.id)                AS orders
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        LEFT JOIN (SELECT id_hoa_don, SUM(so_tien_hoan) AS hoan FROM yeu_cau_doi_tra WHERE trang_thai = 'HOAN_THANH' GROUP BY id_hoa_don) yc ON yc.id_hoa_don = h.id
        WHERE h.ngay_tao BETWEEN :start AND :end
          AND tthd.ma IN ('HOAN_THANH', 'DA_DOI', 'DA_TRA')
        GROUP BY DAY(h.ngay_tao)
        ORDER BY DAY(h.ngay_tao)
        """)
    List<Object[]> getChartDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT MONTH(h.ngay_tao)          AS label,
               ISNULL(SUM(h.gia_tong - ISNULL(h.phi_van_chuyen, 0) - ISNULL(yc.hoan, 0)), 0) AS revenue,
               COUNT(h.id)                AS orders
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        LEFT JOIN (SELECT id_hoa_don, SUM(so_tien_hoan) AS hoan FROM yeu_cau_doi_tra WHERE trang_thai = 'HOAN_THANH' GROUP BY id_hoa_don) yc ON yc.id_hoa_don = h.id
        WHERE h.ngay_tao BETWEEN :start AND :end
          AND tthd.ma IN ('HOAN_THANH', 'DA_DOI', 'DA_TRA')
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
            SUM(hdct.so_luong - ISNULL(ctdt.sl_tra, 0)) AS sold,
            SUM((hdct.so_luong - ISNULL(ctdt.sl_tra, 0)) * hdct.gia_tien) AS rev
        FROM hoa_don_chi_tiet hdct
        JOIN hoa_don h         ON hdct.id_hoa_don          = h.id
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        JOIN san_pham_chi_tiet spct ON hdct.id_san_pham_chi_tiet = spct.id
        JOIN san_pham sp        ON spct.id_san_pham          = sp.id
        JOIN mau_sac ms         ON spct.id_mau_sac           = ms.id
        JOIN kich_thuoc kt      ON spct.id_kich_thuoc        = kt.id
        LEFT JOIN (
            SELECT id_hoa_don_chi_tiet, SUM(so_luong) as sl_tra
            FROM chi_tiet_doi_tra c 
            JOIN yeu_cau_doi_tra y ON c.id_yeu_cau_doi_tra = y.id
            WHERE y.trang_thai = 'HOAN_THANH'
            GROUP BY id_hoa_don_chi_tiet
        ) ctdt ON ctdt.id_hoa_don_chi_tiet = hdct.id
        WHERE h.ngay_tao BETWEEN :start AND :end
          AND tthd.ma IN ('HOAN_THANH', 'DA_DOI', 'DA_TRA')
        GROUP BY spct.id, sp.ten, spct.sku, ms.ten, kt.ten
        ORDER BY sold DESC
        """)
    List<Object[]> getTopProducts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(nativeQuery = true, value = """
        SELECT TOP 12
            h.id, h.ma,
            ISNULL(kh.ho_ten, N'Khách vãng lai')     AS ten_kh,
            (SELECT COUNT(*) FROM hoa_don_chi_tiet WHERE id_hoa_don = h.id) AS so_sp,
            (h.gia_tong - ISNULL(h.phi_van_chuyen, 0)) AS gia_tong,
            ISNULL(h.phuong_thuc_thanh_toan, N'Tiền mặt') AS phuong_thuc,
            tthd.ma AS trang_thai
        FROM hoa_don h
        JOIN trang_thai_hoa_don tthd ON h.id_trang_thai_hoa_don = tthd.id
        LEFT JOIN khach_hang kh ON h.id_khach_hang = kh.id
        ORDER BY h.ngay_tao DESC
        """)
    List<Object[]> getRecentOrders();

    // 🌟 ĐÃ FIX: Tính phương thức thanh toán của TOÀN BỘ đơn hàng (không giới hạn trạng thái)
    @Query(nativeQuery = true, value = """
        SELECT
            ISNULL(h.phuong_thuc_thanh_toan, N'Tiền mặt') AS method,
            COUNT(h.id) AS cnt
        FROM hoa_don h
        WHERE h.ngay_tao BETWEEN :start AND :end
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
    SELECT ISNULL(AVG(h.gia_tong - ISNULL(h.phi_van_chuyen, 0) - ISNULL(yc.hoan, 0)), 0)
    FROM hoa_don h
    JOIN trang_thai_hoa_don t ON h.id_trang_thai_hoa_don = t.id
    LEFT JOIN (SELECT id_hoa_don, SUM(so_tien_hoan) AS hoan FROM yeu_cau_doi_tra WHERE trang_thai = 'HOAN_THANH' GROUP BY id_hoa_don) yc ON yc.id_hoa_don = h.id
    WHERE h.ngay_tao BETWEEN :start AND :end
      AND t.ma IN ('HOAN_THANH', 'DA_DOI', 'DA_TRA')
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
        ISNULL(SUM(h.gia_tong - ISNULL(h.phi_van_chuyen, 0) - ISNULL(yc.hoan, 0)), 0) AS revenue
    FROM hoa_don h
    JOIN trang_thai_hoa_don t ON h.id_trang_thai_hoa_don = t.id
    LEFT JOIN (SELECT id_hoa_don, SUM(so_tien_hoan) AS hoan FROM yeu_cau_doi_tra WHERE trang_thai = 'HOAN_THANH' GROUP BY id_hoa_don) yc ON yc.id_hoa_don = h.id
    WHERE h.ngay_tao BETWEEN :start AND :end
      AND t.ma IN ('HOAN_THANH', 'DA_DOI', 'DA_TRA')
    GROUP BY DATEPART(WEEKDAY, h.ngay_tao), DATEPART(HOUR, h.ngay_tao)
    """)
    List<Object[]> getHeatmap(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}