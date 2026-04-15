package com.example.bee.controllers.api.customer;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.cart.GioHang;
import com.example.bee.entities.cart.GioHangChiTiet;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.cart.GioHangChiTietRepository;
import com.example.bee.repositories.cart.GioHangRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gio-hang")
@RequiredArgsConstructor
@Transactional
public class GioHangApi {

    private final GioHangRepository gioHangRepo;
    private final GioHangChiTietRepository gioHangChiTietRepo;
    private final SanPhamChiTietRepository spctRepo;
    private final TaiKhoanRepository taiKhoanRepo;

    private TaiKhoan getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return taiKhoanRepo.findByTenDangNhap(auth.getName()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> getMyCart() {
        TaiKhoan tk = getLoggedInUser();
        if (tk == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập"));
        }

        GioHang gh = gioHangRepo.findByTaiKhoan_Id(tk.getId()).orElse(null);
        if (gh == null) {
            gh = new GioHang();
            gh.setTaiKhoan(tk);
            gh = gioHangRepo.save(gh);
        }

        List<GioHangChiTiet> chiTiets = gioHangChiTietRepo.findByGioHang_Id(gh.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (GioHangChiTiet ct : chiTiets) {
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            if (spct == null) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("id", ct.getId());
            item.put("idSanPhamChiTiet", spct.getId());

            // 🌟 BỌC THÉP CHỐNG LỖI NULL POINTER EXCEPTION
            String tenSp = spct.getSanPham() != null ? spct.getSanPham().getTen() : "Sản phẩm";
            Integer idSp = spct.getSanPham() != null ? spct.getSanPham().getId() : 0;
            String tenMau = spct.getMauSac() != null ? spct.getMauSac().getTen() : "";
            String tenKichThuoc = spct.getKichThuoc() != null ? spct.getKichThuoc().getTen() : "";

            item.put("idSanPham", idSp);
            item.put("tenSanPham", tenSp);
            item.put("thuocTinh", tenMau + " - " + tenKichThuoc);
            item.put("hinhAnh", spct.getHinhAnh());
            item.put("giaBan", spct.getGiaBan());
            item.put("giaSauKhuyenMai", spct.getGiaSauKhuyenMai() != null ? spct.getGiaSauKhuyenMai() : spct.getGiaBan());
            item.put("soLuongTrongGio", ct.getSoLuong());
            item.put("soLuongTonKho", spct.getSoLuong());
            item.put("trangThai", spct.getTrangThai());
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/them")
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Integer> payload) {
        TaiKhoan tk = getLoggedInUser();
        if (tk == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập để thêm vào giỏ hàng"));
        }

        Integer spctId = payload.get("idSanPhamChiTiet");
        Integer soLuongThem = payload.get("soLuong");
        if (soLuongThem == null) soLuongThem = 1;

        if (spctId == null || soLuongThem <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ"));
        }

        SanPhamChiTiet spct = spctRepo.findById(spctId).orElse(null);
        if (spct == null || !spct.getTrangThai()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh"));
        }

        GioHang gh = gioHangRepo.findByTaiKhoan_Id(tk.getId()).orElse(null);
        if (gh == null) {
            gh = new GioHang();
            gh.setTaiKhoan(tk);
            gh = gioHangRepo.save(gh);
        }

        GioHangChiTiet existingItem = gioHangChiTietRepo.findByGioHang_IdAndSanPhamChiTiet_Id(gh.getId(), spctId);

        if (existingItem != null) {
            int newQty = existingItem.getSoLuong() + soLuongThem;
            if (newQty > spct.getSoLuong()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số lượng vượt quá tồn kho hiện tại (" + spct.getSoLuong() + ")"));
            }
            existingItem.setSoLuong(newQty);
            existingItem.setNgayThem(LocalDateTime.now());
            gioHangChiTietRepo.save(existingItem);
        } else {
            if (soLuongThem > spct.getSoLuong()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số lượng vượt quá tồn kho hiện tại (" + spct.getSoLuong() + ")"));
            }
            GioHangChiTiet newItem = new GioHangChiTiet();
            newItem.setGioHang(gh);
            newItem.setSanPhamChiTiet(spct);
            newItem.setSoLuong(soLuongThem);
            newItem.setNgayThem(LocalDateTime.now());
            gioHangChiTietRepo.save(newItem);
        }

        gh.setCapNhatCuoi(LocalDateTime.now());
        gioHangRepo.save(gh);

        return ResponseEntity.ok(Map.of("message", "Đã thêm sản phẩm vào giỏ hàng!"));
    }

    @PutMapping("/cap-nhat")
    public ResponseEntity<?> updateCartItem(@RequestBody Map<String, Integer> payload) {
        TaiKhoan tk = getLoggedInUser();
        if (tk == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Integer idGioHangChiTiet = payload.get("idGioHangChiTiet");
        Integer soLuongMoi = payload.get("soLuong");

        if (idGioHangChiTiet == null || soLuongMoi == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ"));
        }

        GioHangChiTiet item = gioHangChiTietRepo.findById(idGioHangChiTiet).orElse(null);
        if (item == null || !item.getGioHang().getTaiKhoan().getId().equals(tk.getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy sản phẩm trong giỏ"));
        }

        if (soLuongMoi <= 0) {
            gioHangChiTietRepo.delete(item);
            return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm khỏi giỏ hàng"));
        }

        if (soLuongMoi > item.getSanPhamChiTiet().getSoLuong()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số lượng vượt quá tồn kho"));
        }

        item.setSoLuong(soLuongMoi);
        gioHangChiTietRepo.save(item);

        GioHang gh = item.getGioHang();
        gh.setCapNhatCuoi(LocalDateTime.now());
        gioHangRepo.save(gh);

        return ResponseEntity.ok(Map.of("message", "Cập nhật số lượng thành công"));
    }

    @DeleteMapping("/xoa/{idGioHangChiTiet}")
    public ResponseEntity<?> deleteCartItem(@PathVariable("idGioHangChiTiet") Integer idGioHangChiTiet) {
        TaiKhoan tk = getLoggedInUser();
        if (tk == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        GioHangChiTiet item = gioHangChiTietRepo.findById(idGioHangChiTiet).orElse(null);
        if (item != null && item.getGioHang().getTaiKhoan().getId().equals(tk.getId())) {
            gioHangChiTietRepo.delete(item);

            GioHang gh = item.getGioHang();
            gh.setCapNhatCuoi(LocalDateTime.now());
            gioHangRepo.save(gh);
        }

        return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm khỏi giỏ hàng"));
    }

    @DeleteMapping("/xoa-tat-ca")
    public ResponseEntity<?> clearCart() {
        TaiKhoan tk = getLoggedInUser();
        if (tk == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        GioHang gh = gioHangRepo.findByTaiKhoan_Id(tk.getId()).orElse(null);
        if (gh != null) {
            List<GioHangChiTiet> chiTiets = gioHangChiTietRepo.findByGioHang_Id(gh.getId());
            gioHangChiTietRepo.deleteAll(chiTiets);

            gh.setCapNhatCuoi(LocalDateTime.now());
            gioHangRepo.save(gh);
        }

        return ResponseEntity.ok(Map.of("message", "Đã dọn sạch giỏ hàng"));
    }
}