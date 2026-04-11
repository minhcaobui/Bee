//package com.example.bee.controllers.api.order;
//
//import com.example.bee.services.DoiTraService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/doi-tra")
//@RequiredArgsConstructor
//public class DoiTraApi {
//
//    private final DoiTraService doiTraService;
//
//    @GetMapping("/danh-sach")
//    public ResponseEntity<?> layDanhSach() {
//        return doiTraService.layDanhSach();
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<?> layChiTiet(@PathVariable Integer id) {
//        return doiTraService.layChiTiet(id);
//    }
//
//    @PostMapping("/{id}/phe-duyet")
//    public ResponseEntity<?> pheDuyetYeuCau(@PathVariable Integer id, @RequestBody(required = false) Map<String, Object> payload) {
//        return doiTraService.pheDuyetYeuCau(id, payload);
//    }
//
//    @PostMapping("/{id}/tu-choi")
//    public ResponseEntity<?> tuChoiYeuCau(@PathVariable Integer id) {
//        return doiTraService.tuChoiYeuCau(id);
//    }
//
//    @GetMapping("/tra-cuu-hoa-don/{ma}")
//    public ResponseEntity<?> traCuuHoaDonDeTra(@PathVariable String ma) {
//        return doiTraService.traCuuHoaDonDeTra(ma);
//    }
//
//    @PostMapping("/tao-moi")
//    public ResponseEntity<?> taoYeuCauMoi(@RequestBody Map<String, Object> payload) {
//        return doiTraService.taoYeuCauMoi(payload);
//    }
//}