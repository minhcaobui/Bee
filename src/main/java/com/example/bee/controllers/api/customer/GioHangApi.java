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
import org.springframework.transaction.annotation.Isolation;
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
    @Transactional(readOnly = true)
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

            // BẢO MẬT LOGIC KHO: Phải trả về số lượng KHẢ DỤNG cho Frontend
            Integer slKhaDung = Math.max(0, spct.getSoLuong() - (spct.getSoLuongTamGiu() != null ? spct.getSoLuongTamGiu() : 0));
            item.put("soLuongTonKho", slKhaDung);

            item.put("trangThai", spct.getTrangThai());
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    // BẢO MẬT RACE CONDITION: Thêm Isolation.READ_COMMITTED để chặn người khác đọc/lưu sai số lượng khi click liên tục
    @PostMapping("/them")
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

        // BẢO MẬT LOGIC KHO: Tính Số lượng Khả Dụng thực tế
        int slKhaDung = Math.max(0, spct.getSoLuong() - (spct.getSoLuongTamGiu() != null ? spct.getSoLuongTamGiu() : 0));

        if (existingItem != null) {
            // Không tính số lượng đã có trong giỏ hàng vì nó thuộc về giỏ hàng hiện tại, chỉ check khoảng thêm mới
            if (soLuongThem > slKhaDung) {
                return ResponseEntity.badRequest().body(Map.of("message", "Kho không đủ số lượng để thêm!"));
            }
            existingItem.setSoLuong(existingItem.getSoLuong() + soLuongThem);
            existingItem.setNgayThem(LocalDateTime.now());
            gioHangChiTietRepo.save(existingItem);
        } else {
            if (soLuongThem > slKhaDung) {
                return ResponseEntity.badRequest().body(Map.of("message", "Kho không đủ số lượng để thêm!"));
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
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

        SanPhamChiTiet spct = item.getSanPhamChiTiet();
        // BẢO MẬT LOGIC KHO: Cập nhật giỏ hàng cần so với tổng khả dụng nếu tăng số lượng
        int slKhaDung = Math.max(0, spct.getSoLuong() - (spct.getSoLuongTamGiu() != null ? spct.getSoLuongTamGiu() : 0));

        int soChenhLechTangThem = soLuongMoi - item.getSoLuong();

        if (soChenhLechTangThem > 0 && soChenhLechTangThem > slKhaDung) {
            return ResponseEntity.badRequest().body(Map.of("message", "Kho chỉ còn " + slKhaDung + " sản phẩm khả dụng"));
        }

        item.setSoLuong(soLuongMoi);
        gioHangChiTietRepo.save(item);

        GioHang gh = item.getGioHang();
        gh.setCapNhatCuoi(LocalDateTime.now());
        gioHangRepo.save(gh);

        return ResponseEntity.ok(Map.of("message", "Cập nhật số lượng thành công"));
    }

    @DeleteMapping("/xoa/{idGioHangChiTiet}")
    @Transactional
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
    @Transactional
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