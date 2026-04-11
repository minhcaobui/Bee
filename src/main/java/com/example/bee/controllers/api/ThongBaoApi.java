package com.example.bee.controllers.api.notification;

import com.example.bee.services.ThongBaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/thong-bao")
@RequiredArgsConstructor
public class ThongBaoApi {

    private final ThongBaoService thongBaoService;

    @GetMapping("/cua-toi")
    public ResponseEntity<?> layThongBaoCuaToi() {
        return thongBaoService.layThongBaoCuaToi();
    }

    @PatchMapping("/{id}/da-doc")
    public ResponseEntity<?> danhDauDaDoc(@PathVariable Long id) {
        return thongBaoService.danhDauDaDoc(id);
    }

    @PatchMapping("/doc-tat-ca")
    public ResponseEntity<?> docTatCa() {
        return thongBaoService.docTatCa();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> xoaThongBao(@PathVariable Long id) {
        return thongBaoService.xoaThongBao(id);
    }

    @DeleteMapping("/xoa-tat-ca")
    public ResponseEntity<?> xoaTatCaThongBao() {
        return thongBaoService.xoaTatCaThongBao();
    }
}