package com.example.bee.controllers.api.dashboard;

import com.example.bee.repositories.dashboard.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/thong-ke")
@RequiredArgsConstructor
public class ThongKeApi {

    private final DashboardRepository repo;

    private LocalDateTime[] getRange(String period, String from, String to) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case "today" -> new LocalDateTime[]{today.atStartOfDay(), today.atTime(23, 59, 59)};
            case "week" -> new LocalDateTime[]{today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(), today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(23, 59, 59)};
            case "month" -> new LocalDateTime[]{today.withDayOfMonth(1).atStartOfDay(), today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59)};
            case "year" -> new LocalDateTime[]{today.withDayOfYear(1).atStartOfDay(), today.withDayOfYear(today.lengthOfYear()).atTime(23, 59, 59)};
            default -> new LocalDateTime[]{LocalDate.parse(from).atStartOfDay(), LocalDate.parse(to).atTime(23, 59, 59)};
        };
    }

    private LocalDateTime[] getPrevRange(String period, LocalDateTime[] cur) {
        return switch (period) {
            case "today" -> new LocalDateTime[]{cur[0].minusDays(1), cur[1].minusDays(1)};
            case "week" -> new LocalDateTime[]{cur[0].minusWeeks(1), cur[1].minusWeeks(1)};
            case "month" -> new LocalDateTime[]{cur[0].minusMonths(1), cur[1].minusMonths(1)};
            case "year" -> new LocalDateTime[]{cur[0].minusYears(1), cur[1].minusYears(1)};
            default -> new LocalDateTime[]{cur[0].minusMonths(1), cur[1].minusMonths(1)};
        };
    }

    private double calcChg(double cur, double prev) {
        if (prev == 0) return cur > 0 ? 100.0 : 0.0;
        return Math.round((cur - prev) / prev * 1000.0) / 10.0;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> stats(@RequestParam(defaultValue = "week") String type, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LocalDateTime[] cur = getRange(type, startDate, endDate);
        LocalDateTime[] prev = getPrevRange(type, cur);

        List<Object[]> cList = repo.getStats(cur[0], cur[1]);
        List<Object[]> pList = repo.getStats(prev[0], prev[1]);

        Object[] c = cList.isEmpty() ? new Object[8] : cList.get(0);
        Object[] p = pList.isEmpty() ? new Object[8] : pList.get(0);

        double rev = c[0] != null ? ((Number) c[0]).doubleValue() : 0;
        double ord = c[1] != null ? ((Number) c[1]).doubleValue() : 0;
        double pend = c[3] != null ? ((Number) c[3]).doubleValue() : 0;
        double canc = c[4] != null ? ((Number) c[4]).doubleValue() : 0;
        double comp = c[5] != null ? ((Number) c[5]).doubleValue() : 0;
        double sold = c[6] != null ? ((Number) c[6]).doubleValue() : 0;

        double pRev = p[0] != null ? ((Number) p[0]).doubleValue() : 0;
        double pComp = p[5] != null ? ((Number) p[5]).doubleValue() : 0;
        double pCanc = p[4] != null ? ((Number) p[4]).doubleValue() : 0;
        double pSold = p[6] != null ? ((Number) p[6]).doubleValue() : 0;

        Map<String, Object> res = new HashMap<>();
        res.put("doanhThu", rev);
        res.put("doanhThuRatio", calcChg(rev, pRev));
        res.put("sanPhamDaBan", sold);
        res.put("sanPhamRatio", calcChg(sold, pSold));
        res.put("donThanhCong", comp);
        res.put("donThanhCongRatio", calcChg(comp, pComp));
        res.put("donDaHuy", canc);
        res.put("donHuyRatio", calcChg(canc, pCanc));

        return ResponseEntity.ok(res);
    }

    @GetMapping("/chart-doanh-thu")
    public ResponseEntity<?> chartDoanhThu(@RequestParam(defaultValue = "week") String type, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LocalDateTime[] cur = getRange(type, startDate, endDate);
        List<Object[]> curRows = switch (type) {
            case "today" -> repo.getChartHour(cur[0], cur[1]);
            case "week" -> repo.getChartWeek(cur[0], cur[1]);
            case "year" -> repo.getChartMonth(cur[0], cur[1]);
            default -> repo.getChartDay(cur[0], cur[1]);
        };

        Map<Integer, long[]> curMap = new LinkedHashMap<>();
        curRows.forEach(r -> curMap.put(((Number) r[0]).intValue(), new long[]{((Number) r[1]).longValue(), ((Number) r[2]).longValue()}));

        List<String> labels = switch (type) {
            case "today" -> IntStream.rangeClosed(7, 21).mapToObj(h -> h + "h").toList();
            case "week" -> List.of("T2", "T3", "T4", "T5", "T6", "T7", "CN");
            case "year" -> List.of("T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12");
            default -> IntStream.rangeClosed(1, cur[0].toLocalDate().lengthOfMonth()).mapToObj(String::valueOf).toList();
        };

        List<Long> revList = new ArrayList<>();
        List<Long> ordList = new ArrayList<>();

        for (int i = 0; i < labels.size(); i++) {
            int key = switch (type) {
                case "today" -> i + 7;
                case "week" -> (i == 6) ? 1 : i + 2;
                case "year" -> i + 1;
                default -> i + 1;
            };
            long[] cv = curMap.getOrDefault(key, new long[]{0L, 0L});
            revList.add(cv[0]);
            ordList.add(cv[1]);
        }

        Map<String, Object> chartRes = new HashMap<>();
        chartRes.put("labels", labels);
        chartRes.put("doanhThu", revList);
        chartRes.put("donHang", ordList);
        return ResponseEntity.ok(chartRes);
    }

    @GetMapping("/chart-trang-thai")
    public ResponseEntity<?> chartTrangThai(@RequestParam(defaultValue = "week") String type, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LocalDateTime[] cur = getRange(type, startDate, endDate);
        List<Object[]> rows = repo.getOrderStatusesRatio(cur[0], cur[1]);

        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("tenTrangThai", r[0].toString());
            m.put("maTrangThai", r[1].toString());
            m.put("soLuong", ((Number) r[2]).longValue());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/top-san-pham")
    public ResponseEntity<?> topProducts(@RequestParam(defaultValue = "week") String type, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        LocalDateTime[] cur = getRange(type, startDate, endDate);
        List<Object[]> rows = repo.getTopProducts(cur[0], cur[1]);

        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ((Number) r[0]).intValue());
            m.put("tenSanPham", r[1].toString() + " (" + r[3].toString() + ")");
            m.put("soLuongDaBan", ((Number) r[4]).longValue());
            m.put("doanhThu", ((Number) r[5]).longValue());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/sap-het-hang")
    public ResponseEntity<?> lowStock() {
        List<Object[]> rows = repo.getLowStock();
        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ((Number) r[0]).intValue());
            m.put("tenSanPham", r[1].toString());
            m.put("thuocTinh", r[3].toString());
            m.put("soLuong", ((Number) r[4]).intValue());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}