package com.example.bee.services;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.account.VaiTro;
import com.example.bee.entities.cart.GioHang;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.order.*;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.entities.staff.NhanVien;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.account.VaiTroRepository;
import com.example.bee.repositories.cart.GioHangRepository;
import com.example.bee.repositories.catalog.KichThuocRepository;
import com.example.bee.repositories.catalog.MauSacRepository;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.order.*;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import com.example.bee.repositories.staff.NhanVienRepository;
import com.example.bee.utils.MomoSecurity;
import com.example.bee.utils.VnPayUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosService {

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
    private final TaiKhoanRepository taiKhoanRepo;
    private final VaiTroRepository vaiTroRepo;
    private final PasswordEncoder passwordEncoder;
    private final GioHangRepository gioHangRepository;

    @Value("${momo.partnerCode}")
    private String partnerCode;
    @Value("${momo.accessKey}")
    private String accessKey;
    @Value("${momo.secretKey}")
    private String secretKey;
    @Value("${momo.endpoint}")
    private String endpoint;
    @Value("${momo.notifyUrl}")
    private String notifyUrl;
    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;
    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;
    @Value("${vnpay.payUrl}")
    private String vnp_PayUrl;
    @Value("${vnpay.returnUrl}")
    private String vnp_ReturnUrl;

    public NhanVien getLoggedInNhanVien() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            return nvRepo.findByTaiKhoan_TenDangNhap(username).orElse(null);
        }
        return null;
    }

    public List<SanPhamChiTiet> searchProducts(String q, Integer color, Integer size) {
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

    @Transactional
    public void updateHoldStock(Integer spctId, Integer delta) {
        SanPhamChiTiet spct = variantRepo.findById(spctId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        if (delta > 0 && spct.getSoLuongKhaDung() < delta) {
            throw new IllegalArgumentException("Kho không đủ đáp ứng!");
        }

        spct.setSoLuongTamGiu(Math.max(0, spct.getSoLuongTamGiu() + delta));
        variantRepo.save(spct);
    }

    @Transactional
    public HoaDon createOrderToDB(Map<String, Object> payload, String method, String statusMa) {
        Integer customerId = (Integer) payload.get("customerId");
        List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");
        Integer voucherId = (Integer) payload.get("voucherId");
        BigDecimal totalAmount = new BigDecimal(payload.get("amount").toString());

        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống!");
        }

        String maHD = ("HD" + System.currentTimeMillis());
        NhanVien nvChotDon = getLoggedInNhanVien();

        HoaDon hd = HoaDon.builder()
                .ma(maHD)
                .giaTamThoi(BigDecimal.ZERO)
                .giaTong(BigDecimal.ZERO)
                .loaiHoaDon(0) // 0 là đơn tại quầy
                .hinhThucGiaoHang("NHAN_TAI_CUA_HANG") // Phân biệt với GIAO_TAN_NOI
                .ngayTao(new Date())
                .nhanVien(nvChotDon)
                .build();

        if (customerId != null) hd.setKhachHang(khachHangRepo.findById(customerId).orElse(null));
        hd = hoaDonRepo.save(hd);

        BigDecimal giaTamTinh = BigDecimal.ZERO;
        BigDecimal tongTienNguyenGia = BigDecimal.ZERO;

        for (Map<String, Object> item : cart) {
            Integer spctId = Integer.valueOf(item.get("id").toString());
            Integer qty = Integer.valueOf(item.get("qty").toString());
            BigDecimal price = new BigDecimal(item.get("price").toString());

            SanPhamChiTiet spct = variantRepo.findById(spctId).orElseThrow(() -> new IllegalArgumentException("Lỗi kho"));

            if (spct.getSoLuong() < qty) {
                throw new IllegalArgumentException("Sản phẩm " + spct.getSanPham().getTen() + " không đủ số lượng trong kho!");
            }

            BigDecimal giaGoc = spct.getGiaBan();
            BigDecimal thanhTienItem = price.multiply(BigDecimal.valueOf(qty));
            giaTamTinh = giaTamTinh.add(thanhTienItem);

            if (price.compareTo(giaGoc) >= 0) {
                tongTienNguyenGia = tongTienNguyenGia.add(thanhTienItem);
            }

            spct.setSoLuong(spct.getSoLuong() - qty);
            spct.setSoLuongTamGiu(Math.max(0, spct.getSoLuongTamGiu() - qty));

            if (spct.getSoLuong() <= 0) spct.setTrangThai(false);
            variantRepo.save(spct);

            hdctRepo.save(HoaDonChiTiet.builder().hoaDon(hd).sanPhamChiTiet(spct).soLuong(qty).giaTien(price).build());
        }

        BigDecimal chietKhauNV = BigDecimal.ZERO;
        if (hd.getKhachHang() != null && hd.getKhachHang().getSoDienThoai() != null) {
            boolean isEmployee = nvRepo.existsBySoDienThoaiAndTrangThaiTrue(hd.getKhachHang().getSoDienThoai());
            if (isEmployee) {
                chietKhauNV = giaTamTinh.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
            }
        }

        BigDecimal giamGiaVoucher = BigDecimal.ZERO;
        if (voucherId != null) {
            MaGiamGia voucher = maGiamGiaRepo.findById(voucherId).orElse(null);
            if (voucher != null) {
                if (!voucher.getTrangThai() ||
                        voucher.getLuotSuDung() >= voucher.getSoLuong() ||
                        (voucher.getNgayKetThuc() != null && voucher.getNgayKetThuc().isBefore(java.time.LocalDateTime.now()))) {
                    throw new IllegalArgumentException("Mã giảm giá này đã hết hạn hoặc hết lượt sử dụng!");
                }

                if (giaTamTinh.compareTo(voucher.getDieuKien()) < 0) {
                    throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getDieuKien() + "đ để dùng mã này!");
                }

                if (Boolean.FALSE.equals(voucher.getChoPhepCongDon()) && !"FREESHIP".equalsIgnoreCase(voucher.getLoaiGiamGia())) {
                    if (tongTienNguyenGia.compareTo(BigDecimal.ZERO) == 0) {
                        throw new IllegalArgumentException("Mã giảm giá này KHÔNG hỗ trợ áp dụng cho các sản phẩm đang chạy Sale!");
                    }
                }

                if (hd.getKhachHang() != null) {
                    boolean daSuDung = hoaDonRepo.existsByKhachHangIdAndMaGiamGiaIdAndTrangThaiHoaDon_MaNot(hd.getKhachHang().getId(), voucher.getId(), "DA_HUY");
                    if (daSuDung) {
                        throw new IllegalArgumentException("Khách hàng này đã sử dụng mã giảm giá này rồi!");
                    }
                }

                hd.setMaGiamGia(voucher);
                BigDecimal baseForVoucher = giaTamTinh;
                if (Boolean.FALSE.equals(voucher.getChoPhepCongDon()) && !"FREESHIP".equalsIgnoreCase(voucher.getLoaiGiamGia())) {
                    baseForVoucher = tongTienNguyenGia;
                }
                boolean isPercent = voucher.getLoaiGiamGia() != null && (voucher.getLoaiGiamGia().toUpperCase().contains("PERCENT") || voucher.getLoaiGiamGia().contains("%"));
                if (isPercent) {
                    giamGiaVoucher = baseForVoucher.multiply(voucher.getGiaTriGiamGia().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
                    if (voucher.getGiaTriGiamGiaToiDa() != null && giamGiaVoucher.compareTo(voucher.getGiaTriGiamGiaToiDa()) > 0) {
                        giamGiaVoucher = voucher.getGiaTriGiamGiaToiDa();
                    }
                } else {
                    giamGiaVoucher = voucher.getGiaTriGiamGia();
                    if (giamGiaVoucher.compareTo(baseForVoucher) > 0) giamGiaVoucher = baseForVoucher;
                }
                int luotMoi = voucher.getLuotSuDung() + 1;
                voucher.setLuotSuDung(luotMoi);
                if (luotMoi >= voucher.getSoLuong()) voucher.setTrangThai(false);
                maGiamGiaRepo.save(voucher);
            }
        }

        BigDecimal tongKhuyenMai = chietKhauNV.add(giamGiaVoucher);
        BigDecimal maxGiamGiaChoPhep = giaTamTinh.multiply(new BigDecimal("0.70")).setScale(0, RoundingMode.HALF_UP);
        if (tongKhuyenMai.compareTo(maxGiamGiaChoPhep) > 0) tongKhuyenMai = maxGiamGiaChoPhep;

        hd.setGiaTamThoi(giaTamTinh);
        hd.setGiaTriKhuyenMai(tongKhuyenMai);

        BigDecimal totalFinal = giaTamTinh.subtract(tongKhuyenMai);
        if (totalFinal.compareTo(BigDecimal.ZERO) < 0) totalFinal = BigDecimal.ZERO;
        hd.setGiaTong(totalAmount);

        TrangThaiHoaDon tthd = trangThaiRepo.findByMa(statusMa);
        hd.setTrangThaiHoaDon(tthd);
        if (statusMa.equals("HOAN_THANH")) {
            hd.setNgayThanhToan(new Date());
            hd.setNgayHangSanSang(new Date()); // Khách lấy hàng luôn
            hd.setNgayHenLayHang(new Date());
        }
        hoaDonRepo.save(hd);

        String ttStatus = statusMa.equals("HOAN_THANH") ? "THANH_CONG" : "CHO_THANH_TOAN";
        String ghiChuLichSu = statusMa.equals("HOAN_THANH") ? "Thanh toán " + method + " trực tiếp" : "Đang chờ thanh toán online " + method;

        thanhToanRepo.save(ThanhToan.builder()
                .hoaDon(hd)
                .soTien(hd.getGiaTong())
                .phuongThuc(method)
                .trangThai(ttStatus)
                .nhanVien(nvChotDon)
                .build());

        lichSuRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(tthd)
                .ghiChu(ghiChuLichSu)
                .nhanVien(nvChotDon)
                .build());

        return hd;
    }

    @Transactional
    public Map<String, Object> finishOrder(Map<String, Object> payload) {
        String method = payload.get("method") != null ? payload.get("method").toString() : "TIEN_MAT";
        HoaDon hd = createOrderToDB(payload, method, "HOAN_THANH");
        return Map.of("message", "Thành công!", "ma", hd.getMa(), "id", hd.getId());
    }

    @Transactional
    public Map<String, Object> getMomoUrl(Map<String, Object> payload) throws Exception {
        HoaDon hd = createOrderToDB(payload, "MOMO", "CHO_THANH_TOAN");

        String rId = String.valueOf(System.currentTimeMillis());
        String oId = hd.getMa() + "_" + rId;
        String amountStr = hd.getGiaTong().setScale(0, RoundingMode.HALF_UP).toString();

        String posReturnUrl = "http://beemate.store/api/pos/momo-callback";
        String rawHash = "accessKey=" + accessKey.trim() + "&amount=" + amountStr + "&extraData=&ipnUrl=" + notifyUrl.trim() + "&orderId=" + oId + "&orderInfo=ThanhToan_" + hd.getMa() + "&partnerCode=" + partnerCode.trim() + "&redirectUrl=" + posReturnUrl + "&requestId=" + rId + "&requestType=captureWallet";
        String signature = MomoSecurity.signHmacSHA256(rawHash, secretKey.trim());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode.trim());
        body.put("accessKey", accessKey.trim());
        body.put("requestId", rId);
        body.put("amount", Long.valueOf(amountStr));
        body.put("orderId", oId);
        body.put("orderInfo", "ThanhToan_" + hd.getMa());
        body.put("redirectUrl", posReturnUrl);
        body.put("ipnUrl", notifyUrl.trim());
        body.put("extraData", "");
        body.put("requestType", "captureWallet");
        body.put("signature", signature);
        body.put("lang", "vi");

        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.postForObject(endpoint.trim(), body, Map.class);
        return Map.of("payUrl", response.get("payUrl"), "maHD", hd.getMa());
    }

    @Transactional
    public Map<String, Object> getVnPayUrl(Map<String, Object> payload) throws Exception {
        HoaDon hd = createOrderToDB(payload, "VNPAY", "CHO_THANH_TOAN");

        String vnp_TxnRef = hd.getMa() + "_" + System.currentTimeMillis();
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode.trim());
        vnp_Params.put("vnp_Amount", String.valueOf(hd.getGiaTong().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "ThanhToan_HoaDon_" + hd.getMa());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl.trim());
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext(); ) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()).replace("+", "%20");
                String encodedName = URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString());
                hashData.append(fieldName).append('=').append(encodedValue);
                query.append(encodedName).append('=').append(encodedValue);
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String vnp_SecureHash = VnPayUtil.hmacSHA512(vnp_HashSecret.trim(), hashData.toString());
        String paymentUrl = vnp_PayUrl.trim() + "?" + query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;

        return Map.of("payUrl", paymentUrl, "maHD", hd.getMa());
    }

    @Transactional
    public Map<String, Object> confirmOnlinePayment(String maHD) {
        HoaDon hd = hoaDonRepo.findByMa(maHD);
        if (hd == null) throw new IllegalArgumentException("Không tìm thấy hóa đơn");
        if ("HOAN_THANH".equals(hd.getTrangThaiHoaDon().getMa())) {
            throw new IllegalArgumentException("Đã in hóa đơn trước đó rồi");
        }

        TrangThaiHoaDon ttHoanThanh = trangThaiRepo.findByMa("HOAN_THANH");
        hd.setTrangThaiHoaDon(ttHoanThanh);
        hd.setNgayThanhToan(new Date());
        hd.setNgayHangSanSang(new Date());
        hoaDonRepo.save(hd);

        List<ThanhToan> tts = thanhToanRepo.findByHoaDon_Id(hd.getId());
        if (tts != null && !tts.isEmpty()) {
            ThanhToan tt = tts.get(0);
            tt.setTrangThai("THANH_CONG");
            tt.setNgayThanhToan(new Date());
            thanhToanRepo.save(tt);
        }

        lichSuRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(ttHoanThanh)
                .ghiChu("Thanh toán Online thành công tại quầy")
                .nhanVien(getLoggedInNhanVien())
                .build());

        return Map.of("message", "Thành công", "id", hd.getId());
    }

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

    public List<KhachHang> searchCustomers(String q) {
        return khachHangRepo.findBySoDienThoaiContainingOrHoTenContaining(q, q);
    }

    @Transactional
    public KhachHang createCustomer(KhachHang kh) {
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

    public Map<String, Object> getAttributes() {
        return Map.of("colors", mauSacRepo.findAll(), "sizes", kichThuocRepo.findAll());
    }

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

        // Cập nhật lấy thông tin khách theo đúng cấu trúc Entity mới
        String tenNhan = "Khách vãng lai";
        String sdtNhan = "";
        if (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getTenNguoiNhan() != null) {
            tenNhan = hd.getThongTinGiaoHang().getTenNguoiNhan();
            sdtNhan = hd.getThongTinGiaoHang().getSdtNhan();
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

    public List<Integer> getCustomerUsedVouchers(Integer id) {
        return hoaDonRepo.findByKhachHangIdOrderByNgayTaoDesc(id).stream()
                .filter(hd -> !"DA_HUY".equals(hd.getTrangThaiHoaDon().getMa()) && hd.getMaGiamGia() != null)
                .map(hd -> hd.getMaGiamGia().getId())
                .distinct().collect(Collectors.toList());
    }
}