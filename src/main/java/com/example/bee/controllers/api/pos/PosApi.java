package com.example.bee.controllers.api.pos;

import com.example.bee.entities.order.*;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.entities.user.NhanVien;
import com.example.bee.repositories.catalog.KichThuocRepository;
import com.example.bee.repositories.catalog.MauSacRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.order.*;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import com.example.bee.repositories.role.NhanVienRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosApi {
    private final SanPhamChiTietRepository variantRepo;
    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository hdctRepo;
    private final KhachHangRepository khachHangRepo;
    private final ThanhToanRepository thanhToanRepo;
    private final LichSuHoaDonRepository lichSuRepo;
    private final TrangThaiHoaDonRepository trangThaiRepo;
    private final MauSacRepository mauSacRepo;
    private final KichThuocRepository kichThuocRepo;
    private final MaGiamGiaRepository maGiamGiaRepo;
    private final KhuyenMaiRepository khuyenMaiRepo;
    private final NhanVienRepository nvRepo;

    // Hàm hỗ trợ tái sử dụng để lấy NhanVien đang đăng nhập
    private NhanVien getLoggedInNhanVien() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            return nvRepo.findByTaiKhoan_TenDangNhap(username).orElse(null);
        }
        return null;
    }

    // 1. TÌM KIẾM SẢN PHẨM
    @GetMapping("/products/search")
    public List<SanPhamChiTiet> searchProducts(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Integer color,
            @RequestParam(required = false) Integer size) {

        List<SanPhamChiTiet> list = variantRepo.findAvailableProducts(q, color, size);

        for (SanPhamChiTiet spct : list) {
            java.math.BigDecimal giaGoc = spct.getGiaBan();
            java.math.BigDecimal giaSauKM = giaGoc;

            List<KhuyenMai> activeSales = khuyenMaiRepo.findActiveKhuyenMaiBySanPhamId(spct.getSanPham().getId());

            if (activeSales != null && !activeSales.isEmpty()) {
                KhuyenMai km = activeSales.get(0);
                if ("PERCENT".equals(km.getLoai())) {
                    java.math.BigDecimal tyLe = km.getGiaTri().divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                    java.math.BigDecimal tienGiam = giaGoc.multiply(tyLe).setScale(0, java.math.RoundingMode.HALF_UP);
                    giaSauKM = giaGoc.subtract(tienGiam);
                } else {
                    giaSauKM = giaGoc.subtract(km.getGiaTri());
                }
            }

            if (giaSauKM.compareTo(java.math.BigDecimal.ZERO) < 0) {
                giaSauKM = java.math.BigDecimal.ZERO;
            }

            spct.setGiaSauKhuyenMai(giaSauKM);
        }

        return list;
    }

    // 2. LẤY DANH SÁCH HÓA ĐƠN CHỜ
    @GetMapping("/invoices/pending")
    public List<HoaDon> getPendingInvoices() {
        return hoaDonRepo.findByLoaiHoaDonAndTrangThaiHoaDonMa(0, "CHO_THANH_TOAN");
    }

    // 3. TẠO MỚI HÓA ĐƠN CHỜ
    @PostMapping("/invoices")
    @Transactional
    public HoaDon createInvoice() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("HD");
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }

        TrangThaiHoaDon trangThai = trangThaiRepo.findByMa("CHO_THANH_TOAN");

        NhanVien nvTaoDon = getLoggedInNhanVien();

        HoaDon hd = HoaDon.builder()
                .ma(sb.toString())
                .giaTamThoi(BigDecimal.ZERO)
                .giaTong(BigDecimal.ZERO)
                .loaiHoaDon(0)
                .trangThaiHoaDon(trangThai)
                .nhanVien(nvTaoDon)
                .ngayTao(new Date())
                .build();

        HoaDon savedHd = hoaDonRepo.save(hd);

        lichSuRepo.save(LichSuHoaDon.builder()
                .hoaDon(savedHd)
                .trangThaiHoaDon(trangThai)
                .ghiChu("Tạo hóa đơn mới tại quầy")
                .nhanVien(nvTaoDon)
                .build());

        return savedHd;
    }

    // ==========================================================
    // CÁC HÀM MỚI: QUẢN LÝ GIỎ HÀNG VÀ TỒN KHO TRỰC TIẾP
    // ==========================================================

    // THÊM SẢN PHẨM VÀO HÓA ĐƠN (Trừ kho ngay lập tức)
    @PostMapping("/invoices/{id}/add-product")
    @Transactional
    public ResponseEntity<?> addProductToInvoice(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Integer spctId = (Integer) body.get("spctId");
        Integer qty = (Integer) body.get("qty");

        HoaDon hd = hoaDonRepo.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        SanPhamChiTiet spct = variantRepo.findById(spctId).orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // Kiểm tra tồn kho
        if (spct.getSoLuong() < qty) {
            return ResponseEntity.badRequest().body(Map.of("message", "Sản phẩm không đủ số lượng!"));
        }

        // 1. Trừ kho ngay lập tức
        spct.setSoLuong(spct.getSoLuong() - qty);
        if (spct.getSoLuong() == 0) spct.setTrangThai(false);
        variantRepo.save(spct);

        // 2. Cập nhật hoặc tạo mới HoaDonChiTiet
        Optional<HoaDonChiTiet> existingDetail = hdctRepo.findByHoaDonIdAndSanPhamChiTietId(id, spctId);
        if (existingDetail.isPresent()) {
            HoaDonChiTiet detail = existingDetail.get();
            detail.setSoLuong(detail.getSoLuong() + qty);
            hdctRepo.save(detail);
        } else {
            hdctRepo.save(HoaDonChiTiet.builder()
                    .hoaDon(hd)
                    .sanPhamChiTiet(spct)
                    .soLuong(qty)
                    .giaTien(spct.getGiaSauKhuyenMai() != null ? spct.getGiaSauKhuyenMai() : spct.getGiaBan())
                    .build());
        }

        // 3. Cập nhật lại tổng tiền tạm thời của hóa đơn
        updateInvoiceTotal(hd);
        return ResponseEntity.ok(Map.of("message", "Đã thêm vào hóa đơn và trừ kho"));
    }

    // CẬP NHẬT SỐ LƯỢNG TRONG GIỎ (Bù/Trừ kho)
    @PatchMapping("/invoices/{id}/update-quantity")
    @Transactional
    public ResponseEntity<?> updateQuantity(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Integer spctId = (Integer) body.get("spctId");
        Integer newQty = (Integer) body.get("newQty");

        HoaDonChiTiet detail = hdctRepo.findByHoaDonIdAndSanPhamChiTietId(id, spctId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));
        SanPhamChiTiet spct = detail.getSanPhamChiTiet();

        int diff = newQty - detail.getSoLuong(); // Số lượng chênh lệch

        if (diff > 0) {
            // Nếu tăng số lượng mua -> Trừ thêm kho
            if (spct.getSoLuong() < diff) return ResponseEntity.badRequest().body(Map.of("message", "Không đủ hàng trong kho"));
            spct.setSoLuong(spct.getSoLuong() - diff);
        } else {
            // Nếu giảm số lượng mua -> Hoàn lại kho
            spct.setSoLuong(spct.getSoLuong() + Math.abs(diff));
        }

        if (spct.getSoLuong() == 0) spct.setTrangThai(false);
        else spct.setTrangThai(true);

        variantRepo.save(spct);
        detail.setSoLuong(newQty);
        hdctRepo.save(detail);
        updateInvoiceTotal(detail.getHoaDon());

        return ResponseEntity.ok(Map.of("message", "Đã cập nhật kho"));
    }

    // XÓA SẢN PHẨM KHỎI GIỎ (Hoàn kho toàn bộ)
    @DeleteMapping("/invoices/{id}/remove-product/{spctId}")
    @Transactional
    public ResponseEntity<?> removeProduct(@PathVariable Integer id, @PathVariable Integer spctId) {
        HoaDonChiTiet detail = hdctRepo.findByHoaDonIdAndSanPhamChiTietId(id, spctId)
                .orElseThrow(() -> new RuntimeException("Không có sản phẩm này trong giỏ"));

        // HOÀN LẠI KHO
        SanPhamChiTiet spct = detail.getSanPhamChiTiet();
        spct.setSoLuong(spct.getSoLuong() + detail.getSoLuong());
        if (spct.getSoLuong() > 0) spct.setTrangThai(true);
        variantRepo.save(spct);

        hdctRepo.delete(detail);
        updateInvoiceTotal(detail.getHoaDon());
        return ResponseEntity.ok(Map.of("message", "Đã hoàn kho"));
    }

    // HÀM PHỤ TRỢ TÍNH TỔNG TIỀN
    private void updateInvoiceTotal(HoaDon hd) {
        List<HoaDonChiTiet> details = hdctRepo.findByHoaDonId(hd.getId());
        BigDecimal total = details.stream()
                .map(d -> d.getGiaTien().multiply(BigDecimal.valueOf(d.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        hd.setGiaTamThoi(total);
        hd.setGiaTong(total);
        hoaDonRepo.save(hd);
    }

    // ==========================================================

    // 4. CHỐT ĐƠN & THANH TOÁN (Đã sửa: Không trừ kho nữa)
    @PostMapping("/finish")
    @Transactional
    public ResponseEntity<?> finishOrder(@RequestBody Map<String, Object> payload) {
        try {
            Integer invoiceId = (Integer) payload.get("invoiceId");
            Integer customerId = (Integer) payload.get("customerId");
            BigDecimal total = new BigDecimal(payload.get("total").toString());
            String method = (String) payload.get("method");
            Integer voucherId = (Integer) payload.get("voucherId");

            HoaDon hd = hoaDonRepo.findById(invoiceId).orElseThrow(() -> new RuntimeException("Không tìm thấy HD!"));
            if (customerId != null) {
                hd.setKhachHang(khachHangRepo.findById(customerId).orElse(null));
            } else {
                hd.setKhachHang(null);
            }

            if (voucherId != null) {
                MaGiamGia voucher = maGiamGiaRepo.findById(voucherId).orElse(null);
                if (voucher != null) {
                    hd.setMaGiamGia(voucher);
                    int luotMoi = voucher.getLuotSuDung() + 1;
                    voucher.setLuotSuDung(luotMoi);
                    if (luotMoi >= voucher.getSoLuong()) {
                        voucher.setTrangThai(false);
                    }
                    maGiamGiaRepo.save(voucher);
                }
            }

            NhanVien nvChotDon = getLoggedInNhanVien();
            if (hd.getNhanVien() == null && nvChotDon != null) {
                hd.setNhanVien(nvChotDon);
            }

            // ĐÃ XÓA VÒNG LẶP TRỪ KHO Ở ĐÂY VÌ KHO ĐÃ TRỪ TRONG QUÁ TRÌNH THÊM VÀO GIỎ RỒI!

            hd.setGiaTong(total);
            hd.setNgayThanhToan(new Date());
            hd.setPhuongThucThanhToan(method);

            TrangThaiHoaDon ttHoanThanh = trangThaiRepo.findByMa("HOAN_THANH");
            hd.setTrangThaiHoaDon(ttHoanThanh);
            hoaDonRepo.save(hd);

            thanhToanRepo.save(ThanhToan.builder()
                    .hoaDon(hd)
                    .soTien(total)
                    .phuongThuc(method)
                    .nhanVien(nvChotDon)
                    .build());

            lichSuRepo.save(LichSuHoaDon.builder()
                    .hoaDon(hd)
                    .trangThaiHoaDon(ttHoanThanh)
                    .ghiChu("Thanh toán tại quầy")
                    .nhanVien(nvChotDon)
                    .build());

            return ResponseEntity.ok(Map.of("message", "Thành công!", "ma", hd.getMa()));
        } catch (Exception e) {
            e.printStackTrace();
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống không xác định";
            return ResponseEntity.badRequest().body(Map.of("message", errorMsg));
        }
    }

    // 5. VOUCHER: CHECK MÃ GIẢM GIÁ
    @GetMapping("/vouchers/apply")
    public ResponseEntity<?> applyVoucher(@RequestParam String code, @RequestParam BigDecimal currentTotal) {
        Optional<MaGiamGia> voucherOpt = maGiamGiaRepo.findByMaCode(code);
        if (voucherOpt.isEmpty()) return ResponseEntity.badRequest().body("Mã không tồn tại!");

        MaGiamGia v = voucherOpt.get();
        if (v.getNgayKetThuc().isBefore(LocalDateTime.now()) || v.getSoLuong() <= v.getLuotSuDung()) {
            return ResponseEntity.badRequest().body("MÃ GIẢM GIÁ ĐÃ HẾT HẠN HOẶC HẾT LƯỢT SỬ DỤNG");
        }

        if (currentTotal.compareTo(v.getDieuKien()) < 0) {
            return ResponseEntity.badRequest().body("Chưa đủ điều kiện (Tối thiểu " + v.getDieuKien() + ")");
        }
        return ResponseEntity.ok(v);
    }

    // 6. KHÁCH HÀNG: TÌM KIẾM & TẠO NHANH
    @GetMapping("/customers/search")
    public List<KhachHang> searchCustomers(@RequestParam String q) {
        return khachHangRepo.findBySoDienThoaiContainingOrHoTenContaining(q, q);
    }

    @PostMapping("/customers")
    public KhachHang createCustomer(@RequestBody KhachHang kh) {
        kh.setMa("KH" + System.currentTimeMillis());
        kh.setHoTen(kh.getHoTen() == null ? "Khách mới" : kh.getHoTen());
        kh.setTrangThai(true);
        return khachHangRepo.save(kh);
    }

    // 7. XÓA (HỦY) HÓA ĐƠN VÀ HOÀN KHO TOÀN BỘ
    @DeleteMapping("/invoices/{id}")
    @Transactional
    public ResponseEntity<?> deleteInvoice(@PathVariable Integer id) {
        HoaDon hd = hoaDonRepo.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        TrangThaiHoaDon trangThaiHuy = trangThaiRepo.findByMa("DA_HUY");
        if (trangThaiHuy == null) {
            trangThaiHuy = trangThaiRepo.findByMa("HUY_DON");
        }
        hd.setTrangThaiHoaDon(trangThaiHuy);

        if (hd.getMaGiamGia() != null) {
            MaGiamGia voucher = hd.getMaGiamGia();
            int luotMoi = voucher.getLuotSuDung() - 1;

            if (luotMoi >= 0) {
                voucher.setLuotSuDung(luotMoi);
                if (!voucher.getTrangThai() && voucher.getNgayKetThuc().isAfter(LocalDateTime.now())) {
                    voucher.setTrangThai(true);
                }
                maGiamGiaRepo.save(voucher);
            }
        }

        List<HoaDonChiTiet> listHdct = hdctRepo.findByHoaDonId(id);
        if (!listHdct.isEmpty()) {
            for (HoaDonChiTiet hdct : listHdct) {
                SanPhamChiTiet spct = hdct.getSanPhamChiTiet();
                spct.setSoLuong(spct.getSoLuong() + hdct.getSoLuong());

                // TỰ ĐỘNG BẬT LẠI TRẠNG THÁI KHI HOÀN KHO
                if (spct.getSoLuong() > 0 && (spct.getTrangThai() == null || spct.getTrangThai() == false)) {
                    spct.setTrangThai(true);
                }

                variantRepo.save(spct);
            }
        }

        NhanVien nvHuy = getLoggedInNhanVien();
        lichSuRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(trangThaiHuy)
                .ghiChu("Hủy hóa đơn chờ tại quầy")
                .nhanVien(nvHuy)
                .build());

        hoaDonRepo.save(hd);

        return ResponseEntity.ok(Map.of("message", "Đã hủy hóa đơn thành công"));
    }

    // 8. LẤY THUỘC TÍNH (Màu sắc & Kích thước)
    @GetMapping("/attributes")
    public Map<String, Object> getAttributes() {
        return Map.of(
                "colors", mauSacRepo.findAll(),
                "sizes", kichThuocRepo.findAll()
        );
    }
}