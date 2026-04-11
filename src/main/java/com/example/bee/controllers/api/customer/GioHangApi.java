package com.example.bee.controllers.api.cart;

import com.example.bee.services.GioHangService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gio-hang")
@RequiredArgsConstructor
public class GioHangApi {

    private final GioHangService gioHangService;

    @GetMapping
    public ResponseEntity<?> layGioHangCuaToi() {
        return gioHangService.layGioHangCuaToi();
    }

    @PostMapping("/them")
    public ResponseEntity<?> themVaoGio(@RequestBody Map<String, Integer> payload) {
        return gioHangService.themVaoGio(payload);
    }

    @PutMapping("/cap-nhat")
    public ResponseEntity<?> capNhatSoLuong(@RequestBody Map<String, Integer> payload) {
        return gioHangService.capNhatSoLuong(payload);
    }

    @DeleteMapping("/xoa/{idGioHangChiTiet}")
    public ResponseEntity<?> xoaKhoiGio(@PathVariable("idGioHangChiTiet") Integer idGioHangChiTiet) {
        return gioHangService.xoaKhoiGio(idGioHangChiTiet);
    }

    @DeleteMapping("/xoa-tat-ca")
    public ResponseEntity<?> xoaTatCa() {
        return gioHangService.xoaTatCa();
    }
}