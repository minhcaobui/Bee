package com.example.bee.controllers.api.dashboard;

import com.example.bee.repositories.dashboard.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashBoardApi {

    private final DashboardRepository repo;

    // ═══════════════════════════════
    //  Helper: tính khoảng thời gian
    // ═══════════════════════════════
    private LocalDateTime[] getRange(String period, String from, String to) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case "today" -> new LocalDateTime[]{
                    today.atStartOfDay(),
                    today.atTime(23, 59, 59)
            };
            case "week" -> new LocalDateTime[]{
                    today.with(DayOfWeek.MONDAY).atStartOfDay(),
                    today.with(DayOfWeek.SUNDAY).atTime(23, 59, 59)
            };
            case "month" -> new LocalDateTime[]{
                    today.withDayOfMonth(1).atStartOfDay(),
                    today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59)
            };
            case "year" -> new LocalDateTime[]{
                    today.withDayOfYear(1).atStartOfDay(),
                    today.withDayOfYear(today.lengthOfYear()).atTime(23, 59, 59)
            };
            default -> new LocalDateTime[]{  // custom range
                    LocalDate.parse(from).atStartOfDay(),
                    LocalDate.parse(to).atTime(23, 59, 59)
            };
        };
    }

    // Tính khoảng kỳ trước (để so sánh % thay đổi)
    private LocalDateTime[] getPrevRange(String period, LocalDateTime[] cur) {
        return switch (period) {
            case "today"  -> new LocalDateTime[]{cur[0].minusDays(1),  cur[1].minusDays(1)};
            case "week"   -> new LocalDateTime[]{cur[0].minusWeeks(1), cur[1].minusWeeks(1)};
            case "month"  -> new LocalDateTime[]{cur[0].minusMonths(1),cur[1].minusMonths(1)};
            case "year"   -> new LocalDateTime[]{cur[0].minusYears(1), cur[1].minusYears(1)};
            default       -> new LocalDateTime[]{cur[0].minusMonths(1),cur[1].minusMonths(1)};
        };
    }

    private double calcChg(double cur, double prev) {
        if (prev == 0) return cur > 0 ? 100.0 : 0.0;
        return Math.round((cur - prev) / prev * 1000.0) / 10.0; // 1 chữ số thập phân
    }

    // ═══════════════════════════════
    //  1. STATS
    // ═══════════════════════════════
    @GetMapping("/stats")
    public ResponseEntity<?> stats(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime[] cur  = getRange(period, from, to);
        LocalDateTime[] prev = getPrevRange(period, cur);

        // ✅ FIX: getStats trả List<Object[]>, phải lấy .get(0)
        List<Object[]> cList = repo.getStats(cur[0],  cur[1]);
        List<Object[]> pList = repo.getStats(prev[0], prev[1]);

        // Nếu DB chưa có data → trả về toàn 0
        if (cList.isEmpty()) return ResponseEntity.ok(emptyStats());

        Object[] c = cList.get(0);
        Object[] p = pList.isEmpty() ? new Object[8] : pList.get(0);

        // Object[]: rev, ord, avg_val, pend, cancelled, completed, sold, new_cust
        double rev  = c[0] != null ? ((Number) c[0]).doubleValue() : 0;
        double ord  = c[1] != null ? ((Number) c[1]).doubleValue() : 0;
        double avg  = c[2] != null ? ((Number) c[2]).doubleValue() : 0;
        double pend = c[3] != null ? ((Number) c[3]).doubleValue() : 0;
        double canc = c[4] != null ? ((Number) c[4]).doubleValue() : 0;
        double comp = c[5] != null ? ((Number) c[5]).doubleValue() : 0;
        double sold = c[6] != null ? ((Number) c[6]).doubleValue() : 0;
        double cust = c[7] != null ? ((Number) c[7]).doubleValue() : 0;

        double pRev  = p[0] != null ? ((Number) p[0]).doubleValue() : 0;
        double pOrd  = p[1] != null ? ((Number) p[1]).doubleValue() : 0;
        double pAvg  = p[2] != null ? ((Number) p[2]).doubleValue() : 0;
        double pCust = p[7] != null ? ((Number) p[7]).doubleValue() : 0;

        double rate = (comp + canc) > 0 ? Math.round(comp / (comp + canc) * 100) : 0;

        double revChg  = calcChg(rev,  pRev);
        double ordChg  = calcChg(ord,  pOrd);
        double avgChg  = calcChg(avg,  pAvg);
        double custChg = calcChg(cust, pCust);

        Map<String, Object> res = new HashMap<>();
        res.put("rev",       rev);
        res.put("revChg",    Math.abs(revChg));
        res.put("revUp",     revChg >= 0);
        res.put("ord",       (long) ord);
        res.put("ordChg",    Math.abs(ordChg));
        res.put("ordUp",     ordChg >= 0);
        res.put("pend",      (long) pend);
        res.put("avg",       avg);
        res.put("avgChg",    Math.abs(avgChg));
        res.put("avgUp",     avgChg >= 0);
        res.put("cust",      (long) cust);
        res.put("custChg",   Math.abs(custChg));
        res.put("custUp",    custChg >= 0);
        res.put("sold",      (long) sold);
        res.put("rate",      (long) rate);
        res.put("cancelled", (long) canc);
        return ResponseEntity.ok(res);
    }

    private Map<String, Object> emptyStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("rev", 0.0);   m.put("revChg", 0.0);  m.put("revUp", true);
        m.put("ord", 0L);    m.put("ordChg", 0.0);   m.put("ordUp", true);
        m.put("pend", 0L);   m.put("avg", 0.0);
        m.put("avgChg", 0.0); m.put("avgUp", true);
        m.put("cust", 0L);   m.put("custChg", 0.0);  m.put("custUp", true);
        m.put("sold", 0L);   m.put("rate", 0L);       m.put("cancelled", 0L);
        return m;
    }

    // ═══════════════════════════════
    //  2. CHART
    // ═══════════════════════════════
    @GetMapping("/chart")
    public ResponseEntity<?> chart(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime[] cur  = getRange(period, from, to);
        LocalDateTime[] prev = getPrevRange(period, cur);

        // Chọn đúng query theo period
        List<Object[]> curRows  = switch (period) {
            case "today"  -> repo.getChartHour(cur[0],  cur[1]);
            case "week"   -> repo.getChartDay(cur[0],   cur[1]);
            case "year"   -> repo.getChartMonth(cur[0], cur[1]);
            default       -> repo.getChartDay(cur[0],   cur[1]);
        };
        List<Object[]> prevRows = switch (period) {
            case "today"  -> repo.getChartHour(prev[0],  prev[1]);
            case "week"   -> repo.getChartDay(prev[0],   prev[1]);
            case "year"   -> repo.getChartMonth(prev[0], prev[1]);
            default       -> repo.getChartDay(prev[0],   prev[1]);
        };

        // Build map: label → {revenue, orders}
        Map<Integer, long[]> curMap  = new LinkedHashMap<>();
        Map<Integer, Long>   prevMap = new LinkedHashMap<>();
        curRows.forEach(r  -> curMap.put( ((Number)r[0]).intValue(), new long[]{((Number)r[1]).longValue(), ((Number)r[2]).longValue()}));
        prevRows.forEach(r -> prevMap.put(((Number)r[0]).intValue(),              ((Number)r[1]).longValue()));

        // Tạo labels đầy đủ theo period
        List<String> labels = switch (period) {
            case "today"  -> IntStream.rangeClosed(7, 21).mapToObj(h -> h + "h").toList();
            case "week"   -> List.of("T2","T3","T4","T5","T6","T7","CN");
            case "year"   -> List.of("T1","T2","T3","T4","T5","T6","T7","T8","T9","T10","T11","T12");
            default -> IntStream.rangeClosed(1, cur[0].toLocalDate().lengthOfMonth())
                    .mapToObj(String::valueOf).toList();
        };

        List<Long>   revList      = new ArrayList<>();
        List<Long>   ordList      = new ArrayList<>();
        List<Long>   revPrevList  = new ArrayList<>();

        for (int i = 0; i < labels.size(); i++) {
            int key = switch (period) {
                case "today"  -> i + 7;
                case "week"   -> i + 2;  // DATEPART WEEKDAY: 2=Mon...8=Sun (SQL Server)
                case "year"   -> i + 1;
                default       -> i + 1;
            };
            long[] cv = curMap.getOrDefault(key, new long[]{0L, 0L});
            revList.add(cv[0]);
            ordList.add(cv[1]);
            revPrevList.add(prevMap.getOrDefault(key, 0L));
        }

        // Sparkline = 7 điểm cuối
        int spFrom = Math.max(0, revList.size() - 7);

        // Tính sparkline avg và cust — chia 7 khoảng đều
        List<Double> spAvg  = new ArrayList<>();
        List<Long>   spCust = new ArrayList<>();
        long totalSeconds = java.time.Duration.between(cur[0], cur[1]).getSeconds();
        long stepSeconds  = Math.max(totalSeconds / 7, 1);
        for (int i = 0; i < 7; i++) {
            LocalDateTime s = cur[0].plusSeconds(i * stepSeconds);
            LocalDateTime e = cur[0].plusSeconds((i + 1) * stepSeconds);
            Double avg  = repo.getAvgOrderValue(s, e);
            Long   cust = repo.getNewCustomers(s, e);
            spAvg.add(avg  != null ? avg  : 0.0);
            spCust.add(cust != null ? cust : 0L);
        }

        Map<String, Object> chartRes = new HashMap<>();
        chartRes.put("cL",     labels);
        chartRes.put("cR",     revList);
        chartRes.put("cO",     ordList);
        chartRes.put("cRprev", revPrevList);
        chartRes.put("spRev",  revList.subList(spFrom, revList.size()));
        chartRes.put("spOrd",  ordList.subList(spFrom, ordList.size()));
        chartRes.put("spAvg",  spAvg);
        chartRes.put("spCust", spCust);
        return ResponseEntity.ok(chartRes);

    }

    // ═══════════════════════════════
    //  3. TOP PRODUCTS
    // ═══════════════════════════════
    @GetMapping("/top-products")
    public ResponseEntity<?> topProducts(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime[] cur = getRange(period, from, to);
        List<Object[]> rows = repo.getTopProducts(cur[0], cur[1]);

        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",   ((Number) r[0]).intValue());
            m.put("name", r[1].toString());
            m.put("sku",  r[2].toString());
            m.put("attr", r[3].toString());
            m.put("sold", ((Number) r[4]).longValue());
            m.put("rev",  ((Number) r[5]).longValue());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ═══════════════════════════════
    //  4. RECENT ORDERS
    // ═══════════════════════════════
    @GetMapping("/recent-orders")
    public ResponseEntity<?> recentOrders() {
        // Map trang_thai DB → key frontend đang dùng
        Map<String, String> statusMap = Map.of(
                "HOAN_THANH",     "completed",
                "DA_HUY",         "cancelled",
                "CHO_THANH_TOAN", "pending",
                "CHO_XAC_NHAN",   "pending",
                "CHO_GIAO",       "pending",
                "DANG_GIAO",      "pending"
        );

        List<Map<String, Object>> result = repo.getRecentOrders().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",     ((Number) r[0]).intValue());
            m.put("code",   r[1].toString());
            m.put("name",   r[2].toString());
            m.put("items",  ((Number) r[3]).intValue());
            m.put("total",  ((Number) r[4]).longValue());
            m.put("method", r[5].toString());
            m.put("status", statusMap.getOrDefault(r[6].toString(), "pending"));
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ═══════════════════════════════
    //  5. PAYMENT METHODS
    // ═══════════════════════════════
    @GetMapping("/payment-methods")
    public ResponseEntity<?> paymentMethods(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime[] cur = getRange(period, from, to);
        List<Object[]> rows = repo.getPaymentMethods(cur[0], cur[1]);

        Map<String, String> colorMap = Map.of(
                "TIEN_MAT",       "#000000",
                "Tiền mặt",       "#000000",
                "CHUYEN_KHOAN",   "#3b82f6",
                "Chuyển khoản",   "#3b82f6",
                "VNPAY",          "#0ea5e9",
                "MOMO",           "#ec4899"
        );

        List<String> labels = new ArrayList<>();
        List<Long>   values = new ArrayList<>();
        List<String> colors = new ArrayList<>();

        rows.forEach(r -> {
            String method = r[0].toString();
            labels.add(method);
            values.add(((Number) r[1]).longValue());
            colors.add(colorMap.getOrDefault(method, "#888888"));
        });

        return ResponseEntity.ok(Map.of(
                "labels", labels,
                "values", values,
                "colors", colors
        ));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<?> lowStock() {
        List<Object[]> rows = repo.getLowStock();

        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",   ((Number) r[0]).intValue());
            m.put("name", r[1].toString());
            m.put("sku",  r[2].toString());
            m.put("attr", r[3].toString());
            m.put("qty",  ((Number) r[4]).intValue());
            m.put("max",  100); // max cố định hoặc bạn thêm cột max vào query
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/heatmap")
    public ResponseEntity<?> heatmap(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        // Heatmap nên dùng range rộng (tháng/năm) để có đủ data
        LocalDateTime[] cur = getRange(period, from, to);
        List<Object[]> rows = repo.getHeatmap(cur[0], cur[1]);

        int[] dayOrder = {2, 3, 4, 5, 6, 7, 1};

        int hourStart = 7, hourEnd = 18;
        int numHours = hourEnd - hourStart + 1;
        int numDays  = 7;

        double[][] grid = new double[numDays][numHours];
        rows.forEach(r -> {
            int dow = ((Number) r[0]).intValue();
            int hr  = ((Number) r[1]).intValue();
            double rev = ((Number) r[2]).doubleValue() / 1_000_000.0;
            for (int i = 0; i < dayOrder.length; i++) {
                if (dayOrder[i] == dow && hr >= hourStart && hr <= hourEnd) {
                    grid[i][hr - hourStart] = rev;
                }
            }
        });

        List<List<Double>> values = new ArrayList<>();
        for (double[] row : grid) {
            List<Double> rowList = new ArrayList<>();
            for (double v : row) rowList.add(Math.round(v * 10.0) / 10.0);
            values.add(rowList);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("days",  List.of("T2","T3","T4","T5","T6","T7","CN"));
        res.put("hours", List.of("7h","8h","9h","10h","11h","12h","13h","14h","15h","16h","17h","18h"));
        res.put("values", values);
        return ResponseEntity.ok(res);
    }
}
