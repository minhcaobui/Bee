package com.example.bee.controllers.api.pos;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.account.VaiTro;
import com.example.bee.entities.cart.GioHang;
import com.example.bee.entities.order.*;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.entities.user.NhanVien;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.account.VaiTroRepository;
import com.example.bee.repositories.cart.GioHangRepository;
import com.example.bee.repositories.catalog.KichThuocRepository;
import com.example.bee.repositories.catalog.MauSacRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.order.*;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import com.example.bee.repositories.role.NhanVienRepository;
import com.example.bee.utils.MomoSecurity;
import com.example.bee.utils.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosApi {

    private static final Map<String, Map<String, Object>> pendingCarts = new java.util.concurrent.ConcurrentHashMap<>();

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
    @Value("${momo.returnUrl}")
    private String returnUrl;
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

    private NhanVien getLoggedInNhanVien() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            return nvRepo.findByTaiKhoan_TenDangNhap(username).orElse(null);
        }
        return null;
    }

    @GetMapping("/products/search")
    public List<SanPhamChiTiet> searchProducts(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Integer color,
            @RequestParam(required = false) Integer size) {
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

    @PostMapping("/invoices/update-hold")
    @Transactional
    public ResponseEntity<?> updateHoldStock(@RequestBody Map<String, Object> body) {
        Integer spctId = (Integer) body.get("spctId");
        Integer delta  = (Integer) body.get("delta");

        SanPhamChiTiet spct = variantRepo.findById(spctId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        if (delta > 0 && spct.getSoLuongKhaDung() < delta) {
            return ResponseEntity.badRequest().body(Map.of("message", "Kho không đủ đáp ứng!"));
        }

        spct.setSoLuongTamGiu(Math.max(0, spct.getSoLuongTamGiu() + delta));
        variantRepo.save(spct);

        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    @PostMapping("/finish")
    @Transactional
    public ResponseEntity<?> finishOrder(@RequestBody Map<String, Object> payload) {
        try {
            Integer customerId = (Integer) payload.get("customerId");
            String method     = (String)  payload.get("method");
            Integer voucherId = (Integer) payload.get("voucherId");
            List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");

            if (cart == null || cart.isEmpty()) {
                throw new RuntimeException("Giỏ hàng trống!");
            }

            String maHD = ("HD" + System.currentTimeMillis());

            NhanVien nvChotDon = getLoggedInNhanVien();

            HoaDon hd = HoaDon.builder()
                    .ma(maHD)
                    .giaTamThoi(BigDecimal.ZERO)
                    .giaTong(BigDecimal.ZERO)
                    .loaiHoaDon(0)
                    .ngayTao(new Date())
                    .nhanVien(nvChotDon)
                    .build();

            if (customerId != null) hd.setKhachHang(khachHangRepo.findById(customerId).orElse(null));

            hd = hoaDonRepo.save(hd);

            BigDecimal giaTamTinh = BigDecimal.ZERO;
            BigDecimal tongTienNguyenGia = BigDecimal.ZERO;

            for (Map<String, Object> item : cart) {
                Integer spctId = Integer.valueOf(item.get("id").toString());
                Integer qty    = Integer.valueOf(item.get("qty").toString());
                BigDecimal price = new BigDecimal(item.get("price").toString());

                SanPhamChiTiet spct = variantRepo.findById(spctId).orElseThrow(() -> new RuntimeException("Lỗi kho"));

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
            String noteKhuyenMai = "";
            if (hd.getKhachHang() != null && hd.getKhachHang().getSoDienThoai() != null) {
                boolean isEmployee = nvRepo.existsBySoDienThoaiAndTrangThaiTrue(hd.getKhachHang().getSoDienThoai());
                if (isEmployee) {
                    chietKhauNV = giaTamTinh.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
                    noteKhuyenMai += "(Áp dụng giảm 5% NV) ";
                }
            }

            BigDecimal giamGiaVoucher = BigDecimal.ZERO;
            if (voucherId != null) {
                MaGiamGia voucher = maGiamGiaRepo.findById(voucherId).orElse(null);
                if (voucher != null) {

                    // 🌟 BỔ SUNG: KIỂM TRA MỖI KHÁCH CHỈ ĐƯỢC DÙNG 1 LẦN KHI CHỐT ĐƠN TẠI POS
                    if (hd.getKhachHang() != null) {
                        boolean daSuDung = hoaDonRepo.existsByKhachHangIdAndMaGiamGiaIdAndTrangThaiHoaDon_MaNot(hd.getKhachHang().getId(), voucher.getId(), "DA_HUY");
                        if (daSuDung) {
                            throw new RuntimeException("Khách hàng này đã sử dụng mã giảm giá này rồi!"); // Ném lỗi để rollback giao dịch
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
            hd.setGiaTong(totalFinal);

            hd.setNgayThanhToan(new Date());
            TrangThaiHoaDon ttHoanThanh = trangThaiRepo.findByMa("HOAN_THANH");
            hd.setTrangThaiHoaDon(ttHoanThanh);
            hoaDonRepo.save(hd);

            thanhToanRepo.save(ThanhToan.builder().hoaDon(hd).soTien(hd.getGiaTong()).phuongThuc(method).trangThai("THANH_CONG").nhanVien(nvChotDon).build());
            lichSuRepo.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(ttHoanThanh).ghiChu("Thanh toán trực tiếp").nhanVien(nvChotDon).build());

            return ResponseEntity.ok(Map.of("message", "Thành công!", "ma", hd.getMa(), "id", hd.getId()));
        } catch (Exception e) {
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống"));
        }
    }

    @PostMapping("/momo-payment")
    public ResponseEntity<?> getMomoUrl(@RequestBody Map<String, Object> payload) {
        try {
            BigDecimal totalAmount = new BigDecimal(payload.get("amount").toString());

            String maHD = ("HD" + System.currentTimeMillis());

            payload.put("maHD", maHD);
            pendingCarts.put(maHD, payload);

            String rId = String.valueOf(System.currentTimeMillis());
            String oId = maHD + "_" + rId;
            String amountStr = totalAmount.setScale(0, RoundingMode.HALF_UP).toString();

            // 🌟 ĐÃ SỬA: Ép cứng ReturnUrl chạy vào Backend của POS trước
            String posReturnUrl = "http://localhost:8080/api/pos/momo-callback";

            String rawHash = "accessKey=" + accessKey.trim() + "&amount=" + amountStr + "&extraData=&ipnUrl=" + notifyUrl.trim() + "&orderId=" + oId + "&orderInfo=ThanhToan_" + maHD + "&partnerCode=" + partnerCode.trim() + "&redirectUrl=" + posReturnUrl + "&requestId=" + rId + "&requestType=captureWallet";
            String signature = MomoSecurity.signHmacSHA256(rawHash, secretKey.trim());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("partnerCode", partnerCode.trim());
            body.put("accessKey", accessKey.trim());
            body.put("requestId", rId);
            body.put("amount", Long.valueOf(amountStr));
            body.put("orderId", oId);
            body.put("orderInfo", "ThanhToan_" + maHD);
            body.put("redirectUrl", posReturnUrl); // 🌟 BẮT MOMO TRẢ VỀ BACKEND
            body.put("ipnUrl", notifyUrl.trim());
            body.put("extraData", "");
            body.put("requestType", "captureWallet");
            body.put("signature", signature);
            body.put("lang", "vi");
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.postForObject(endpoint.trim(), body, Map.class);
            return ResponseEntity.ok(Map.of("payUrl", response.get("payUrl"), "maHD", maHD));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/vnpay-payment")
    public ResponseEntity<?> getVnPayUrl(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            BigDecimal totalAmount = new BigDecimal(payload.get("amount").toString());

            String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            StringBuilder sb = new StringBuilder("HD");
            Random random = new Random();
            for (int i = 0; i < 6; i++) sb.append(characters.charAt(random.nextInt(characters.length())));
            String maHD = sb.toString();

            payload.put("maHD", maHD);
            pendingCarts.put(maHD, payload);

            String vnp_TxnRef = maHD + "_" + System.currentTimeMillis();
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode.trim());
            vnp_Params.put("vnp_Amount", String.valueOf(totalAmount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "ThanhToan_HoaDon_" + maHD);
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
            return ResponseEntity.ok(Map.of("payUrl", paymentUrl, "maHD", maHD));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/confirm-online-payment")
    @Transactional
    public ResponseEntity<?> confirmOnlinePayment(@RequestBody Map<String, String> body) {
        String maHD = body.get("maHD");

        // 🌟 BƯỚC 1: Xóa ngay lập tức Payload khỏi RAM để chống chạy 2 lần đồng thời
        Map<String, Object> payload = pendingCarts.remove(maHD);

        if (payload == null) {
            // Nếu payload = null -> Request này là Request sinh đôi đang chạy chậm hơn, hoặc HĐ đã được xử lý xong
            HoaDon existing = hoaDonRepo.findByMa(maHD);
            if (existing != null) {
                // Trả về lỗi để trình duyệt KHÔNG IN RA HÓA ĐƠN NỮA
                return ResponseEntity.badRequest().body(Map.of("message", "Đã in hóa đơn trước đó rồi"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi hoặc hóa đơn đã bị hủy"));
        }

        // BƯỚC 2: Request đầu tiên qua được chốt chặn sẽ thực hiện lưu vào DB
        Integer customerId = (Integer) payload.get("customerId");
        List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");
        Integer voucherId = (Integer) payload.get("voucherId");

        HoaDon hd = HoaDon.builder()
                .ma(maHD)
                .giaTamThoi(BigDecimal.ZERO)
                .giaTong(BigDecimal.ZERO)
                .loaiHoaDon(0) // Tại quầy
                .ngayTao(new Date())
                .nhanVien(getLoggedInNhanVien())
                .build();

        if (customerId != null) hd.setKhachHang(khachHangRepo.findById(customerId).orElse(null));
        hd = hoaDonRepo.save(hd);

        BigDecimal giaTamTinh = BigDecimal.ZERO;

        for (Map<String, Object> item : cart) {
            SanPhamChiTiet spct = variantRepo.findById(Integer.valueOf(item.get("id").toString())).get();
            int qty = Integer.valueOf(item.get("qty").toString());
            BigDecimal price = new BigDecimal(item.get("price").toString());

            spct.setSoLuong(spct.getSoLuong() - qty);
            spct.setSoLuongTamGiu(Math.max(0, spct.getSoLuongTamGiu() - qty));

            if (spct.getSoLuong() <= 0) spct.setTrangThai(false);
            variantRepo.save(spct);
            hdctRepo.save(HoaDonChiTiet.builder().hoaDon(hd).sanPhamChiTiet(spct).soLuong(qty).giaTien(price).build());
            giaTamTinh = giaTamTinh.add(price.multiply(BigDecimal.valueOf(qty)));
        }

        BigDecimal totalAmount = new BigDecimal(payload.get("amount").toString());
        hd.setGiaTamThoi(giaTamTinh);
        hd.setGiaTriKhuyenMai(giaTamTinh.subtract(totalAmount));
        hd.setGiaTong(totalAmount);
        hd.setNgayThanhToan(new Date());

        String pMethod = payload.get("method") != null ? payload.get("method").toString() : "ONLINE";

        if (voucherId != null) {
            MaGiamGia voucher = maGiamGiaRepo.findById(voucherId).orElse(null);
            if (voucher != null) {
                hd.setMaGiamGia(voucher);
                int luotMoi = voucher.getLuotSuDung() + 1;
                voucher.setLuotSuDung(luotMoi);
                if (luotMoi >= voucher.getSoLuong()) voucher.setTrangThai(false);
                maGiamGiaRepo.save(voucher);
            }
        }

        hd.setTrangThaiHoaDon(trangThaiRepo.findByMa("HOAN_THANH"));
        hoaDonRepo.save(hd);

        thanhToanRepo.save(ThanhToan.builder().hoaDon(hd).soTien(totalAmount).phuongThuc(pMethod).trangThai("THANH_CONG").nhanVien(getLoggedInNhanVien()).build());
        lichSuRepo.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(hd.getTrangThaiHoaDon()).ghiChu("Thanh toán " + pMethod + " thành công").nhanVien(getLoggedInNhanVien()).build());

        // CHỈ TRẢ VỀ ID CHO REQUEST CHẠY ĐẦU TIÊN NÀY
        return ResponseEntity.ok(Map.of(
                "message", "Thành công",
                "id", hd.getId()
        ));
    }

    // 🌟 SỬA LỖI MOMO VÀ VNPAY TRẢ VỀ SAI CÚ PHÁP
    @RequestMapping(value = "/momo-callback", method = {RequestMethod.GET, RequestMethod.POST})
    public void momoCallback(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String queryString = request.getQueryString();
        String redirectUrl = "http://localhost:8080/admin";

        // Nếu QueryString rỗng (do MoMo dùng POST), tự động móc dữ liệu từ Body ra để ghép thành URL
        if (queryString == null || queryString.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue()[0], StandardCharsets.UTF_8.name()));
            }
            queryString = sb.toString();
        }

        if (queryString != null && !queryString.isEmpty()) {
            redirectUrl += "?" + queryString;
        }
        redirectUrl += "#pos"; // Đẩy về đúng tab POS

        // Dùng sendRedirect nguyên thủy của Servlet để trình duyệt không bao giờ bị lỗi URL
        response.sendRedirect(redirectUrl);
    }

    @RequestMapping(value = "/vnpay-callback", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> vnpayCallback(@RequestParam Map<String, String> params) {
        String redirectUrl = "http://localhost:8080/admin";
        StringBuilder queryString = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (queryString.length() > 0) queryString.append("&");
            try {
                queryString.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            } catch (Exception e) {}
        }

        if (queryString.length() > 0) redirectUrl += "?" + queryString.toString();
        redirectUrl += "#pos";

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Location", redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/vouchers/apply")
    public ResponseEntity<?> applyVoucher(@RequestBody Map<String, Object> payload) {
        String code = (String) payload.get("code");
        List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");
        // 🌟 Lấy thêm ID Khách hàng từ payload (nếu có chọn khách)
        Object customerIdObj = payload.get("customerId");

        Optional<MaGiamGia> voucherOpt = maGiamGiaRepo.findByMaCode(code);
        if (voucherOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã không tồn tại!"));
        }

        MaGiamGia v = voucherOpt.get();
        if (v.getNgayKetThuc().isBefore(LocalDateTime.now()) || v.getSoLuong() <= v.getLuotSuDung() || !v.getTrangThai()) {
            return ResponseEntity.badRequest().body(Map.of("message", "MÃ GIẢM GIÁ ĐÃ HẾT HẠN HOẶC HẾT LƯỢT SỬ DỤNG"));
        }

        // 🌟 BỔ SUNG: KIỂM TRA MỖI KHÁCH 1 LẦN NGAY LÚC ÁP MÃ
        if (customerIdObj != null && !customerIdObj.toString().trim().isEmpty()) {
            Integer cId = Integer.valueOf(customerIdObj.toString());
            boolean daSuDung = hoaDonRepo.existsByKhachHangIdAndMaGiamGiaIdAndTrangThaiHoaDon_MaNot(cId, v.getId(), "DA_HUY");
            if (daSuDung) {
                return ResponseEntity.badRequest().body(Map.of("message", "Khách hàng này đã sử dụng mã giảm giá này rồi!"));
            }
        }

        BigDecimal giaTamTinh = BigDecimal.ZERO;
        BigDecimal tongTienNguyenGia = BigDecimal.ZERO;

        if (cart != null) {
            for (Map<String, Object> item : cart) {
                Integer spctId = Integer.valueOf(item.get("id").toString());
                Integer qty = Integer.valueOf(item.get("qty").toString());
                BigDecimal price = new BigDecimal(item.get("price").toString());

                SanPhamChiTiet spct = variantRepo.findById(spctId).orElseThrow(() -> new RuntimeException("Lỗi kho"));
                BigDecimal giaGoc = spct.getGiaBan();
                BigDecimal thanhTienItem = price.multiply(BigDecimal.valueOf(qty));
                giaTamTinh = giaTamTinh.add(thanhTienItem);

                if (price.compareTo(giaGoc) >= 0) {
                    tongTienNguyenGia = tongTienNguyenGia.add(thanhTienItem);
                }
            }
        }

        if (giaTamTinh.compareTo(v.getDieuKien()) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chưa đủ điều kiện (Tối thiểu " + new java.text.DecimalFormat("#,###").format(v.getDieuKien()) + "đ)"));
        }

        if (Boolean.FALSE.equals(v.getChoPhepCongDon()) && !"FREESHIP".equalsIgnoreCase(v.getLoaiGiamGia())) {
            if (tongTienNguyenGia.compareTo(BigDecimal.ZERO) == 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mã giảm giá này KHÔNG hỗ trợ áp dụng cho các sản phẩm đang chạy Sale!"));
            }
        }

        return ResponseEntity.ok(v);
    }

    @GetMapping("/customers/search")
    public List<KhachHang> searchCustomers(@RequestParam String q) {
        return khachHangRepo.findBySoDienThoaiContainingOrHoTenContaining(q, q);
    }

    @PostMapping("/customers")
    @Transactional
    public ResponseEntity<?> createCustomer(@RequestBody KhachHang kh) {
        String sdt = kh.getSoDienThoai();
        if (sdt == null || sdt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập số điện thoại của khách hàng!"));
        }

        if (taiKhoanRepo.existsByTenDangNhap(sdt)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại này đã được đăng ký trên hệ thống!"));
        }

        try {
            VaiTro roleCustomer = vaiTroRepo.findByMa("ROLE_CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Chưa cấu hình quyền ROLE_CUSTOMER"));

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

            KhachHang savedKh = khachHangRepo.save(kh);
            return ResponseEntity.ok(savedKh);

        } catch (Exception e) {
            e.printStackTrace();
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống khi tạo tài khoản: " + e.getMessage()));
        }
    }

    @GetMapping("/attributes")
    public Map<String, Object> getAttributes() {
        return Map.of(
                "colors", mauSacRepo.findAll(),
                "sizes", kichThuocRepo.findAll()
        );
    }

    @GetMapping("/{id}/print-data")
    public ResponseEntity<?> getInvoicePrintData(@PathVariable Integer id) {
        HoaDon hd = hoaDonRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));

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

        List<ThanhToan> ttListPrint = thanhToanRepo.findByHoaDon_Id(hd.getId());
        String ptttPrint = "TIEN_MAT";
        if (ttListPrint != null && !ttListPrint.isEmpty()) {
            ptttPrint = ttListPrint.get(0).getPhuongThuc();
        }
        summary.put("phuongThuc", ptttPrint);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("store", storeInfo);
        responseData.put("order", orderInfo);
        responseData.put("items", items);
        responseData.put("summary", summary);

        return ResponseEntity.ok(responseData);
    }

    // 🌟 API Lấy danh sách ID mã giảm giá của một khách hàng cụ thể (Dành cho POS)
    @GetMapping("/customers/{id}/used-vouchers")
    public ResponseEntity<?> getCustomerUsedVouchers(@PathVariable Integer id) {
        List<Integer> usedIds = hoaDonRepo.findByKhachHangIdOrderByNgayTaoDesc(id).stream()
                .filter(hd -> !"DA_HUY".equals(hd.getTrangThaiHoaDon().getMa()) && hd.getMaGiamGia() != null)
                .map(hd -> hd.getMaGiamGia().getId())
                .distinct()
                .collect(Collectors.toList());
        return ResponseEntity.ok(usedIds);
    }
}