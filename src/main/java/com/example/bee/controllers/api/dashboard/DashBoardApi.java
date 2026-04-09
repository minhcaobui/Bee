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
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashBoardApi {

    private final DashboardRepository repo;

    private LocalDateTime[] getRange(String period, String from, String to) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case "today" -> new LocalDateTime[]{
                    today.atStartOfDay(),
                    today.atTime(23, 59, 59)
            };
            case "week" -> new LocalDateTime[]{
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(),
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(23, 59, 59)
            };
            case "month" -> new LocalDateTime[]{
                    today.withDayOfMonth(1).atStartOfDay(),
                    today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59)
            };
            case "year" -> new LocalDateTime[]{
                    today.withDayOfYear(1).atStartOfDay(),
                    today.withDayOfYear(today.lengthOfYear()).atTime(23, 59, 59)
            };
            default -> new LocalDateTime[]{
                    LocalDate.parse(from).atStartOfDay(),
                    LocalDate.parse(to).atTime(23, 59, 59)
            };
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

    @GetMapping("/stats")
    public ResponseEntity<?> stats(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime[] cur = getRange(period, from, to);
        LocalDateTime[] prev = getPrevRange(period, cur);

        List<Object[]> cList = repo.getStats(cur[0], cur[1]);
        List<Object[]> pList = repo.getStats(prev[0], prev[1]);

        if (cList.isEmpty()) return ResponseEntity.ok(emptyStats());

        Object[] c = cList.get(0);
        Object[] p = pList.isEmpty() ? new Object[8] : pList.get(0);

        double rev = c[0] != null ? ((Number) c[0]).doubleValue() : 0;
        double ord = c[1] != null ? ((Number) c[1]).doubleValue() : 0;
        double avg = c[2] != null ? ((Number) c[2]).doubleValue() : 0;
        double pend = c[3] != null ? ((Number) c[3]).doubleValue() : 0;
        double canc = c[4] != null ? ((Number) c[4]).doubleValue() : 0;
        double comp = c[5] != null ? ((Number) c[5]).doubleValue() : 0;
        double sold = c[6] != null ? ((Number) c[6]).doubleValue() : 0;
        double cust = c[7] != null ? ((Number) c[7]).doubleValue() : 0;

        double pureComp = c.length > 8 && c[8] != null ? ((Number) c[8]).doubleValue() : 0;
        double exchanged = c.length > 9 && c[9] != null ? ((Number) c[9]).doubleValue() : 0;
        double returned = c.length > 10 && c[10] != null ? ((Number) c[10]).doubleValue() : 0;

        double pRev = p[0] != null ? ((Number) p[0]).doubleValue() : 0;
        double pOrd = p[1] != null ? ((Number) p[1]).doubleValue() : 0;
        double pAvg = p[2] != null ? ((Number) p[2]).doubleValue() : 0;
        double pCust = p[7] != null ? ((Number) p[7]).doubleValue() : 0;

        double rate = ord > 0 ? Math.round((comp / ord) * 100.0) : 0;

        double revChg = calcChg(rev, pRev);
        double ordChg = calcChg(ord, pOrd);
        double avgChg = calcChg(avg, pAvg);
        double custChg = calcChg(cust, pCust);

        Map<String, Object> res = new HashMap<>();
        res.put("rev", rev);
        res.put("revChg", Math.abs(revChg));
        res.put("revUp", revChg >= 0);
        res.put("ord", (long) ord);
        res.put("ordChg", Math.abs(ordChg));
        res.put("ordUp", ordChg >= 0);
        res.put("pend", (long) pend);
        res.put("avg", avg);
        res.put("avgChg", Math.abs(avgChg));
        res.put("avgUp", avgChg >= 0);
        res.put("cust", (long) cust);
        res.put("custChg", Math.abs(custChg));
        res.put("custUp", custChg >= 0);
        res.put("sold", (long) sold);
        res.put("rate", (long) rate);
        res.put("cancelled", (long) canc);

        res.put("pureCompleted", (long) pureComp);
        res.put("exchanged", (long) exchanged);
        res.put("returned", (long) returned);

        return ResponseEntity.ok(res);
    }

    private Map<String, Object> emptyStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("rev", 0.0);
        m.put("revChg", 0.0);
        m.put("revUp", true);
        m.put("ord", 0L);
        m.put("ordChg", 0.0);
        m.put("ordUp", true);
        m.put("pend", 0L);
        m.put("avg", 0.0);
        m.put("avgChg", 0.0);
        m.put("avgUp", true);
        m.put("cust", 0L);
        m.put("custChg", 0.0);
        m.put("custUp", true);
        m.put("sold", 0L);
        m.put("rate", 0L);
        m.put("cancelled", 0L);
        m.put("pureCompleted", 0L);
        m.put("exchanged", 0L);
        m.put("returned", 0L);
        return m;
    }

    @GetMapping("/chart")
    public ResponseEntity<?> chart(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime[] cur = getRange(period, from, to);
        LocalDateTime[] prev = getPrevRange(period, cur);

        List<Object[]> curRows = switch (period) {
            case "today" -> repo.getChartHour(cur[0], cur[1]);
            case "week" -> repo.getChartWeek(cur[0], cur[1]);
            case "year" -> repo.getChartMonth(cur[0], cur[1]);
            default -> repo.getChartDay(cur[0], cur[1]);
        };
        List<Object[]> prevRows = switch (period) {
            case "today" -> repo.getChartHour(prev[0], prev[1]);
            case "week" -> repo.getChartWeek(prev[0], prev[1]);
            case "year" -> repo.getChartMonth(prev[0], prev[1]);
            default -> repo.getChartDay(prev[0], prev[1]);
        };

        Map<Integer, long[]> curMap = new LinkedHashMap<>();
        Map<Integer, Long> prevMap = new LinkedHashMap<>();
        curRows.forEach(r -> curMap.put(((Number) r[0]).intValue(), new long[]{((Number) r[1]).longValue(), ((Number) r[2]).longValue()}));
        prevRows.forEach(r -> prevMap.put(((Number) r[0]).intValue(), ((Number) r[1]).longValue()));

        List<String> labels = switch (period) {
            case "today" -> IntStream.rangeClosed(7, 21).mapToObj(h -> h + "h").toList();
            case "week" -> List.of("T2", "T3", "T4", "T5", "T6", "T7", "CN");
            case "year" -> List.of("T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12");
            default -> IntStream.rangeClosed(1, cur[0].toLocalDate().lengthOfMonth())
                    .mapToObj(String::valueOf).toList();
        };

        List<Long> revList = new ArrayList<>();
        List<Long> ordList = new ArrayList<>();
        List<Long> revPrevList = new ArrayList<>();

        for (int i = 0; i < labels.size(); i++) {
            int key = switch (period) {
                case "today" -> i + 7;
                case "week" -> (i == 6) ? 1 : i + 2;
                case "year" -> i + 1;
                default -> i + 1;
            };
            long[] cv = curMap.getOrDefault(key, new long[]{0L, 0L});
            revList.add(cv[0]);
            ordList.add(cv[1]);
            revPrevList.add(prevMap.getOrDefault(key, 0L));
        }

        int spFrom = Math.max(0, revList.size() - 7);

        List<Double> spAvg = new ArrayList<>();
        List<Long> spCust = new ArrayList<>();
        long totalSeconds = java.time.Duration.between(cur[0], cur[1]).getSeconds();
        long stepSeconds = Math.max(totalSeconds / 7, 1);
        for (int i = 0; i < 7; i++) {
            LocalDateTime s = cur[0].plusSeconds(i * stepSeconds);
            LocalDateTime e = cur[0].plusSeconds((i + 1) * stepSeconds);
            Double avg = repo.getAvgOrderValue(s, e);
            Long cust = repo.getNewCustomers(s, e);
            spAvg.add(avg != null ? avg : 0.0);
            spCust.add(cust != null ? cust : 0L);
        }

        Map<String, Object> chartRes = new HashMap<>();
        chartRes.put("cL", labels);
        chartRes.put("cR", revList);
        chartRes.put("cO", ordList);
        chartRes.put("cRprev", revPrevList);
        chartRes.put("spRev", revList.subList(spFrom, revList.size()));
        chartRes.put("spOrd", ordList.subList(spFrom, ordList.size()));
        chartRes.put("spAvg", spAvg);
        chartRes.put("spCust", spCust);
        return ResponseEntity.ok(chartRes);
    }

    @GetMapping("/ai-insights")
    public ResponseEntity<?> getAiInsights() {
        List<Map<String, Object>> insights = new ArrayList<>();

        LocalDateTime[] curMonth = getRange("month", null, null);
        LocalDateTime[] prevMonth = getPrevRange("month", curMonth);
        List<Object[]> cList = repo.getStats(curMonth[0], curMonth[1]);
        List<Object[]> pList = repo.getStats(prevMonth[0], prevMonth[1]);

        if (!cList.isEmpty() && !pList.isEmpty()) {
            double cRev = cList.get(0)[0] != null ? ((Number) cList.get(0)[0]).doubleValue() : 0;
            double pRev = pList.get(0)[0] != null ? ((Number) pList.get(0)[0]).doubleValue() : 0;
            double cCanc = cList.get(0)[4] != null ? ((Number) cList.get(0)[4]).doubleValue() : 0;
            double cOrd = cList.get(0)[1] != null ? ((Number) cList.get(0)[1]).doubleValue() : 0;

            if (pRev > 0) {
                double growth = Math.round((cRev - pRev) / pRev * 100.0);
                if (growth > 5) {
                    insights.add(Map.of("type", "success", "msg", "Tốt quá! Doanh thu tháng này đã tăng " + growth + "% so với tháng trước. Hệ thống đang hoạt động rất hiệu quả."));
                } else if (growth < -5) {
                    insights.add(Map.of("type", "warning", "msg", "Lưu ý: Doanh thu tháng này đang giảm " + Math.abs(growth) + "% so với tháng trước. Hãy xem xét tung ra các đợt Sale hoặc Voucher mới."));
                }
            }

            if (cOrd > 0) {
                double cancelRate = Math.round((cCanc / cOrd) * 100.0);
                if (cancelRate > 15) {
                    insights.add(Map.of("type", "error", "msg", "Báo động: Tỷ lệ hủy đơn đang ở mức cao (" + cancelRate + "%). Cần rà soát lại quy trình giao hàng hoặc bộ phận chăm sóc khách hàng."));
                }
            }
        }

        List<Object[]> lowStock = repo.getLowStock();
        if (lowStock != null && !lowStock.isEmpty()) {
            if (lowStock.size() >= 3) {
                insights.add(Map.of("type", "warning", "msg", "Có " + lowStock.size() + " sản phẩm đang sắp cạn kho. Hệ thống khuyến nghị bổ sung nguồn hàng ngay lập tức để không gián đoạn doanh thu."));
            } else {
                Object[] item = lowStock.get(0);
                insights.add(Map.of("type", "info", "msg", "Sản phẩm '" + item[1] + "' phân loại '" + item[3] + "' sắp hết (còn " + item[4] + ")."));
            }
        }

        insights.add(Map.of("type", "info", "msg", "Dữ liệu Heatmap cho thấy khách hàng thường chốt đơn mạnh vào 10h-12h trưa và 19h-21h tối. Đây là " +
                "khung giờ vàng để chạy Flash Sale."));

        return ResponseEntity.ok(insights);
    }

    @GetMapping("/top-products")
    public ResponseEntity<?> topProducts(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime[] cur = getRange(period, from, to);
        List<Object[]> rows = repo.getTopProducts(cur[0], cur[1]);

        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ((Number) r[0]).intValue());
            m.put("name", r[1].toString());
            m.put("sku", r[2].toString());
            m.put("attr", r[3].toString());
            m.put("sold", ((Number) r[4]).longValue());
            m.put("rev", ((Number) r[5]).longValue());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/recent-orders")
    public ResponseEntity<?> recentOrders() {
        Map<String, String> statusMap = Map.of(
                "HOAN_THANH", "completed",
                "DA_HUY", "cancelled",
                "CHO_THANH_TOAN", "pending",
                "CHO_XAC_NHAN", "pending",
                "CHO_GIAO", "pending",
                "DANG_GIAO", "pending"
        );

        List<Map<String, Object>> result = repo.getRecentOrders().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ((Number) r[0]).intValue());
            m.put("code", r[1].toString());
            m.put("name", r[2].toString());
            m.put("items", ((Number) r[3]).intValue());
            m.put("total", ((Number) r[4]).longValue());
            m.put("method", r[5].toString());
            m.put("status", statusMap.getOrDefault(r[6].toString(), "pending"));
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/payment-methods")
    public ResponseEntity<?> paymentMethods(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime[] cur = getRange(period, from, to);
        List<Object[]> rows = repo.getPaymentMethods(cur[0], cur[1]);

        Map<String, Long> aggregated = new LinkedHashMap<>();

        rows.forEach(r -> {
            String rawMethod = r[0] != null ? r[0].toString().toUpperCase() : "TIEN_MAT";
            String key = "TIEN_MAT";

            if (rawMethod.contains("MOMO")) key = "MOMO";
            else if (rawMethod.contains("VNPAY")) key = "VNPAY";
            else if (rawMethod.contains("COD") || rawMethod.contains("GIAO")) key = "COD";
            else if (rawMethod.contains("CHUYEN_KHOAN") || rawMethod.contains("CHUYỂN KHOẢN")) key = "CHUYEN_KHOAN";

            aggregated.put(key, aggregated.getOrDefault(key, 0L) + ((Number) r[1]).longValue());
        });

        List<String> keys = new ArrayList<>(aggregated.keySet());
        List<Long> values = new ArrayList<>(aggregated.values());

        return ResponseEntity.ok(Map.of(
                "keys", keys,
                "values", values
        ));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<?> lowStock() {
        List<Object[]> rows = repo.getLowStock();

        List<Map<String, Object>> result = rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ((Number) r[0]).intValue());
            m.put("name", r[1].toString());
            m.put("sku", r[2].toString());
            m.put("attr", r[3].toString());
            m.put("qty", ((Number) r[4]).intValue());
            m.put("max", 100);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/heatmap")
    public ResponseEntity<?> heatmap(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime[] cur = getRange(period, from, to);
        List<Object[]> rows = repo.getHeatmap(cur[0], cur[1]);

        int[] dayOrder = {2, 3, 4, 5, 6, 7, 1};
        int hourStart = 7, hourEnd = 18;
        int numHours = hourEnd - hourStart + 1;
        int numDays = 7;

        double[][] grid = new double[numDays][numHours];
        rows.forEach(r -> {
            int dow = ((Number) r[0]).intValue();
            int hr = ((Number) r[1]).intValue();
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
        res.put("days", List.of("T2", "T3", "T4", "T5", "T6", "T7", "CN"));
        res.put("hours", List.of("7h", "8h", "9h", "10h", "11h", "12h", "13h", "14h", "15h", "16h", "17h", "18h"));
        res.put("values", values);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/chat")
    public ResponseEntity<?> handleChatbot(@RequestBody Map<String, String> payload) {
        String userMsg = payload.getOrDefault("message", "").toLowerCase();
        String reply = "Xin lỗi sếp, em chưa hiểu ý sếp lắm. Sếp có thể hỏi về doanh thu hoặc đơn hàng hôm nay nhé!";

        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

        try {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

            if (userMsg.contains("doanh thu") || userMsg.contains("bán được")) {
                List<Object[]> stats = repo.getStats(startOfDay, endOfDay);
                if (!stats.isEmpty() && stats.get(0)[0] != null) {
                    double rev = ((Number) stats.get(0)[0]).doubleValue();
                    reply = "Báo cáo sếp, doanh thu thực tế hôm nay là: <b>" + currencyFormat.format(rev) + " ₫</b> ạ!";
                } else {
                    reply = "Hôm nay chưa có doanh thu nào sếp ạ";
                }
            } else if (userMsg.contains("đơn hàng") || userMsg.contains("bao nhiêu đơn")) {
                List<Object[]> stats = repo.getStats(startOfDay, endOfDay);
                if (!stats.isEmpty() && stats.get(0)[1] != null) {
                    long ord = ((Number) stats.get(0)[1]).longValue();
                    reply = "Hôm nay hệ thống ghi nhận tổng cộng <b>" + ord + "</b> đơn hàng sếp nhé!";
                } else {
                    reply = "Hôm nay chưa có đơn hàng nào cả sếp ơi.";
                }
            } else if (userMsg.contains("chào") || userMsg.contains("hello")) {
                reply = "Dạ em chào sếp! Chúc sếp một ngày làm việc chốt được ngàn đơn nhé!";
            } else if (userMsg.contains("tồn kho") || userMsg.contains("hết hàng")) {
                List<Object[]> lowStock = repo.getLowStock();
                if (lowStock.isEmpty()) {
                    reply = "Tuyệt vời, hiện tại không có sản phẩm nào sắp hết hàng sếp nhé!";
                } else {
                    reply = "Cảnh báo sếp: Đang có <b>" + lowStock.size() + "</b> mã sản phẩm sắp cạn kho. Sếp nhớ nhập thêm hàng nha!";
                }
            }

        } catch (Exception e) {
            reply = "Đang có lỗi lúc tra cứu dữ liệu sếp ạ: " + e.getMessage();
        }

        return ResponseEntity.ok(Map.of("reply", reply));
    }
}