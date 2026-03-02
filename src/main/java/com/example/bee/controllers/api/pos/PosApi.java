package com.example.bee.controllers.api.pos;

import com.example.bee.entities.order.*;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.entities.promotion.MaGiamGia; // Check lại package này
import com.example.bee.repositories.catalog.KichThuocRepository;
import com.example.bee.repositories.catalog.MauSacRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.order.*;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository; // Repository cho voucher
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    // CHỈ ĐỂ DUY NHẤT 1 HÀM NÀY CHO ĐƯỜNG DẪN /products/search
    // 2. Thay lại hàm này:
    @GetMapping("/products/search")
    public List<SanPhamChiTiet> searchProducts(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Integer color,
            @RequestParam(required = false) Integer size) {

        List<SanPhamChiTiet> list = variantRepo.findAvailableProducts(q, color, size);

        // Chạy vòng lặp để soi từng sản phẩm xem có Sale không
        for (SanPhamChiTiet spct : list) {
            java.math.BigDecimal giaGoc = spct.getGiaBan();
            java.math.BigDecimal giaSauKM = giaGoc;

            // Lấy danh sách Sale đang chạy cho sản phẩm này
            List<KhuyenMai> activeSales = khuyenMaiRepo.findActiveKhuyenMaiBySanPhamId(spct.getSanPham().getId());

            if (activeSales != null && !activeSales.isEmpty()) {
                KhuyenMai km = activeSales.get(0); // Lấy Sale đầu tiên
                if ("PERCENT".equals(km.getLoai())) {
                    // Chia 100 và làm tròn HALF_UP
                    java.math.BigDecimal tyLe = km.getGiaTri().divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                    // Nhân với giá gốc để ra số tiền được giảm
                    java.math.BigDecimal tienGiam = giaGoc.multiply(tyLe).setScale(0, java.math.RoundingMode.HALF_UP);

                    giaSauKM = giaGoc.subtract(tienGiam);
                } else {
                    // Khuyến mãi trừ tiền mặt thẳng tay
                    giaSauKM = giaGoc.subtract(km.getGiaTri());
                }
            }

            // Ép giá ảo vào, nếu giảm dưới 0đ (âm) thì set = 0đ cho an toàn
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

    // 3. TẠO MỚI HÓA ĐƠN CHỜ (Mã HD 8 ký tự: HD + 6 ký tự random)
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
        HoaDon hd = HoaDon.builder()
                .ma(sb.toString())
                .giaTamThoi(BigDecimal.ZERO) // Thay 0.0
                .giaTong(BigDecimal.ZERO)    // Thay 0.0
                .loaiHoaDon(0)
                .trangThaiHoaDon(trangThai)
                .ngayTao(new Date())
                .build();
        return hoaDonRepo.save(hd);
    }

    // 4. CHỐT ĐƠN & THANH TOÁN (Logic trừ kho + Voucher + Lịch sử)
    @PostMapping("/finish")
    @Transactional
    public ResponseEntity<?> finishOrder(@RequestBody Map<String, Object> payload) {
        try {
            Integer invoiceId = (Integer) payload.get("invoiceId");
            Integer customerId = (Integer) payload.get("customerId");

            // Sửa tổng tiền thành BigDecimal
            BigDecimal total = new BigDecimal(payload.get("total").toString());
            String method = (String) payload.get("method");
            List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");
            Integer voucherId = (Integer) payload.get("voucherId");

            // Sửa giá tạm tính thành BigDecimal
            BigDecimal giaTamTinh = BigDecimal.ZERO;
            for (Map<String, Object> item : cart) {
                Integer qty = (Integer) item.get("qty");
                BigDecimal price = new BigDecimal(item.get("price").toString()); // Đổi sang BigDecimal

                // giaTamTinh += (price * qty) chuyển thành:
                giaTamTinh = giaTamTinh.add(price.multiply(BigDecimal.valueOf(qty)));
            }

            HoaDon hd = hoaDonRepo.findById(invoiceId).orElseThrow(() -> new RuntimeException("Không tìm thấy HD!"));
            if (customerId != null) {
                hd.setKhachHang(khachHangRepo.findById(customerId).orElse(null));
            } else {
                hd.setKhachHang(null);
            }

            // Logic trừ Voucher em đã fix cho anh ở trên
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

            hd.setGiaTamThoi(giaTamTinh);
            hd.setGiaTong(total);
            hd.setNgayThanhToan(new Date());
            hd.setPhuongThucThanhToan(method);
            hd.setTrangThaiHoaDon(trangThaiRepo.findByMa("HOAN_THANH"));
            hoaDonRepo.save(hd);

            for (Map<String, Object> item : cart) {
                Integer spctId = (Integer) item.get("id");
                Integer qty = (Integer) item.get("qty");

                // SỬA: Chuyển đổi giá từ Map sang BigDecimal thông qua chuỗi để tránh sai số
                BigDecimal price = new BigDecimal(item.get("price").toString());

                // 1. Tìm sản phẩm chi tiết
                SanPhamChiTiet spct = variantRepo.findById(spctId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm mã ID: " + spctId));

                // 2. KIỂM TRA TỒN KHO TRƯỚC (Logic quan trọng)
                if (spct.getSoLuong() < qty) {
                    throw new RuntimeException("Sản phẩm " + spct.getSku() + " không đủ số lượng trong kho!");
                }

                // 3. Cập nhật số lượng sản phẩm chi tiết
                spct.setSoLuong(spct.getSoLuong() - qty);
                variantRepo.save(spct);

                // 4. Lưu chi tiết hóa đơn với kiểu BigDecimal
                hdctRepo.save(HoaDonChiTiet.builder()
                        .hoaDon(hd)
                        .sanPhamChiTiet(spct)
                        .soLuong(qty)
                        .giaTien(price) // Giờ nó đã là chuẩn BigDecimal
                        .build());
            }

            thanhToanRepo.save(ThanhToan.builder()
                    .hoaDon(hd)
                    .soTien(total)
                    .phuongThuc(method)
                    .build());
            lichSuRepo.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(hd.getTrangThaiHoaDon()).ghiChu("Thanh toán tại quầy").build());

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

        // Sửa điều kiện so sánh
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

    // 7. XÓA (HỦY) HÓA ĐƠN - XÓA MỀM & HOÀN KHO/VOUCHER
    @DeleteMapping("/invoices/{id}")
    @Transactional
    public ResponseEntity<?> deleteInvoice(@PathVariable Integer id) {
        HoaDon hd = hoaDonRepo.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        // 1. CẬP NHẬT TRẠNG THÁI THÀNH "ĐÃ HỦY"
        // Lưu ý: Tùy database của anh quy định mã Hủy là gì (DA_HUY, HUY_DON, v.v.), anh thay cho khớp nhé!
        TrangThaiHoaDon trangThaiHuy = trangThaiRepo.findByMa("DA_HUY");
        if (trangThaiHuy == null) {
            trangThaiHuy = trangThaiRepo.findByMa("HUY_DON"); // Dự phòng nếu mã của anh là HUY_DON
        }
        hd.setTrangThaiHoaDon(trangThaiHuy);

        // 2. LOGIC HOÀN VOUCHER (Nếu đơn đã gắn voucher)
        if (hd.getMaGiamGia() != null) {
            MaGiamGia voucher = hd.getMaGiamGia();
            int luotMoi = voucher.getLuotSuDung() - 1;

            // Đảm bảo không bị âm lượt dùng
            if (luotMoi >= 0) {
                voucher.setLuotSuDung(luotMoi);

                // Bật lại voucher nếu trước đó bị tắt (do hết lượt) và vẫn còn hạn
                if (!voucher.getTrangThai() && voucher.getNgayKetThuc().isAfter(LocalDateTime.now())) {
                    voucher.setTrangThai(true);
                }
                maGiamGiaRepo.save(voucher);
            }
        }

        // 3. LOGIC HOÀN LẠI TỒN KHO (Nếu đơn đã có chi tiết sản phẩm)
        List<HoaDonChiTiet> listHdct = hdctRepo.findByHoaDonId(id);
        if (!listHdct.isEmpty()) {
            for (HoaDonChiTiet hdct : listHdct) {
                SanPhamChiTiet spct = hdct.getSanPhamChiTiet();
                // Cộng trả lại số lượng
                spct.setSoLuong(spct.getSoLuong() + hdct.getSoLuong());
                variantRepo.save(spct);
            }
        }

        // 4. LƯU LẠI HÓA ĐƠN VỚI TRẠNG THÁI MỚI (Bỏ đi 2 lệnh deleteById cũ)
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