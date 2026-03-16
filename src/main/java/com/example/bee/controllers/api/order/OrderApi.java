package com.example.bee.controllers.api.order;

import com.example.bee.dto.HoaDonChiTietResponse;
import com.example.bee.dto.HoaDonResponse;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.notification.ThongBao;
import com.example.bee.entities.order.HoaDon;
import com.example.bee.entities.order.HoaDonChiTiet;
import com.example.bee.entities.order.LichSuHoaDon;
import com.example.bee.entities.order.TrangThaiHoaDon;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
import com.example.bee.repositories.order.HoaDonChiTietRepository;
import com.example.bee.repositories.order.HoaDonRepository;
import com.example.bee.repositories.order.LichSuHoaDonRepository;
import com.example.bee.repositories.order.TrangThaiHoaDonRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.role.NhanVienRepository;
import com.example.bee.services.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/hoa-don")
@RequiredArgsConstructor
public class OrderApi {

    private final HoaDonRepository hdRepo;
    private final TrangThaiHoaDonRepository ttRepo;
    private final LichSuHoaDonRepository lsRepo;
    private final HoaDonChiTietRepository hdctRepo;
    private final NhanVienRepository nvRepo;
    private final SanPhamChiTietRepository spctRepo;
    private final KhachHangRepository khRepo;
    private final EmailService emailService;
    private final ThongBaoRepository thongBaoRepository;

    @GetMapping("/don-hang")
    public ResponseEntity<?> getDonHangChoXuLy(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) Integer loaiHoaDon,
            @RequestParam(required = false) String phuongThucThanhToan,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        q = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        phuongThucThanhToan = (phuongThucThanhToan != null && !phuongThucThanhToan.trim().isEmpty()) ? phuongThucThanhToan : null;

        if (endDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            endDate = cal.getTime();
        }

        Page<HoaDon> result = hdRepo.searchDonHangChoXuLy(q, statusId, loaiHoaDon, phuongThucThanhToan, startDate, endDate, pageable);
        return ResponseEntity.ok(result);
    }

    // =======================================================
    // 2. API LẤY LỊCH SỬ HÓA ĐƠN (Chỉ lấy Hoàn thành & Đã hủy)
    // =======================================================
    @GetMapping("/lich-su")
    public ResponseEntity<?> getLichSu(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) Integer nhanVienId,
            @RequestParam(required = false) Integer loaiHoaDon,
            @RequestParam(required = false) String phuongThucThanhToan,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        q = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        phuongThucThanhToan = (phuongThucThanhToan != null && !phuongThucThanhToan.trim().isEmpty()) ? phuongThucThanhToan : null;

        if (endDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            endDate = cal.getTime();
        }

        Page<HoaDon> result = hdRepo.searchLichSuHoaDon(q, statusId, nhanVienId, loaiHoaDon, phuongThucThanhToan, startDate, endDate, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HoaDonResponse> getDetail(@PathVariable Integer id) {
        HoaDon hd = hdRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<HoaDonChiTiet> listHdct = hdctRepo.findByHoaDonId(id);
        BigDecimal tongTienHangThucTe = BigDecimal.ZERO;
        List<HoaDonChiTietResponse> chiTietResponses = new ArrayList<>();
        for (HoaDonChiTiet ct : listHdct) {
            BigDecimal giaBan = ct.getGiaTien();
            Integer soLuong = ct.getSoLuong();
            tongTienHangThucTe = tongTienHangThucTe.add(giaBan.multiply(BigDecimal.valueOf(soLuong)));
            chiTietResponses.add(HoaDonChiTietResponse.builder()
                    .id(ct.getId())
                    .tenSanPham(ct.getSanPhamChiTiet().getSanPham().getTen())
                    .sku(ct.getSanPhamChiTiet().getSku())
                    .thuocTinh("Size " + ct.getSanPhamChiTiet().getKichThuoc().getTen() + " - " + ct.getSanPhamChiTiet().getMauSac().getTen())
                    .hinhAnh(ct.getSanPhamChiTiet().getHinhAnh())
                    .soLuong(soLuong)
                    .donGia(ct.getSanPhamChiTiet().getGiaBan())
                    .giaBan(giaBan)
                    .build());
        }
        BigDecimal phiShip = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal tongPhaiTra = hd.getGiaTong() != null ? hd.getGiaTong() : BigDecimal.ZERO;
        BigDecimal voucherCalculated = tongTienHangThucTe.add(phiShip).subtract(tongPhaiTra);
        if (voucherCalculated.compareTo(BigDecimal.ZERO) < 0) voucherCalculated = BigDecimal.ZERO;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String tenNhanVienHienTai = "Hệ thống";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            var nhanVienDangNhap = nvRepo.findByTaiKhoan_TenDangNhap(username).orElse(null);
            if (nhanVienDangNhap != null) {
                tenNhanVienHienTai = nhanVienDangNhap.getHoTen();
            }
        }
        HoaDonResponse response = HoaDonResponse.builder()
                .id(hd.getId())
                .ma(hd.getMa())
                .tenKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getHoTen() : "Khách vãng lai")
                .tenNhanVien(hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : tenNhanVienHienTai)
                .sdtNhan(hd.getSdtNhan())
                .diaChiGiaoHang(hd.getDiaChiGiaoHang())
                .phuongThucThanhToan(hd.getPhuongThucThanhToan())
                .trangThaiMa(hd.getTrangThaiHoaDon().getMa())
                .trangThaiTen(hd.getTrangThaiHoaDon().getTen())
                .loaiHoaDon(hd.getLoaiHoaDon())
                .ngayTao(hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : null)
                .tienHang(tongTienHangThucTe)
                .phiVanChuyen(phiShip)
                .tienGiamVoucher(voucherCalculated)
                .tienGiamSale(BigDecimal.ZERO)
                .tongTien(tongPhaiTra)
                .chiTiets(chiTietResponses)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/next-status")
    @Transactional
    public ResponseEntity<?> nextStatus(@PathVariable Integer id, @RequestBody Map<String, String> req) {
        HoaDon hd = hdRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));
        String currentMa = hd.getTrangThaiHoaDon().getMa();
        String nextMa;
        String ghiChu = req.getOrDefault("ghiChu", "Cập nhật trạng thái tự động");
        switch (currentMa) {
            case "CHO_XAC_NHAN" -> nextMa = "CHO_GIAO";
            case "CHO_GIAO" -> nextMa = "DANG_GIAO";
            case "DANG_GIAO" -> {
                nextMa = "HOAN_THANH";
                hd.setNgayThanhToan(new Date());
            }
            default ->
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái hiện tại không thể chuyển tiếp");
        }
        TrangThaiHoaDon nextStatus = ttRepo.findByMa(nextMa);
        hd.setTrangThaiHoaDon(nextStatus);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                ? nvRepo.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null)
                : null;
        if (hd.getNhanVien() == null && nhanVienThaoTac != null) {
            hd.setNhanVien(nhanVienThaoTac);
        }
        hdRepo.save(hd);
        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            ThongBao tb = new ThongBao();
            tb.setTaiKhoanId(hd.getKhachHang().getTaiKhoan().getId());
            tb.setLoaiThongBao("ORDER");
            tb.setDaDoc(false);
            if ("CHO_GIAO".equals(nextMa)) {
                tb.setTieuDe("Đơn hàng đã được xác nhận");
                tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã đóng gói xong và đang chờ giao cho đơn vị vận chuyển.");
                thongBaoRepository.save(tb);
            } else if ("DANG_GIAO".equals(nextMa)) {
                tb.setTieuDe("Đơn hàng đang trên đường giao");
                tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đang được vận chuyển đến bạn. Vui lòng chú ý điện thoại nhé!");
                thongBaoRepository.save(tb);
            } else if ("HOAN_THANH".equals(nextMa)) {
                tb.setTieuDe("Giao hàng thành công");
                tb.setNoiDung("Tuyệt vời! Đơn hàng #" + hd.getMa() + " đã được giao thành công. Cảm ơn bạn đã mua sắm tại Beemate.");
                thongBaoRepository.save(tb);
            }
        }
        lsRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(nextStatus)
                .nhanVien(nhanVienThaoTac)
                .ghiChu(ghiChu)
                .build());
        return ResponseEntity.ok(Map.of(
                "message", "Cập nhật thành công",
                "nextStatus", nextStatus.getTen(),
                "nextStatusMa", nextStatus.getMa()
        ));
    }

    @PatchMapping("/{id}/huy")
    @Transactional
    public ResponseEntity<?> huyDon(@PathVariable Integer id, @RequestBody Map<String, String> req) {
        HoaDon hd = hdRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));
        TrangThaiHoaDon ttHuy = ttRepo.findByMa("DA_HUY");
        hd.setTrangThaiHoaDon(ttHuy);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                ? nvRepo.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null)
                : null;
        if (hd.getNhanVien() == null && nhanVienThaoTac != null) {
            hd.setNhanVien(nhanVienThaoTac);
        }
        hdRepo.save(hd);
        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            ThongBao tb = new ThongBao();
            tb.setTaiKhoanId(hd.getKhachHang().getTaiKhoan().getId());
            tb.setLoaiThongBao("ORDER");
            tb.setDaDoc(false);
            tb.setTieuDe("Đơn hàng đã bị hủy");
            tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã bị hủy. Lý do: " + req.getOrDefault("ghiChu", "Khách hàng / Admin yêu cầu hủy đơn."));
            thongBaoRepository.save(tb);
        }
        lsRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(ttHuy)
                .nhanVien(nhanVienThaoTac)
                .ghiChu(req.getOrDefault("ghiChu", "Khách hàng/Admin yêu cầu hủy đơn"))
                .build());
        return ResponseEntity.ok(Map.of("message", "Đơn hàng đã được chuyển sang trạng thái hủy"));
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest req) {
        try {
            // 1. Tạo mới đối tượng Hóa Đơn
            HoaDon hd = new HoaDon();
            hd.setMa("HD" + System.currentTimeMillis());
            hd.setLoaiHoaDon(1); // 1 = Đơn Online
            hd.setNgayTao(new Date());

            // Gán thông tin giao hàng
            hd.setTenNguoiNhan(req.tenNguoiNhan);
            hd.setSdtNhan(req.soDienThoai);
            hd.setDiaChiGiaoHang(req.diaChiGiaoHang);
            hd.setPhuongThucThanhToan(req.phuongThucThanhToan);

            // 2. Xử lý khách hàng (Nếu có đăng nhập)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                String username = auth.getName();
                KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(username).orElse(null);
                hd.setKhachHang(kh);
            } else {
                hd.setKhachHang(null); // Khách vãng lai (Guest)
            }

            // =================================================================
            // 🌟 LOGIC TỰ ĐỘNG GIẢM GIÁ 5% CHO NHÂN VIÊN MUA ONLINE 🌟
            // =================================================================
            BigDecimal chietKhauNV = BigDecimal.ZERO;
            String ghiChu = req.ghiChu != null ? req.ghiChu : "";

            if (req.soDienThoai != null && !req.soDienThoai.isBlank()) {
                // Quét SĐT người đặt xem có nằm trong bảng nhân viên không
                boolean isEmployee = nvRepo.existsBySoDienThoaiAndTrangThaiTrue(req.soDienThoai);
                if (isEmployee) {
                    // Tính 5% từ tổng tiền hàng (tienHang) gửi lên từ Frontend
                    chietKhauNV = req.tienHang.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
                    ghiChu += " (Tự động giảm 5% ưu đãi nhân viên nội bộ)";
                }
            }
            hd.setGhiChu(ghiChu);

            // 3. Tính toán lại tổng tiền bảo mật ở Backend
            BigDecimal tongTienCuoi = req.tienHang
                    .subtract(req.tienGiam != null ? req.tienGiam : BigDecimal.ZERO) // Trừ voucher thường
                    .subtract(chietKhauNV) // Trừ thêm 5% nhân viên
                    .add(req.phiShip != null ? req.phiShip : BigDecimal.ZERO); // Cộng phí ship

            if (tongTienCuoi.compareTo(BigDecimal.ZERO) < 0) {
                tongTienCuoi = BigDecimal.ZERO;
            }

            // Gán tiền bạc chuẩn xác vào Hóa Đơn
            hd.setGiaTamThoi(req.tienHang);
            hd.setGiaTriKhuyenMai((req.tienGiam != null ? req.tienGiam : BigDecimal.ZERO).add(chietKhauNV));
            hd.setPhiVanChuyen(req.phiShip);
            hd.setGiaTong(tongTienCuoi); // Lưu tổng tiền đã trừ 5%

            // 4. Gán trạng thái Mặc định
            TrangThaiHoaDon ttChoXacNhan = ttRepo.findByMa("CHO_XAC_NHAN");
            if (ttChoXacNhan == null) throw new RuntimeException("Không tìm thấy trạng thái CHO_XAC_NHAN");
            hd.setTrangThaiHoaDon(ttChoXacNhan);

            // 5. LƯU HÓA ĐƠN VÀ CHI TIẾT VÀO DB
            HoaDon savedHd = hdRepo.save(hd);

            for (CheckoutItemRequest itemReq : req.chiTietDonHangs) {
                SanPhamChiTiet spct = spctRepo.findById(itemReq.chiTietSanPhamId)
                        .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

                if (spct.getSoLuong() < itemReq.soLuong) {
                    throw new RuntimeException("Sản phẩm " + spct.getSanPham().getTen() + " không đủ số lượng!");
                }

                spct.setSoLuong(spct.getSoLuong() - itemReq.soLuong);
                spctRepo.save(spct);

                HoaDonChiTiet hdct = new HoaDonChiTiet();
                hdct.setHoaDon(savedHd);
                hdct.setSanPhamChiTiet(spct);
                hdct.setSoLuong(itemReq.soLuong);
                hdct.setGiaTien(itemReq.donGia);
                hdctRepo.save(hdct);
            }

            // Lưu lịch sử hóa đơn
            LichSuHoaDon ls = new LichSuHoaDon();
            ls.setHoaDon(savedHd);
            ls.setTrangThaiHoaDon(ttChoXacNhan);
            ls.setGhiChu("Khách hàng đặt đơn Online trên Website");
            ls.setNgayTao(new Date());
            lsRepo.save(ls);

            // =================================================================
            // 6. THÔNG BÁO & EMAIL
            // =================================================================
            try {
                if (savedHd.getKhachHang() != null && savedHd.getKhachHang().getTaiKhoan() != null) {
                    ThongBao tb = new ThongBao();
                    tb.setTaiKhoanId(savedHd.getKhachHang().getTaiKhoan().getId());
                    tb.setTieuDe("Đặt hàng thành công");
                    tb.setNoiDung("Đơn hàng #" + savedHd.getMa() + " đã được ghi nhận.");
                    tb.setLoaiThongBao("ORDER");
                    tb.setDaDoc(false);
                    thongBaoRepository.save(tb);
                }
            } catch (Exception e) {
                System.err.println("Lỗi lưu thông báo (vẫn tiếp tục): " + e.getMessage());
            }

            try {
                if (req.email != null && !req.email.isBlank()) {
                    emailService.sendOrderConfirmationEmail(savedHd, req.email);
                }
            } catch (Exception e) {
                System.err.println("Lỗi gửi email (vẫn tiếp tục): " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of("message", "Đặt hàng thành công", "maHoaDon", savedHd.getMa(), "tongTienThucTe", tongTienCuoi));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập"));
        }
        String username = auth.getName();
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        List<HoaDon> myOrders = hdRepo.findByKhachHangIdOrderByNgayTaoDesc(kh.getId());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        List<Map<String, Object>> response = new ArrayList<>();
        for (HoaDon hd : myOrders) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", hd.getId());
            map.put("ma", hd.getMa());
            map.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : "");
            map.put("tongTien", hd.getGiaTong());
            map.put("trangThai", hd.getTrangThaiHoaDon().getTen());
            map.put("trangThaiMa", hd.getTrangThaiHoaDon().getMa());
            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tra-cuu/{ma}")
    public ResponseEntity<?> traCuuNhanh(@PathVariable String ma) {
        HoaDon hoaDon = hdRepo.findByMa(ma);
        if (hoaDon == null) {
            return ResponseEntity.notFound().build();
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("ma", hoaDon.getMa());
        result.put("ngayTao", hoaDon.getNgayTao());
        result.put("tenNguoiNhan", hoaDon.getTenNguoiNhan());
        result.put("giaTong", hoaDon.getGiaTong());
        result.put("trangThaiHoaDon", hoaDon.getTrangThaiHoaDon());
        java.util.List<com.example.bee.entities.order.HoaDonChiTiet> danhSachChiTiet = hdctRepo.findByHoaDon(hoaDon);
        java.util.List<java.util.Map<String, Object>> listChiTiet = new java.util.ArrayList<>();
        if (danhSachChiTiet != null && !danhSachChiTiet.isEmpty()) {
            for (com.example.bee.entities.order.HoaDonChiTiet hdct : danhSachChiTiet) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("soLuong", hdct.getSoLuong());
                item.put("giaBan", hdct.getGiaTien());
                if (hdct.getSanPhamChiTiet() != null) {
                    item.put("hinhAnh", hdct.getSanPhamChiTiet().getHinhAnh());
                    if (hdct.getSanPhamChiTiet().getSanPham() != null) {
                        item.put("tenSanPham", hdct.getSanPhamChiTiet().getSanPham().getTen());
                    }
                    if (hdct.getSanPhamChiTiet().getMauSac() != null && hdct.getSanPhamChiTiet().getKichThuoc() != null) {
                        item.put("thuocTinh", hdct.getSanPhamChiTiet().getMauSac().getTen() + " - " + hdct.getSanPhamChiTiet().getKichThuoc().getTen());
                    }
                }
                listChiTiet.add(item);
            }
        }
        result.put("chiTiets", listChiTiet);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/print-data")
    public ResponseEntity<?> getInvoicePrintData(@PathVariable Integer id) {
        HoaDon hd = hdRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));
        List<HoaDonChiTiet> listHdct = hdctRepo.findByHoaDonId(id);
        Map<String, Object> storeInfo = new HashMap<>();
        storeInfo.put("tenCuaHang", "BEEMATE STORE");
        storeInfo.put("diaChi", "13 phố Phan Tây Nhạc, phường Xuân Phương, TP Hà Nội"); // Điền địa chỉ thực tế của cửa hàng bạn
        storeInfo.put("soDienThoai", "1900 3636");
        Map<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("maHoaDon", hd.getMa());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        orderInfo.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : sdf.format(new Date()));
        orderInfo.put("thuNgan", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Hệ thống");
        if (hd.getKhachHang() != null) {
            orderInfo.put("tenKhachHang", hd.getKhachHang().getHoTen());
            orderInfo.put("sdtKhachHang", hd.getKhachHang().getSoDienThoai());
            orderInfo.put("inThongTinTaiKhoan", true);
        } else {
            orderInfo.put("tenKhachHang", hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "Khách vãng lai");
            orderInfo.put("inThongTinTaiKhoan", false);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal tongTienHang = BigDecimal.ZERO;
        for (HoaDonChiTiet ct : listHdct) {
            Map<String, Object> item = new HashMap<>();
            String tenSP = ct.getSanPhamChiTiet().getSanPham().getTen();
            String thuocTinh = ct.getSanPhamChiTiet().getMauSac().getTen() + " - " + ct.getSanPhamChiTiet().getKichThuoc().getTen();
            item.put("ten", tenSP + " (" + thuocTinh + ")");
            item.put("soLuong", ct.getSoLuong());
            item.put("donGia", ct.getGiaTien());
            BigDecimal thanhTien = ct.getGiaTien().multiply(BigDecimal.valueOf(ct.getSoLuong()));
            item.put("thanhTien", thanhTien);
            tongTienHang = tongTienHang.add(thanhTien);
            items.add(item);
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("tongTienHang", tongTienHang);
        BigDecimal phiShip = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        summary.put("phiVanChuyen", phiShip);
        BigDecimal tongPhaiTra = hd.getGiaTong() != null ? hd.getGiaTong() : BigDecimal.ZERO;
        BigDecimal giamGia = tongTienHang.add(phiShip).subtract(tongPhaiTra);
        if (giamGia.compareTo(BigDecimal.ZERO) < 0) giamGia = BigDecimal.ZERO;
        summary.put("giamGia", giamGia);
        summary.put("tongThanhToan", tongPhaiTra);
        summary.put("phuongThuc", hd.getPhuongThucThanhToan());
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("store", storeInfo);
        responseData.put("order", orderInfo);
        responseData.put("items", items);
        responseData.put("summary", summary);
        return ResponseEntity.ok(responseData);
    }

    // =================================================================
    // API KIỂM TRA SĐT NHÂN VIÊN TẠI MÀN HÌNH CHECKOUT ONLINE
    // =================================================================
    @GetMapping("/check-employee")
    public ResponseEntity<?> checkEmployeeDiscount(@RequestParam String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("isEmployee", false));
        }

        // Kiểm tra xem SĐT có tồn tại trong bảng Nhân viên và đang làm việc không
        boolean isEmployee = nvRepo.existsBySoDienThoaiAndTrangThaiTrue(phone.trim());

        return ResponseEntity.ok(Map.of("isEmployee", isEmployee));
    }

    @GetMapping("/thong-bao-moi")
    public ResponseEntity<?> getNewOnlineOrders() {
        // Tìm 5 đơn Online (loaiHoaDon = 1) đang Chờ xác nhận
        List<HoaDon> list = hdRepo.findTop5ByLoaiHoaDonAndTrangThaiHoaDon_MaOrderByNgayTaoDesc(1, "CHO_XAC_NHAN");

        // Trả về DTO rút gọn cho nhẹ
        List<Map<String, Object>> result = new ArrayList<>();
        for (HoaDon hd : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", hd.getId());
            map.put("ma", hd.getMa());
            map.put("khachHang", hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "Khách hàng");
            map.put("tongTien", hd.getGiaTong());
            map.put("ngayTao", hd.getNgayTao());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    public static class CheckoutRequest {
        public String tenNguoiNhan;
        public String soDienThoai;
        public String email;
        public String diaChiGiaoHang;
        public String ghiChu;
        public String phuongThucThanhToan;
        public BigDecimal tienHang;
        public BigDecimal phiShip;
        public BigDecimal tienGiam;
        public BigDecimal tongTien;
        public List<CheckoutItemRequest> chiTietDonHangs;
    }

    public static class CheckoutItemRequest {
        public Integer chiTietSanPhamId;
        public Integer soLuong;
        public BigDecimal donGia;
    }
}