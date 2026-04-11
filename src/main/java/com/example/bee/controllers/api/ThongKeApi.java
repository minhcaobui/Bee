package com.example.bee.controllers.api.dashboard;

import com.example.bee.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/thong-ke")
@RequiredArgsConstructor
public class ThongKeApi {

    private final DashboardService dashboardService;

    @GetMapping("/chi-so")
    public ResponseEntity<?> layChiSo(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return dashboardService.layChiSo(period, from, to);
    }

    @GetMapping("/bieu-do")
    public ResponseEntity<?> layBieuDo(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return dashboardService.layBieuDo(period, from, to);
    }

    @GetMapping("/phan-tich-ai")
    public ResponseEntity<?> layPhanTichAI() {
        return dashboardService.layPhanTichAI();
    }

    @GetMapping("/san-pham-ban-chay")
    public ResponseEntity<?> laySanPhamBanChay(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return dashboardService.laySanPhamBanChay(period, from, to);
    }

    @GetMapping("/don-hang-gan-day")
    public ResponseEntity<?> layDonHangGanDay() {
        return dashboardService.layDonHangGanDay();
    }

    @GetMapping("/phuong-thuc-thanh-toan")
    public ResponseEntity<?> layPhuongThucThanhToan(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return dashboardService.layPhuongThucThanhToan(period, from, to);
    }

    @GetMapping("/sap-het-hang")
    public ResponseEntity<?> layHangSapHet() {
        return dashboardService.layHangSapHet();
    }

    @GetMapping("/bieu-do-nhiet")
    public ResponseEntity<?> layBieuDoNhiet(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return dashboardService.layBieuDoNhiet(period, from, to);
    }

    @PostMapping("/chatbot")
    public ResponseEntity<?> xuLyChatbot(@RequestBody Map<String, String> payload) {
        return dashboardService.xuLyChatbot(payload);
    }
}