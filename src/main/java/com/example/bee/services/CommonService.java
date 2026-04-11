package com.example.bee.services;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.account.VaiTro;
import com.example.bee.entities.cart.GioHang;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.order.HoaDon;
import com.example.bee.entities.order.HoaDonChiTiet;
import com.example.bee.entities.order.ThanhToan;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.account.VaiTroRepository;
import com.example.bee.repositories.cart.GioHangRepository;
import com.example.bee.repositories.catalog.KichThuocRepository;
import com.example.bee.repositories.catalog.MauSacRepository;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.order.HoaDonChiTietRepository;
import com.example.bee.repositories.order.HoaDonRepository;
import com.example.bee.repositories.order.ThanhToanRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommonService {

    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository hdctRepo;
    private final ThanhToanRepository thanhToanRepo;
    private final KhachHangRepository khachHangRepo;
    private final SanPhamChiTietRepository variantRepo;
    private final KhuyenMaiRepository khuyenMaiRepo;
    private final MauSacRepository mauSacRepo;
    private final KichThuocRepository kichThuocRepo;
    private final MaGiamGiaRepository maGiamGiaRepo;
    private final TaiKhoanRepository taiKhoanRepo;
    private final VaiTroRepository vaiTroRepo;
    private final PasswordEncoder passwordEncoder;
    private final GioHangRepository gioHangRepository;

    // ==========================================
    // 1. NGHIỆP VỤ IN HÓA ĐƠN (Dùng chung Online & POS)
    // ==========================================
    public Map<String, Object> getInvoicePrintData(Integer id) {
        HoaDon hd = hoaDonRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        List<HoaDonChiTiet> listHdct = hdctRepo.findByHoaDonId(id);

        Map<String, Object> storeInfo = new HashMap<>();
        storeInfo.put("tenCuaHang", "SHOP THỜI TRANG BEEMATE");
        storeInfo.put("diaChi", "123 Đường Trịnh Văn Bô, Phúc Yên, Vĩnh Phúc");
        storeInfo.put("soDienThoai", "0988.123.456");

        Map<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("maHoaDon", hd.getMa());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        orderInfo.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : sdf.format(new Date()));
        orderInfo.put("thuNgan", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Hệ thống");

        // Xử lý tên người nhận thông minh (Hỗ trợ cấu trúc JSON ThongTinGiaoHang mới)
        String tenNhan = "Khách vãng lai";
        String sdtNhan = "";
        if (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getTenNguoiNhan() != null) {
            tenNhan = hd.getThongTinGiaoHang().getTenNguoiNhan();
            sdtNhan = hd.getThongTinGiaoHang().getSdtNhan() != null ? hd.getThongTinGiaoHang().getSdtNhan() : "";
        } else if (hd.getKhachHang() != null) {
            tenNhan = hd.getKhachHang().getHoTen();
            sdtNhan = hd.getKhachHang().getSoDienThoai();
        }

        orderInfo.put("tenKhachHang", tenNhan);
        orderInfo.put("sdtKhachHang", sdtNhan);
        orderInfo.put("inThongTinTaiKhoan", hd.getKhachHang() != null);

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

        List<ThanhToan> ttListPrint = thanhToanRepo.findByHoaDon_Id(hd.getId());
        String ptttPrint = "TIEN_MAT";
        if (ttListPrint != null && !ttListPrint.isEmpty()) ptttPrint = ttListPrint.get(0).getPhuongThuc();
        summary.put("phuongThuc", ptttPrint);

        return Map.of("store", storeInfo, "order", orderInfo, "items", items, "summary", summary);
    }

    // ==========================================
    // 2. NGHIỆP VỤ TÌM SẢN PHẨM & THUỘC TÍNH (Dùng cho tạo đơn)
    // ==========================================
    public List<SanPhamChiTiet> searchProductsForCheckout(String q, Integer color, Integer size) {
        List<SanPhamChiTiet> list = variantRepo.findAvailableProducts(q, color, size);
        for (SanPhamChiTiet spct : list) {
            spct.setSoLuong(spct.getSoLuongKhaDung());
            BigDecimal giaGoc = spct.getGiaBan();
            BigDecimal giaSauKM = giaGoc;
            List<KhuyenMai> activeSales = khuyenMaiRepo.findActiveKhuyenMaiBySanPhamId(spct.getSanPham().getId());
            if (activeSales != null && !activeSales.isEmpty()) {
                KhuyenMai km = activeSales.get(0);
                boolean isPercent = km.getLoai() != null &&
                        (km.getLoai().toUpperCase().contains("PERCENT") || km.getLoai().contains("%"));
                if (isPercent) {
                    BigDecimal tyLe = km.getGiaTri().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    BigDecimal tienGiam = giaGoc.multiply(tyLe).setScale(0, RoundingMode.HALF_UP);
                    giaSauKM = giaGoc.subtract(tienGiam);
                } else {
                    giaSauKM = giaGoc.subtract(km.getGiaTri());
                }
            }
            if (giaSauKM.compareTo(BigDecimal.ZERO) < 0) giaSauKM = BigDecimal.ZERO;
            spct.setGiaSauKhuyenMai(giaSauKM);
        }
        return list;
    }

    public Map<String, Object> getAttributes() {
        return Map.of("colors", mauSacRepo.findAll(), "sizes", kichThuocRepo.findAll());
    }

    // ==========================================
    // 3. NGHIỆP VỤ VỀ KHÁCH HÀNG (Tìm kiếm & Tạo mới nhanh)
    // ==========================================
    public List<KhachHang> searchCustomers(String q) {
        return khachHangRepo.findBySoDienThoaiContainingOrHoTenContaining(q, q);
    }

    @Transactional
    public KhachHang createCustomerFast(KhachHang kh) {
        String sdt = kh.getSoDienThoai();
        if (sdt == null || sdt.trim().isEmpty()) throw new IllegalArgumentException("Vui lòng nhập số điện thoại của khách hàng!");
        if (taiKhoanRepo.existsByTenDangNhap(sdt)) throw new IllegalArgumentException("Số điện thoại này đã được đăng ký trên hệ thống!");

        VaiTro roleCustomer = vaiTroRepo.findByMa("ROLE_CUSTOMER").orElseThrow(() -> new IllegalArgumentException("Chưa cấu hình quyền ROLE_CUSTOMER"));
        String defaultPassword = "123456";
        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap(sdt);
        tk.setMatKhau(passwordEncoder.encode(defaultPassword));
        tk.setVaiTro(roleCustomer);
        tk.setTrangThai(true);
        TaiKhoan savedTk = taiKhoanRepo.save(tk);

        GioHang gioHang = new GioHang();
        gioHang.setTaiKhoan(savedTk);
        gioHangRepository.save(gioHang);

        long count = khachHangRepo.count();
        String ma;
        do {
            count++;
            ma = String.format("KH%08d", count);
        } while (khachHangRepo.existsByMaIgnoreCase(ma));

        kh.setMa(ma);
        kh.setHoTen(kh.getHoTen() == null || kh.getHoTen().trim().isEmpty() ? "Khách vãng lai" : kh.getHoTen());
        kh.setTaiKhoan(savedTk);
        kh.setTrangThai(true);

        return khachHangRepo.save(kh);
    }

    // ==========================================
    // 4. NGHIỆP VỤ VỀ VOUCHER (Tính toán áp dụng)
    // ==========================================
    public MaGiamGia applyVoucher(Map<String, Object> payload) {
        String code = (String) payload.get("code");
        List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");
        Object customerIdObj = payload.get("customerId");

        Optional<MaGiamGia> voucherOpt = maGiamGiaRepo.findByMaCode(code);
        if (voucherOpt.isEmpty()) throw new IllegalArgumentException("Mã không tồn tại!");

        MaGiamGia v = voucherOpt.get();
        if (v.getNgayKetThuc().isBefore(LocalDateTime.now()) || v.getSoLuong() <= v.getLuotSuDung() || !v.getTrangThai()) {
            throw new IllegalArgumentException("MÃ GIẢM GIÁ ĐÃ HẾT HẠN HOẶC HẾT LƯỢT SỬ DỤNG");
        }

        if (customerIdObj != null && !customerIdObj.toString().trim().isEmpty()) {
            Integer cId = Integer.valueOf(customerIdObj.toString());
            boolean daSuDung = hoaDonRepo.existsByKhachHangIdAndMaGiamGiaIdAndTrangThaiHoaDon_MaNot(cId, v.getId(), "DA_HUY");
            if (daSuDung) throw new IllegalArgumentException("Khách hàng này đã sử dụng mã giảm giá này rồi!");
        }

        BigDecimal giaTamTinh = BigDecimal.ZERO;
        BigDecimal tongTienNguyenGia = BigDecimal.ZERO;

        if (cart != null) {
            for (Map<String, Object> item : cart) {
                Integer spctId = Integer.valueOf(item.get("id").toString());
                Integer qty = Integer.valueOf(item.get("qty").toString());
                BigDecimal price = new BigDecimal(item.get("price").toString());

                SanPhamChiTiet spct = variantRepo.findById(spctId).orElseThrow(() -> new IllegalArgumentException("Lỗi kho"));
                BigDecimal giaGoc = spct.getGiaBan();
                BigDecimal thanhTienItem = price.multiply(BigDecimal.valueOf(qty));
                giaTamTinh = giaTamTinh.add(thanhTienItem);

                if (price.compareTo(giaGoc) >= 0) {
                    tongTienNguyenGia = tongTienNguyenGia.add(thanhTienItem);
                }
            }
        }

        if (giaTamTinh.compareTo(v.getDieuKien()) < 0) {
            throw new IllegalArgumentException("Chưa đủ điều kiện (Tối thiểu " + new java.text.DecimalFormat("#,###").format(v.getDieuKien()) + "đ)");
        }

        if (Boolean.FALSE.equals(v.getChoPhepCongDon()) && !"FREESHIP".equalsIgnoreCase(v.getLoaiGiamGia())) {
            if (tongTienNguyenGia.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Mã giảm giá này KHÔNG hỗ trợ áp dụng cho các sản phẩm đang chạy Sale!");
            }
        }
        return v;
    }

    public List<Integer> getCustomerUsedVouchers(Integer id) {
        return hoaDonRepo.findByKhachHangIdOrderByNgayTaoDesc(id).stream()
                .filter(hd -> !"DA_HUY".equals(hd.getTrangThaiHoaDon().getMa()) && hd.getMaGiamGia() != null)
                .map(hd -> hd.getMaGiamGia().getId())
                .distinct().collect(Collectors.toList());
    }
}