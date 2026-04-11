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
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(defaultValue = "0") int trang,
            @RequestParam(defaultValue = "10") int kichThuocTrang) {
        return ResponseEntity.ok(khachHangService.layDanhSachKhachHang(tuKhoa, trangThai, trang, kichThuocTrang));
    }

    @GetMapping("/{id}")
    public KhachHang layChiTiet(@PathVariable Integer id) {
        return khachHangService.layChiTietKhachHang(id);
    }

    @PostMapping
    public ResponseEntity<?> taoMoi(@RequestBody KhachHangRequest yeuCau) {
        return khachHangService.taoMoiKhachHang(yeuCau);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(@PathVariable Integer id, @RequestBody KhachHangRequest yeuCau) {
        return khachHangService.capNhatKhachHang(id, yeuCau);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable Integer id) {
        return khachHangService.doiTrangThai(id);
    }

    @GetMapping("/{id}/hoa-don")
    public ResponseEntity<?> layHoaDonKhachHang(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int trang,
            @RequestParam(defaultValue = "5") int kichThuocTrang,
            @RequestParam(required = false, defaultValue = "") String tuKhoa,
            @RequestParam(required = false, defaultValue = "") String idTrangThai
    ) {
        return khachHangService.layHoaDonCuaKhachHang(id, trang, kichThuocTrang, tuKhoa, idTrangThai);
    }

    @GetMapping("/{id}/san-pham-da-mua")
    public ResponseEntity<?> laySanPhamDaMua(@PathVariable Integer id,
                                             @RequestParam(defaultValue = "0") int trang,
                                             @RequestParam(defaultValue = "5") int kichThuocTrang) {
        return khachHangService.laySanPhamDaMua(id, trang, kichThuocTrang);
    }

    @GetMapping("/{id}/ma-giam-gia-da-dung")
    public ResponseEntity<?> layMaGiamGiaDaDung(@PathVariable Integer id,
                                                @RequestParam(defaultValue = "0") int trang,
                                                @RequestParam(defaultValue = "5") int kichThuocTrang) {
        return khachHangService.layVoucherDaDung(id, trang, kichThuocTrang);
    }

    @GetMapping("/ho-so-cua-toi")
    public ResponseEntity<?> layHoSoCaNhan(Authentication authentication) {
        return khachHangService.layHoSoCuaToi(authentication);
    }

    @PutMapping("/ho-so-cua-toi")
    public ResponseEntity<?> capNhatHoSoCaNhan(@RequestBody Map<String, String> duLieu, Authentication authentication) {
        return khachHangService.capNhatHoSoCuaToi(duLieu, authentication);
    }

    @GetMapping("/{id}/dia-chi")
    public ResponseEntity<?> layDanhSachDiaChi(@PathVariable Integer id) {
        return khachHangService.layDanhSachDiaChi(id);
    }

    @PostMapping("/{id}/dia-chi")
    public ResponseEntity<?> themDiaChiAdmin(@PathVariable Integer id, @RequestBody DiaChiRequest yeuCau) {
        return khachHangService.themDiaChiChoKhachHang(id, yeuCau);
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
    public ResponseEntity<?> doiMatKhauCaNhan(@RequestBody Map<String, String> yeuCau) {
        return khachHangService.doiMatKhauCaNhan(yeuCau);
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
    public ResponseEntity<?> themDiaChiCaNhan(@RequestBody DiaChiRequest yeuCau, Authentication authentication) {
        return khachHangService.themDiaChiCaNhan(yeuCau, authentication);
    }

    @PutMapping("/dia-chi-ca-nhan/{id}")
    public ResponseEntity<?> capNhatDiaChiCaNhan(@PathVariable Integer id, @RequestBody DiaChiRequest yeuCau, Authentication authentication) {
        return khachHangService.capNhatDiaChiCaNhan(id, yeuCau, authentication);
    }

    @PutMapping("/dia-chi-ca-nhan/{id}/mac-dinh")
    public ResponseEntity<?> datDiaChiCaNhanMacDinh(@PathVariable Integer id, Authentication authentication) {
        return khachHangService.datDiaChiCaNhanMacDinh(id, authentication);
    }

    @DeleteMapping("/dia-chi-ca-nhan/{idDiaChi}")
    public ResponseEntity<?> xoaDiaChiCaNhan(@PathVariable Integer idDiaChi, Authentication authentication) {
        return khachHangService.xoaDiaChiCaNhan(idDiaChi, authentication);
    }

    @GetMapping("/yeu-thich/kiem-tra/{idSanPham}")
    public ResponseEntity<?> kiemTraYeuThich(@PathVariable Integer idSanPham, Authentication authentication) {
        return khachHangService.kiemTraYeuThich(idSanPham, authentication);
    }

    @PostMapping("/yeu-thich/thay-doi/{idSanPham}")
    public ResponseEntity<?> thayDoiYeuThich(@PathVariable Integer idSanPham, Authentication authentication) {
        return khachHangService.thayDoiYeuThich(idSanPham, authentication);
    }

    @GetMapping("/danh-gia/{idSanPham}")
    public ResponseEntity<?> layDanhGiaSanPham(@PathVariable Integer idSanPham) {
        return khachHangService.layDanhGiaSanPham(idSanPham);
    }

    @PostMapping("/danh-gia/chi-tiet/{idHoaDonChiTiet}")
    public ResponseEntity<?> themDanhGia(
            @PathVariable Integer idHoaDonChiTiet,
            @RequestBody DanhGiaRequest yeuCau,
            Authentication auth) {
        return khachHangService.themDanhGia(idHoaDonChiTiet, yeuCau, auth);
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
    public ResponseEntity<?> adminDoiMatKhauChoKhach(@PathVariable Integer id, @RequestBody Map<String, String> duLieu) {
        return khachHangService.adminDoiMatKhauChoKhach(id, duLieu);
    }

    @GetMapping("/danh-gia/kiem-tra-dieu-kien/{idSanPham}")
    public ResponseEntity<?> kiemTraDieuKienDanhGia(@PathVariable Integer idSanPham, Authentication authentication) {
        return khachHangService.kiemTraDieuKienDanhGia(idSanPham, authentication);
    }
}