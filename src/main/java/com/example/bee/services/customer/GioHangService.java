package com.example.bee.services;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GioHangService {

    private final GioHangRepository gioHangRepo;
    private final GioHangChiTietRepository gioHangChiTietRepo;
    private final SanPhamChiTietRepository sanPhamChiTietRepo;
    private final TaiKhoanRepository taiKhoanRepo;

    private TaiKhoan layTaiKhoanDangNhap() {
        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        if (xacThuc == null || !xacThuc.isAuthenticated() || "anonymousUser".equals(xacThuc.getName())) {
            return null;
        }
        return taiKhoanRepo.findByTenDangNhap(xacThuc.getName()).orElse(null);
    }

    public ResponseEntity<?> layGioHangCuaToi() {
        TaiKhoan taiKhoan = layTaiKhoanDangNhap();
        if (taiKhoan == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập"));
        }

        GioHang gioHang = gioHangRepo.findByTaiKhoan_Id(taiKhoan.getId()).orElse(null);
        if (gioHang == null) {
            gioHang = new GioHang();
            gioHang.setTaiKhoan(taiKhoan);
            gioHang = gioHangRepo.save(gioHang);
        }

        List<GioHangChiTiet> danhSachChiTiet = gioHangChiTietRepo.findByGioHang_Id(gioHang.getId());

        List<Map<String, Object>> ketQua = new ArrayList<>();
        for (GioHangChiTiet chiTiet : danhSachChiTiet) {
            SanPhamChiTiet sanPhamChiTiet = chiTiet.getSanPhamChiTiet();
            if (sanPhamChiTiet == null) continue;

            Map<String, Object> chiTietGioHang = new HashMap<>();
            chiTietGioHang.put("id", chiTiet.getId());
            chiTietGioHang.put("idSanPhamChiTiet", sanPhamChiTiet.getId());

            String tenSanPham = sanPhamChiTiet.getSanPham() != null ? sanPhamChiTiet.getSanPham().getTen() : "Sản phẩm";
            Integer idSanPham = sanPhamChiTiet.getSanPham() != null ? sanPhamChiTiet.getSanPham().getId() : 0;
            String tenMauSac = sanPhamChiTiet.getMauSac() != null ? sanPhamChiTiet.getMauSac().getTen() : "";
            String tenKichThuoc = sanPhamChiTiet.getKichThuoc() != null ? sanPhamChiTiet.getKichThuoc().getTen() : "";

            chiTietGioHang.put("idSanPham", idSanPham);
            chiTietGioHang.put("tenSanPham", tenSanPham);
            chiTietGioHang.put("thuocTinh", tenMauSac + " - " + tenKichThuoc);
            chiTietGioHang.put("hinhAnh", sanPhamChiTiet.getHinhAnh());
            chiTietGioHang.put("giaBan", sanPhamChiTiet.getGiaBan());
            chiTietGioHang.put("giaSauKhuyenMai", sanPhamChiTiet.getGiaSauKhuyenMai() != null ? sanPhamChiTiet.getGiaSauKhuyenMai() : sanPhamChiTiet.getGiaBan());
            chiTietGioHang.put("soLuongTrongGio", chiTiet.getSoLuong());
            chiTietGioHang.put("soLuongTonKho", sanPhamChiTiet.getSoLuong());
            chiTietGioHang.put("trangThai", sanPhamChiTiet.getTrangThai());

            ketQua.add(chiTietGioHang);
        }

        return ResponseEntity.ok(ketQua);
    }

    @Transactional
    public ResponseEntity<?> themVaoGio(Map<String, Integer> duLieu) {
        TaiKhoan taiKhoan = layTaiKhoanDangNhap();
        if (taiKhoan == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập để thêm vào giỏ hàng"));
        }

        Integer idSanPhamChiTiet = duLieu.get("idSanPhamChiTiet");
        Integer soLuongThem = duLieu.get("soLuong");
        if (soLuongThem == null) soLuongThem = 1;

        if (idSanPhamChiTiet == null || soLuongThem <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ"));
        }

        SanPhamChiTiet sanPhamChiTiet = sanPhamChiTietRepo.findById(idSanPhamChiTiet).orElse(null);
        if (sanPhamChiTiet == null || !sanPhamChiTiet.getTrangThai()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh"));
        }

        GioHang gioHang = gioHangRepo.findByTaiKhoan_Id(taiKhoan.getId()).orElse(null);
        if (gioHang == null) {
            gioHang = new GioHang();
            gioHang.setTaiKhoan(taiKhoan);
            gioHang = gioHangRepo.save(gioHang);
        }

        GioHangChiTiet chiTietTonTai = gioHangChiTietRepo.findByGioHang_IdAndSanPhamChiTiet_Id(gioHang.getId(), idSanPhamChiTiet);

        if (chiTietTonTai != null) {
            int soLuongMoi = chiTietTonTai.getSoLuong() + soLuongThem;
            if (soLuongMoi > sanPhamChiTiet.getSoLuong()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số lượng vượt quá tồn kho hiện tại (" + sanPhamChiTiet.getSoLuong() + ")"));
            }
            chiTietTonTai.setSoLuong(soLuongMoi);
            chiTietTonTai.setNgayThem(LocalDateTime.now());
            gioHangChiTietRepo.save(chiTietTonTai);
        } else {
            if (soLuongThem > sanPhamChiTiet.getSoLuong()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số lượng vượt quá tồn kho hiện tại (" + sanPhamChiTiet.getSoLuong() + ")"));
            }
            GioHangChiTiet chiTietMoi = new GioHangChiTiet();
            chiTietMoi.setGioHang(gioHang);
            chiTietMoi.setSanPhamChiTiet(sanPhamChiTiet);
            chiTietMoi.setSoLuong(soLuongThem);
            chiTietMoi.setNgayThem(LocalDateTime.now());
            gioHangChiTietRepo.save(chiTietMoi);
        }

        gioHang.setCapNhatCuoi(LocalDateTime.now());
        gioHangRepo.save(gioHang);

        return ResponseEntity.ok(Map.of("message", "Đã thêm sản phẩm vào giỏ hàng!"));
    }

    @Transactional
    public ResponseEntity<?> capNhatSoLuong(Map<String, Integer> duLieu) {
        TaiKhoan taiKhoan = layTaiKhoanDangNhap();
        if (taiKhoan == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Integer idGioHangChiTiet = duLieu.get("idGioHangChiTiet");
        Integer soLuongMoi = duLieu.get("soLuong");

        if (idGioHangChiTiet == null || soLuongMoi == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ"));
        }

        GioHangChiTiet chiTiet = gioHangChiTietRepo.findById(idGioHangChiTiet).orElse(null);
        if (chiTiet == null || !chiTiet.getGioHang().getTaiKhoan().getId().equals(taiKhoan.getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy sản phẩm trong giỏ"));
        }

        if (soLuongMoi <= 0) {
            gioHangChiTietRepo.delete(chiTiet);
            return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm khỏi giỏ hàng"));
        }

        if (soLuongMoi > chiTiet.getSanPhamChiTiet().getSoLuong()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số lượng vượt quá tồn kho"));
        }

        chiTiet.setSoLuong(soLuongMoi);
        gioHangChiTietRepo.save(chiTiet);

        GioHang gioHang = chiTiet.getGioHang();
        gioHang.setCapNhatCuoi(LocalDateTime.now());
        gioHangRepo.save(gioHang);

        return ResponseEntity.ok(Map.of("message", "Cập nhật số lượng thành công"));
    }

    @Transactional
    public ResponseEntity<?> xoaKhoiGio(Integer idGioHangChiTiet) {
        TaiKhoan taiKhoan = layTaiKhoanDangNhap();
        if (taiKhoan == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        GioHangChiTiet chiTiet = gioHangChiTietRepo.findById(idGioHangChiTiet).orElse(null);
        if (chiTiet != null && chiTiet.getGioHang().getTaiKhoan().getId().equals(taiKhoan.getId())) {
            gioHangChiTietRepo.delete(chiTiet);

            GioHang gioHang = chiTiet.getGioHang();
            gioHang.setCapNhatCuoi(LocalDateTime.now());
            gioHangRepo.save(gioHang);
        }

        return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm khỏi giỏ hàng"));
    }

    @Transactional
    public ResponseEntity<?> xoaTatCa() {
        TaiKhoan taiKhoan = layTaiKhoanDangNhap();
        if (taiKhoan == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        GioHang gioHang = gioHangRepo.findByTaiKhoan_Id(taiKhoan.getId()).orElse(null);
        if (gioHang != null) {
            List<GioHangChiTiet> danhSachChiTiet = gioHangChiTietRepo.findByGioHang_Id(gioHang.getId());
            gioHangChiTietRepo.deleteAll(danhSachChiTiet);

            gioHang.setCapNhatCuoi(LocalDateTime.now());
            gioHangRepo.save(gioHang);
        }

        return ResponseEntity.ok(Map.of("message", "Đã dọn sạch giỏ hàng"));
    }
}