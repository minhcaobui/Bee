package com.example.bee.controllers.api.order;

import com.example.bee.entities.order.ChiTietDoiTra;
import com.example.bee.entities.order.HoaDon;
import com.example.bee.entities.order.HoaDonChiTiet;
import com.example.bee.entities.order.LichSuHoaDon;
import com.example.bee.entities.order.TrangThaiHoaDon;
import com.example.bee.entities.order.YeuCauDoiTra;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.user.NhanVien;
import com.example.bee.repositories.order.ChiTietDoiTraRepository;
import com.example.bee.repositories.order.HoaDonChiTietRepository;
import com.example.bee.repositories.order.HoaDonRepository;
import com.example.bee.repositories.order.LichSuHoaDonRepository;
import com.example.bee.repositories.order.TrangThaiHoaDonRepository;
import com.example.bee.repositories.order.YeuCauDoiTraRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.role.NhanVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doi-tra")
@RequiredArgsConstructor
public class ReturnApi {

    private final YeuCauDoiTraRepository ycRepo;
    private final ChiTietDoiTraRepository ctRepo;
    private final SanPhamChiTietRepository spctRepo;
    private final NhanVienRepository nvRepo;

    private final HoaDonRepository hdRepo;
    private final HoaDonChiTietRepository hdctRepo;

    // 🌟 Thêm 2 Repo này để cập nhật trạng thái hóa đơn gốc
    private final TrangThaiHoaDonRepository ttRepo;
    private final LichSuHoaDonRepository lsRepo;

    @GetMapping("/list")
    public ResponseEntity<?> getList() {
        List<YeuCauDoiTra> list = ycRepo.findAllByOrderByNgayTaoDesc();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        List<Map<String, Object>> result = list.stream().map(yc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", yc.getId());
            map.put("maYC", yc.getMa());
            map.put("maHD", yc.getHoaDon().getMa());
            map.put("khachHang", yc.getKhachHang() != null ? yc.getKhachHang().getHoTen() : (yc.getHoaDon().getTenNguoiNhan() != null ? yc.getHoaDon().getTenNguoiNhan() : "Khách vãng lai"));
            map.put("sdt", yc.getKhachHang() != null ? yc.getKhachHang().getSoDienThoai() : yc.getHoaDon().getSdtNhan());
            map.put("loai", yc.getLoaiYeuCau());
            map.put("payment", yc.getHoaDon().getPhuongThucThanhToan() != null ? yc.getHoaDon().getPhuongThucThanhToan() : "TIEN_MAT");
            map.put("tienHoan", yc.getSoTienHoan());
            map.put("trangThai", yc.getTrangThai());
            map.put("ngayTao", yc.getNgayTao() != null ? sdf.format(yc.getNgayTao()) : "");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Integer id) {
        YeuCauDoiTra yc = ycRepo.findById(id).orElse(null);
        if (yc == null) return ResponseEntity.notFound().build();

        List<ChiTietDoiTra> ctList = ctRepo.findByYeuCauDoiTraId(id);

        List<Map<String, Object>> items = ctList.stream().map(ct -> {
            Map<String, Object> item = new HashMap<>();
            SanPhamChiTiet spct = ct.getHoaDonChiTiet().getSanPhamChiTiet();
            item.put("tenSP", spct.getSanPham().getTen());
            item.put("thuocTinh", spct.getMauSac().getTen() + " - " + spct.getKichThuoc().getTen());
            item.put("hinhAnh", spct.getHinhAnh());
            item.put("soLuong", ct.getSoLuong());
            item.put("giaBan", ct.getHoaDonChiTiet().getGiaTien());
            item.put("sku", spct.getSku());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("maYC", yc.getMa());
        result.put("maHD", yc.getHoaDon().getMa());
        result.put("khachHang", yc.getKhachHang() != null ? yc.getKhachHang().getHoTen() : (yc.getHoaDon().getTenNguoiNhan() != null ? yc.getHoaDon().getTenNguoiNhan() : "Khách vãng lai"));
        result.put("sdt", yc.getKhachHang() != null ? yc.getKhachHang().getSoDienThoai() : yc.getHoaDon().getSdtNhan());
        result.put("payment", yc.getHoaDon().getPhuongThucThanhToan() != null ? yc.getHoaDon().getPhuongThucThanhToan() : "TIEN_MAT");
        result.put("tienHoan", yc.getSoTienHoan());
        result.put("lyDo", yc.getLyDo());
        result.put("loai", yc.getLoaiYeuCau());
        result.put("chiTiets", items);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable Integer id, @RequestBody(required = false) Map<String, Object> payload) {
        YeuCauDoiTra yc = ycRepo.findById(id).orElse(null);
        if (yc == null || !yc.getTrangThai().equals("CHO_XU_LY")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Yêu cầu không tồn tại hoặc đã được xử lý!"));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            NhanVien nv = nvRepo.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null);
            yc.setNhanVien(nv);
        }

        // 🌟 LẤY TRẠNG THÁI CHECKBOX CỘNG KHO TỪ FRONTEND
        boolean congKho = payload != null && payload.containsKey("congKho") ? (Boolean) payload.get("congKho") : true;

        if (yc.getLoaiYeuCau().equalsIgnoreCase("ĐỔI HÀNG") && payload != null && payload.containsKey("chiTietMoi")) {
            List<Map<String, Object>> chiTietMoi = (List<Map<String, Object>>) payload.get("chiTietMoi");
            for (Map<String, Object> itemMoi : chiTietMoi) {
                Integer spctId = Integer.parseInt(itemMoi.get("idSanPhamChiTiet").toString());
                Integer slMoi = Integer.parseInt(itemMoi.get("soLuong").toString());

                SanPhamChiTiet spctMoi = spctRepo.findById(spctId).orElse(null);
                if (spctMoi != null) {
                    if (spctMoi.getSoLuong() < slMoi) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Sản phẩm " + spctMoi.getSanPham().getTen() + " không đủ tồn kho để đổi!"));
                    }
                    spctMoi.setSoLuong(spctMoi.getSoLuong() - slMoi);
                    spctRepo.save(spctMoi);
                }
            }
        }

        yc.setTrangThai("HOAN_THANH");
        yc.setNgayXuLy(new Date());
        ycRepo.save(yc);

        // 🌟 FIX LỖI 3 & 4: TĂNG SỐ LƯỢNG TRẢ CỦA HÓA ĐƠN CHI TIẾT (KHÔNG ĐỔI TRẠNG THÁI HÓA ĐƠN MẸ)
        List<ChiTietDoiTra> ctList = ctRepo.findByYeuCauDoiTraId(id);
        for (ChiTietDoiTra ct : ctList) {
            HoaDonChiTiet hdct = ct.getHoaDonChiTiet();

            // Ghi nhận số lượng khách đã trả vào hóa đơn chi tiết gốc
            int daTra = hdct.getSoLuongTra() != null ? hdct.getSoLuongTra() : 0;
            hdct.setSoLuongTra(daTra + ct.getSoLuong());
            hdctRepo.save(hdct);

            // Chỉ cộng lại kho nếu nhân viên tích chọn "Hàng còn nguyên vẹn"
            if (congKho) {
                SanPhamChiTiet spct = hdct.getSanPhamChiTiet();
                spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong());
                spctRepo.save(spct);
            }
        }

        // 🌟 KHÔNG SET LẠI TRẠNG THÁI hd.setTrangThaiHoaDon() NỮA, CHỈ LƯU LỊCH SỬ
        HoaDon hd = yc.getHoaDon();
        if (hd != null) {
            LichSuHoaDon ls = new LichSuHoaDon();
            ls.setHoaDon(hd);
            ls.setTrangThaiHoaDon(hd.getTrangThaiHoaDon()); // Giữ nguyên trạng thái cũ (HOAN_THANH)
            ls.setNhanVien(yc.getNhanVien());
            ls.setGhiChu("Đơn hàng cập nhật số lượng trả do hoàn tất phiếu " + yc.getLoaiYeuCau() + " hàng: " + yc.getMa());
            ls.setNgayTao(new Date());
            lsRepo.save(ls);
        }

        return ResponseEntity.ok(Map.of("message", "Xử lý thành công!"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Integer id) {
        YeuCauDoiTra yc = ycRepo.findById(id).orElse(null);
        if (yc == null) return ResponseEntity.badRequest().build();
        yc.setTrangThai("TU_CHOI");
        yc.setNgayXuLy(new Date());
        ycRepo.save(yc);
        return ResponseEntity.ok(Map.of("message", "Đã từ chối"));
    }

    @GetMapping("/search-order/{ma}")
    public ResponseEntity<?> searchOrderForReturn(@PathVariable String ma) {
        HoaDon hd = hdRepo.findByMa(ma.trim());
        if (hd == null) return ResponseEntity.notFound().build();

        Map<String, Object> res = new HashMap<>();
        res.put("ma", hd.getMa());
        res.put("khachHang", hd.getKhachHang() != null ? hd.getKhachHang().getHoTen() : (hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "Khách vãng lai"));
        res.put("trangThaiMa", hd.getTrangThaiHoaDon().getMa());
        res.put("trangThaiTen", hd.getTrangThaiHoaDon().getTen());

        List<HoaDonChiTiet> ctList = hdctRepo.findByHoaDonId(hd.getId());
        List<Map<String, Object>> items = new ArrayList<>();
        for (HoaDonChiTiet ct : ctList) {
            Map<String, Object> item = new HashMap<>();
            item.put("idHdct", ct.getId()); // Cần ID này để map vào bảng ChiTietDoiTra
            item.put("tenSP", ct.getSanPhamChiTiet().getSanPham().getTen());
            item.put("thuocTinh", ct.getSanPhamChiTiet().getMauSac().getTen() + " - " + ct.getSanPhamChiTiet().getKichThuoc().getTen());
            item.put("hinhAnh", ct.getSanPhamChiTiet().getHinhAnh());
            item.put("soLuong", ct.getSoLuong()); // Số lượng tối đa có thể trả
            item.put("giaBan", ct.getGiaTien());
            items.add(item);
        }
        res.put("chiTiets", items);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createRequest(@RequestBody Map<String, Object> payload) {
        try {
            String maHD = (String) payload.get("maHD");
            String loaiYeuCau = (String) payload.get("loaiYeuCau");
            String lyDo = (String) payload.get("lyDo");
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

            HoaDon hd = hdRepo.findByMa(maHD);
            if (hd == null) return ResponseEntity.badRequest().body(Map.of("message", "Hóa đơn không tồn tại"));
            if (!"HOAN_THANH".equals(hd.getTrangThaiHoaDon().getMa())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Chỉ được đổi trả hóa đơn đã giao thành công!"));
            }

            // 🌟 TÍNH TỔNG TIỀN HÀNG VÀ VOUCHER CỦA ĐƠN ĐỂ PHÂN BỔ TỈ LỆ
            BigDecimal tongTienDon = BigDecimal.ZERO;
            List<HoaDonChiTiet> allHdct = hdctRepo.findByHoaDonId(hd.getId());
            for (HoaDonChiTiet ct : allHdct) {
                tongTienDon = tongTienDon.add(ct.getGiaTien().multiply(BigDecimal.valueOf(ct.getSoLuong())));
            }

            // 💡 LƯU Ý: Nếu Entity HoaDon của bạn dùng tên khác (VD: getTienGiamVoucher, getGiaTriKhuyenMai), hãy sửa tên 'getTienGiam()' ở dòng dưới cho khớp nhé.
            // Dòng code đã sửa đúng theo Entity HoaDon của bạn:
            BigDecimal tienGiam = hd.getGiaTriKhuyenMai() != null ? hd.getGiaTriKhuyenMai() : BigDecimal.ZERO;

            YeuCauDoiTra yc = new YeuCauDoiTra();
            yc.setMa("DT" + System.currentTimeMillis());
            yc.setHoaDon(hd);
            yc.setKhachHang(hd.getKhachHang());
            yc.setLoaiYeuCau(loaiYeuCau);
            yc.setLyDo(lyDo);
            yc.setTrangThai("CHO_XU_LY");
            yc.setNgayTao(new Date());

            BigDecimal tienHoan = BigDecimal.ZERO;
            yc = ycRepo.save(yc);

            for (Map<String, Object> item : items) {
                Integer idHdct = Integer.parseInt(item.get("idHoaDonChiTiet").toString());
                Integer sl = Integer.parseInt(item.get("soLuong").toString());

                HoaDonChiTiet hdct = hdctRepo.findById(idHdct).orElse(null);
                if (hdct != null) {
                    // 🌟 CHECK VALIDATE SỐ LƯỢNG TRẢ
                    int daTra = hdct.getSoLuongTra() != null ? hdct.getSoLuongTra() : 0;
                    int maxCoTheTra = hdct.getSoLuong() - daTra;

                    if (sl <= 0 || sl > maxCoTheTra) {
                        throw new RuntimeException("Số lượng trả không hợp lệ cho SP: " + hdct.getSanPhamChiTiet().getSanPham().getTen());
                    }
                    ChiTietDoiTra ct = new ChiTietDoiTra();
                    ct.setYeuCauDoiTra(yc);
                    ct.setHoaDonChiTiet(hdct);
                    ct.setSoLuong(sl);
                    ct.setTinhTrangSanPham("Bình thường");
                    ctRepo.save(ct);

                    // 🌟 FIX LỖI 1: TÍNH TIỀN HOÀN CÓ TRỪ ĐI PHẦN TRĂM VOUCHER
                    BigDecimal giaTriSp = hdct.getGiaTien().multiply(BigDecimal.valueOf(sl));
                    if (tongTienDon.compareTo(BigDecimal.ZERO) > 0 && tienGiam.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal tyLeGiam = giaTriSp.divide(tongTienDon, 4, java.math.RoundingMode.HALF_UP);
                        BigDecimal giamGiaCuaSp = tienGiam.multiply(tyLeGiam);
                        tienHoan = tienHoan.add(giaTriSp.subtract(giamGiaCuaSp));
                    } else {
                        tienHoan = tienHoan.add(giaTriSp);
                    }
                }
            }

            yc.setSoTienHoan(tienHoan.setScale(0, java.math.RoundingMode.HALF_UP));
            ycRepo.save(yc);

            return ResponseEntity.ok(Map.of("message", "Tạo yêu cầu thành công!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi dữ liệu đầu vào!"));
        }
    }
}