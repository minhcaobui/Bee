package com.example.bee.controllers.api.pos;

import com.example.bee.entities.order.*;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.promotion.MaGiamGia; // Check lại package này
import com.example.bee.repositories.catalog.KichThuocRepository;
import com.example.bee.repositories.catalog.MauSacRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.order.*;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository; // Repository cho voucher
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // CHỈ ĐỂ DUY NHẤT 1 HÀM NÀY CHO ĐƯỜNG DẪN /products/search
    @GetMapping("/products/search")
    public List<SanPhamChiTiet> searchProducts(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Integer color,
            @RequestParam(required = false) Integer size) {

        return variantRepo.findAvailableProducts(q, color, size);
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
                .giaTamThoi(0.0)
                .giaTong(0.0)
                .loaiHoaDon(0) // 0 = TẠI QUẦY
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
            Double total = Double.valueOf(payload.get("total").toString());
            String method = (String) payload.get("method");
            List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");
            Integer voucherId = (Integer) payload.get("voucherId");

            Double giaTamTinh = 0.0;
            for (Map<String, Object> item : cart) {
                Integer qty = (Integer) item.get("qty");
                Double price = Double.valueOf(item.get("price").toString());
                giaTamTinh += (price * qty);
            }

            HoaDon hd = hoaDonRepo.findById(invoiceId).orElseThrow(() -> new RuntimeException("Không tìm thấy HD!"));
            hd.setKhachHang(khachHangRepo.findById(customerId).orElse(null));

            if (voucherId != null) {
                hd.setMaGiamGia(maGiamGiaRepo.findById(voucherId).orElse(null));
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
                Double price = Double.valueOf(item.get("price").toString());

                SanPhamChiTiet spct = variantRepo.findById(spctId).get();
                hdctRepo.save(HoaDonChiTiet.builder()
                        .hoaDon(hd)
                        .sanPhamChiTiet(spct)
                        .soLuong(qty)
                        .giaTien(price)
                        .build());

                if (spct.getSoLuong() < qty) throw new RuntimeException("Sản phẩm " + spct.getSku() + " đã hết hàng!");
                spct.setSoLuong(spct.getSoLuong() - qty);
                variantRepo.save(spct);
            }

            thanhToanRepo.save(ThanhToan.builder()
                    .hoaDon(hd)
                    .soTien(total)
                    .phuongThuc(method)
                    .build());
            lichSuRepo.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(hd.getTrangThaiHoaDon()).ghiChu("Thanh toán tại quầy").build());

            return ResponseEntity.ok(Map.of("message", "Thành công!", "ma", hd.getMa()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 5. VOUCHER: CHECK MÃ GIẢM GIÁ
    @GetMapping("/vouchers/apply")
    public ResponseEntity<?> applyVoucher(@RequestParam String code, @RequestParam Double currentTotal) {
        Optional<MaGiamGia> voucherOpt = maGiamGiaRepo.findByMaCode(code);
        if (voucherOpt.isEmpty()) return ResponseEntity.badRequest().body("Mã không tồn tại!");

        MaGiamGia v = voucherOpt.get();
        if (v.getNgayKetThuc().isBefore(LocalDateTime.now()) || v.getSoLuong() <= v.getLuotSuDung()) {
            return ResponseEntity.badRequest().body("MÃ GIẢM GIÁ ĐÃ HẾT HẠN HOẶC HẾT LƯỢT SỬ DỤNG");
        }
        if (currentTotal < v.getDieuKien().doubleValue()) {
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

    // 7. XÓA HÓA ĐƠN CHỜ
    @DeleteMapping("/invoices/{id}")
    @Transactional
    public ResponseEntity<?> deleteInvoice(@PathVariable Integer id) {
        hdctRepo.deleteByHoaDonId(id);
        hoaDonRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Đã hủy hóa đơn"));
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