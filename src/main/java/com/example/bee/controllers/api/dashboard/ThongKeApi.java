package com.example.bee.controllers.api.dashboard;

import com.example.bee.services.ThongKeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/thong-ke")
@RequiredArgsConstructor
public class ThongKeApi {

    private final ThongKeService thongKeService;

    @GetMapping("/chi-so")
    public ResponseEntity<?> layChiSo(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return thongKeService.layChiSo(period, from, to);
    }

    @GetMapping("/bieu-do")
    public ResponseEntity<?> layBieuDo(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return thongKeService.layBieuDo(period, from, to);
    }

    @GetMapping("/phan-tich-ai")
    public ResponseEntity<?> layPhanTichAI() {
        return thongKeService.layPhanTichAI();
    }

    @GetMapping("/san-pham-ban-chay")
    public ResponseEntity<?> laySanPhamBanChay(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return thongKeService.laySanPhamBanChay(period, from, to);
    }

    @GetMapping("/don-hang-gan-day")
    public ResponseEntity<?> layDonHangGanDay() {
        return thongKeService.layDonHangGanDay();
    }

    @GetMapping("/phuong-thuc-thanh-toan")
    public ResponseEntity<?> layPhuongThucThanhToan(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return thongKeService.layPhuongThucThanhToan(period, from, to);
    }

    @GetMapping("/sap-het-hang")
    public ResponseEntity<?> layHangSapHet() {
        return thongKeService.layHangSapHet();
    }

    @GetMapping("/bieu-do-nhiet")
    public ResponseEntity<?> layBieuDoNhiet(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return thongKeService.layBieuDoNhiet(period, from, to);
    }

    @PostMapping("/chatbot")
    public ResponseEntity<?> xuLyChatbot(@RequestBody Map<String, String> payload) {
        return thongKeService.xuLyChatbot(payload);
    }
}