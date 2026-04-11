package com.example.bee.controllers.api.customer;

import com.example.bee.dtos.DanhGiaRequest;
import com.example.bee.dtos.DiaChiRequest;
import com.example.bee.dtos.KhachHangRequest;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.services.KhachHangService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangApi {

    private final KhachHangService khachHangService;

    @GetMapping
    public ResponseEntity<?> layDanhSach(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(khachHangService.layDanhSachKhachHang(q, trangThai, page, size));
    }

    @GetMapping("/{id}")
    public KhachHang layChiTiet(@PathVariable Integer id) {
        return khachHangService.layChiTietKhachHang(id);
    }

    @PostMapping
    public ResponseEntity<?> taoMoi(@RequestBody KhachHangRequest req) {
        return khachHangService.taoMoiKhachHang(req);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(@PathVariable Integer id, @RequestBody KhachHangRequest req) {
        return khachHangService.capNhatKhachHang(id, req);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable Integer id) {
        return khachHangService.doiTrangThai(id);
    }

    @GetMapping("/{id}/hoa-don")
    public ResponseEntity<?> layHoaDonKhachHang(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "") String statusId
    ) {
        return khachHangService.layHoaDonCuaKhachHang(id, page, size, q, statusId);
    }

    @GetMapping("/{id}/san-pham-da-mua")
    public ResponseEntity<?> laySanPhamDaMua(@PathVariable Integer id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "5") int size) {
        return khachHangService.laySanPhamDaMua(id, page, size);
    }

    @GetMapping("/{id}/voucher-da-dung")
    public ResponseEntity<?> layVoucherDaDung(@PathVariable Integer id,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "5") int size) {
        return khachHangService.layVoucherDaDung(id, page, size);
    }

    @GetMapping("/ho-so-cua-toi")
    public ResponseEntity<?> layHoSoCaNhan(Authentication authentication) {
        return khachHangService.layHoSoCuaToi(authentication);
    }

    @PutMapping("/ho-so-cua-toi")
    public ResponseEntity<?> capNhatHoSoCaNhan(@RequestBody Map<String, String> payload, Authentication authentication) {
        return khachHangService.capNhatHoSoCuaToi(payload, authentication);
    }

    @GetMapping("/{id}/dia-chi")
    public ResponseEntity<?> layDanhSachDiaChi(@PathVariable Integer id) {
        return khachHangService.layDanhSachDiaChi(id);
    }

    @PostMapping("/{id}/dia-chi")
    public ResponseEntity<?> themDiaChiAdmin(@PathVariable Integer id, @RequestBody DiaChiRequest req) {
        return khachHangService.themDiaChiChoKhachHang(id, req);
    }

    @DeleteMapping("/dia-chi/{idDiaChi}")
    public ResponseEntity<?> xoaDiaChiAdmin(@PathVariable Integer idDiaChi) {
        return khachHangService.xoaDiaChi(idDiaChi);
    }

    @PutMapping("/dia-chi/{idDiaChi}/mac-dinh")
    public ResponseEntity<?> datDiaChiMacDinhAdmin(@PathVariable Integer idDiaChi) {
        return khachHangService.datDiaChiMacDinh(idDiaChi);
    }

    @PostMapping("/doi-mat-khau-ca-nhan")
    public ResponseEntity<?> doiMatKhauCaNhan(@RequestBody Map<String, String> request) {
        return khachHangService.doiMatKhauCaNhan(request);
    }

    @GetMapping("/dia-chi-ca-nhan")
    public ResponseEntity<?> layDiaChiCaNhan(Authentication authentication) {
        return khachHangService.layDiaChiCaNhan(authentication);
    }

    @GetMapping("/dia-chi-ca-nhan/mac-dinh")
    public ResponseEntity<?> layDiaChiCaNhanMacDinh(Authentication authentication) {
        return khachHangService.layDiaChiCaNhanMacDinh(authentication);
    }

    @PostMapping("/dia-chi-ca-nhan")
    public ResponseEntity<?> themDiaChiCaNhan(@RequestBody DiaChiRequest req, Authentication authentication) {
        return khachHangService.themDiaChiCaNhan(req, authentication);
    }

    @PutMapping("/dia-chi-ca-nhan/{id}")
    public ResponseEntity<?> capNhatDiaChiCaNhan(@PathVariable Integer id, @RequestBody DiaChiRequest req, Authentication authentication) {
        return khachHangService.capNhatDiaChiCaNhan(id, req, authentication);
    }

    @PutMapping("/dia-chi-ca-nhan/{id}/mac-dinh")
    public ResponseEntity<?> datDiaChiCaNhanMacDinh(@PathVariable Integer id, Authentication authentication) {
        return khachHangService.datDiaChiCaNhanMacDinh(id, authentication);
    }

    @DeleteMapping("/dia-chi-ca-nhan/{idDiaChi}")
    public ResponseEntity<?> xoaDiaChiCaNhan(@PathVariable Integer idDiaChi, Authentication authentication) {
        return khachHangService.xoaDiaChiCaNhan(idDiaChi, authentication);
    }

    @GetMapping("/yeu-thich/kiem-tra/{sanPhamId}")
    public ResponseEntity<?> kiemTraYeuThich(@PathVariable Integer sanPhamId, Authentication authentication) {
        return khachHangService.kiemTraYeuThich(sanPhamId, authentication);
    }

    @PostMapping("/yeu-thich/thay-doi/{sanPhamId}")
    public ResponseEntity<?> thayDoiYeuThich(@PathVariable Integer sanPhamId, Authentication authentication) {
        return khachHangService.thayDoiYeuThich(sanPhamId, authentication);
    }

    @GetMapping("/danh-gia/{sanPhamId}")
    public ResponseEntity<?> layDanhGiaSanPham(@PathVariable Integer sanPhamId) {
        return khachHangService.layDanhGiaSanPham(sanPhamId);
    }

    @PostMapping("/danh-gia/chi-tiet/{orderDetailId}")
    public ResponseEntity<?> themDanhGia(
            @PathVariable Integer orderDetailId,
            @RequestBody DanhGiaRequest req,
            Authentication auth) {
        return khachHangService.themDanhGia(orderDetailId, req, auth);
    }

    @GetMapping("/yeu-thich")
    public ResponseEntity<?> layDanhSachYeuThich(Authentication authentication) {
        return khachHangService.layDanhSachYeuThich(authentication);
    }

    @GetMapping("/danh-gia-cua-toi")
    public ResponseEntity<?> layToanBoDanhGiaCuaToi(Authentication authentication) {
        return khachHangService.layToanBoDanhGiaCuaToi(authentication);
    }

    @PatchMapping("/{id}/doi-mat-khau")
    public ResponseEntity<?> adminDoiMatKhauChoKhach(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return khachHangService.adminDoiMatKhauChoKhach(id, body);
    }

    @GetMapping("/danh-gia/kiem-tra-dieu-kien/{sanPhamId}")
    public ResponseEntity<?> kiemTraDieuKienDanhGia(@PathVariable Integer sanPhamId, Authentication authentication) {
        return khachHangService.kiemTraDieuKienDanhGia(sanPhamId, authentication);
    }

    @GetMapping("/kiem-tra-email")
    public ResponseEntity<?> kiemTraEmail(@RequestParam String email, @RequestParam(required = false) Integer id) {
        return khachHangService.kiemTraEmail(email, id);
    }
}