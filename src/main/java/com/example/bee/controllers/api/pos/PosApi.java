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
import com.example.bee.utils.MomoSecurity;
import com.example.bee.utils.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosApi {

    // 🚀 BỘ NHỚ TẠM ĐỂ LƯU GIỎ HÀNG KHI CHỜ THANH TOÁN ONLINE (MOMO/VNPAY)
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

    @Value("${momo.partnerCode}") private String partnerCode;
    @Value("${momo.accessKey}") private String accessKey;
    @Value("${momo.secretKey}") private String secretKey;
    @Value("${momo.endpoint}") private String endpoint;
    @Value("${momo.returnUrl}") private String returnUrl;
    @Value("${momo.notifyUrl}") private String notifyUrl;

    @Value("${vnpay.tmnCode}") private String vnp_TmnCode;
    @Value("${vnpay.hashSecret}") private String vnp_HashSecret;
    @Value("${vnpay.payUrl}") private String vnp_PayUrl;
    @Value("${vnpay.returnUrl}") private String vnp_ReturnUrl;

    // Hàm hỗ trợ tái sử dụng để lấy NhanVien đang đăng nhập
    private NhanVien getLoggedInNhanVien() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            return nvRepo.findByTaiKhoan_TenDangNhap(username).orElse(null);
        }
        return null;
    }

    // ==========================================================
    // 1. TÌM KIẾM SẢN PHẨM & LẤY HÓA ĐƠN
    // ==========================================================

    @GetMapping("/products/search")
    public List<SanPhamChiTiet> searchProducts(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Integer color,
            @RequestParam(required = false) Integer size) {

        List<SanPhamChiTiet> list = variantRepo.findAvailableProducts(q, color, size);

        for (SanPhamChiTiet spct : list) {
            BigDecimal giaGoc = spct.getGiaBan();
            BigDecimal giaSauKM = giaGoc;

            List<KhuyenMai> activeSales = khuyenMaiRepo.findActiveKhuyenMaiBySanPhamId(spct.getSanPham().getId());

            if (activeSales != null && !activeSales.isEmpty()) {
                KhuyenMai km = activeSales.get(0);
                if ("PERCENT".equals(km.getLoai())) {
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

    @GetMapping("/invoices/pending")
    public List<HoaDon> getPendingInvoices() {
        return hoaDonRepo.findByLoaiHoaDonAndTrangThaiHoaDonMa(0, "CHO_THANH_TOAN");
    }

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
    // 2. CHỈ VALIDATE, KHÔNG LƯU VÀO DB KHI ĐANG Ở TRẠNG THÁI CHỜ
    // ==========================================================

    @PostMapping("/invoices/{id}/add-product")
    public ResponseEntity<?> addProductToInvoice(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Integer spctId = (Integer) body.get("spctId");
        Integer qty = (Integer) body.get("qty");

        SanPhamChiTiet spct = variantRepo.findById(spctId).orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // CHỈ KIỂM TRA TỒN KHO - TUYỆT ĐỐI KHÔNG LƯU DB
        if (spct.getSoLuong() < qty) {
            return ResponseEntity.badRequest().body(Map.of("message", "Sản phẩm không đủ số lượng!"));
        }

        return ResponseEntity.ok(Map.of("message", "Sản phẩm hợp lệ"));
    }

    @PatchMapping("/invoices/{id}/update-quantity")
    public ResponseEntity<?> updateQuantity(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Integer spctId = (Integer) body.get("spctId");
        Integer newQty = (Integer) body.get("newQty");

        SanPhamChiTiet spct = variantRepo.findById(spctId).orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // CHỈ KIỂM TRA TỒN KHO - TUYỆT ĐỐI KHÔNG LƯU DB
        if (spct.getSoLuong() < newQty) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không đủ hàng trong kho"));
        }

        return ResponseEntity.ok(Map.of("message", "Cập nhật hợp lệ"));
    }

    @DeleteMapping("/invoices/{id}/remove-product/{spctId}")
    public ResponseEntity<?> removeProduct(@PathVariable Integer id, @PathVariable Integer spctId) {
        // KHÔNG LÀM GÌ DB CẢ VÌ CHƯA HỀ ĐƯỢC LƯU
        return ResponseEntity.ok(Map.of("message", "Xóa hợp lệ"));
    }


    // ==========================================================
    // 3. CHỐT ĐƠN (TIỀN MẶT / CHUYỂN KHOẢN TRỰC TIẾP)
    // -> BÂY GIỜ MỚI ĐƯỢC PHÉP TRỪ KHO VÀ LƯU VÀO DB
    // ==========================================================

    @PostMapping("/finish")
    @Transactional
    public ResponseEntity<?> finishOrder(@RequestBody Map<String, Object> payload) {
        try {
            Integer invoiceId = (Integer) payload.get("invoiceId");
            Integer customerId = (Integer) payload.get("customerId");
            BigDecimal total = new BigDecimal(payload.get("total").toString());
            String method = (String) payload.get("method");
            Integer voucherId = (Integer) payload.get("voucherId");
            List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");

            HoaDon hd = hoaDonRepo.findById(invoiceId).orElseThrow(() -> new RuntimeException("Không tìm thấy HD!"));

            if (customerId != null) {
                hd.setKhachHang(khachHangRepo.findById(customerId).orElse(null));
            }

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

            NhanVien nvChotDon = getLoggedInNhanVien();
            if (hd.getNhanVien() == null && nvChotDon != null) {
                hd.setNhanVien(nvChotDon);
            }

            BigDecimal giaTamTinh = BigDecimal.ZERO;
            if (cart != null && !cart.isEmpty()) {
                // TIẾN HÀNH TRỪ KHO VÀ LƯU CHI TIẾT
                for (Map<String, Object> item : cart) {
                    Integer spctId = Integer.valueOf(item.get("id").toString());
                    Integer qty = Integer.valueOf(item.get("qty").toString());
                    BigDecimal price = new BigDecimal(item.get("price").toString());

                    SanPhamChiTiet spct = variantRepo.findById(spctId).orElseThrow(() -> new RuntimeException("Lỗi kho"));
                    if (spct.getSoLuong() < qty) {
                        throw new RuntimeException("Sản phẩm không đủ số lượng để chốt đơn!");
                    }
                    spct.setSoLuong(spct.getSoLuong() - qty);
                    if (spct.getSoLuong() <= 0) spct.setTrangThai(false);
                    variantRepo.save(spct);

                    hdctRepo.save(HoaDonChiTiet.builder().hoaDon(hd).sanPhamChiTiet(spct).soLuong(qty).giaTien(price).build());
                    giaTamTinh = giaTamTinh.add(price.multiply(BigDecimal.valueOf(qty)));
                }
            }

            // LƯU TỔNG TIỀN VÀ ĐỔI TRẠNG THÁI THÀNH HOÀN THÀNH
            hd.setGiaTamThoi(giaTamTinh);
            hd.setGiaTong(total);
            hd.setNgayThanhToan(new Date());
            hd.setPhuongThucThanhToan(method);
            TrangThaiHoaDon ttHoanThanh = trangThaiRepo.findByMa("HOAN_THANH");
            hd.setTrangThaiHoaDon(ttHoanThanh);
            hoaDonRepo.save(hd);

            thanhToanRepo.save(ThanhToan.builder().hoaDon(hd).soTien(total).phuongThuc(method).nhanVien(nvChotDon).build());
            lichSuRepo.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(ttHoanThanh).ghiChu("Thanh toán trực tiếp").nhanVien(nvChotDon).build());

            return ResponseEntity.ok(Map.of("message", "Thành công!", "ma", hd.getMa()));
        } catch (Exception e) {
            e.printStackTrace();
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống"));
        }
    }

    // ==========================================================
    // 4. XỬ LÝ VÍ MOMO VÀ VNPAY TẠO URL (VẪN KHÔNG LƯU DB)
    // ==========================================================

    @PostMapping("/momo-payment/{invoiceId}")
    @Transactional
    public ResponseEntity<?> getMomoUrl(@PathVariable Integer invoiceId, @RequestBody Map<String, Object> payload) {
        try {
            HoaDon hd = hoaDonRepo.findById(invoiceId).orElseThrow(() -> new RuntimeException("Không tìm thấy HD"));
            BigDecimal totalAmount = new BigDecimal(payload.get("amount").toString());
            Integer customerId = (Integer) payload.get("customerId");

            if (customerId != null) hd.setKhachHang(khachHangRepo.findById(customerId).orElse(null));
            hd.setPhuongThucThanhToan("MOMO");
            hoaDonRepo.save(hd); // Chỉ lưu lại khách hàng và phương thức, GIA_TONG vẫn bằng 0

            pendingCarts.put(hd.getMa(), payload);

            String rId = String.valueOf(System.currentTimeMillis());
            String oId = hd.getMa() + "_" + rId;
            String amountStr = totalAmount.setScale(0, RoundingMode.HALF_UP).toString();
            String rawHash = "accessKey=" + accessKey.trim() + "&amount=" + amountStr + "&extraData=&ipnUrl=" + notifyUrl.trim() + "&orderId=" + oId + "&orderInfo=ThanhToan_" + hd.getMa() + "&partnerCode=" + partnerCode.trim() + "&redirectUrl=" + returnUrl.trim() + "&requestId=" + rId + "&requestType=captureWallet";
            String signature = MomoSecurity.signHmacSHA256(rawHash, secretKey.trim());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("partnerCode", partnerCode.trim());
            body.put("accessKey", accessKey.trim());
            body.put("requestId", rId);
            body.put("amount", Long.valueOf(amountStr));
            body.put("orderId", oId);
            body.put("orderInfo", "ThanhToan_" + hd.getMa());
            body.put("redirectUrl", returnUrl.trim());
            body.put("ipnUrl", notifyUrl.trim());
            body.put("extraData", "");
            body.put("requestType", "captureWallet");
            body.put("signature", signature);
            body.put("lang", "vi");

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.postForObject(endpoint.trim(), body, Map.class);
            return ResponseEntity.ok(Map.of("payUrl", response.get("payUrl")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/vnpay-payment/{invoiceId}")
    @Transactional
    public ResponseEntity<?> getVnPayUrl(@PathVariable Integer invoiceId, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            HoaDon hd = hoaDonRepo.findById(invoiceId).orElseThrow(() -> new RuntimeException("Không tìm thấy HD"));
            BigDecimal totalAmount = new BigDecimal(payload.get("amount").toString());
            Integer customerId = (Integer) payload.get("customerId");

            if (customerId != null) hd.setKhachHang(khachHangRepo.findById(customerId).orElse(null));
            hd.setPhuongThucThanhToan("VNPAY");
            hoaDonRepo.save(hd); // Tương tự, GIA_TONG vẫn bằng 0

            pendingCarts.put(hd.getMa(), payload);

            String vnp_TxnRef = hd.getMa() + "_" + System.currentTimeMillis();
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode.trim());
            vnp_Params.put("vnp_Amount", String.valueOf(totalAmount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)));
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
                    if (itr.hasNext()) { query.append('&'); hashData.append('&'); }
                }
            }

            String vnp_SecureHash = VnPayUtil.hmacSHA512(vnp_HashSecret.trim(), hashData.toString());
            String paymentUrl = vnp_PayUrl.trim() + "?" + query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;

            return ResponseEntity.ok(Map.of("payUrl", paymentUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ==========================================================
    // 5. CALLBACK TỪ NGÂN HÀNG (KHI THÀNH CÔNG MỚI LƯU DB)
    // ==========================================================

    @PostMapping("/confirm-online-payment")
    @Transactional
    public ResponseEntity<?> confirmOnlinePayment(@RequestBody Map<String, String> body) {
        String maHD = body.get("maHD");
        HoaDon hd = hoaDonRepo.findByMa(maHD);

        if (hd != null && !"HOAN_THANH".equals(hd.getTrangThaiHoaDon().getMa())) {
            Map<String, Object> payload = pendingCarts.get(maHD);
            if (payload != null) {
                Object voucherIdObj = payload.get("voucherId");
                if (voucherIdObj != null) {
                    Integer vId = Integer.parseInt(voucherIdObj.toString());
                    MaGiamGia voucher = maGiamGiaRepo.findById(vId).orElse(null);
                    if (voucher != null) {
                        hd.setMaGiamGia(voucher);
                        int luotMoi = voucher.getLuotSuDung() + 1;
                        voucher.setLuotSuDung(luotMoi);
                        if (luotMoi >= voucher.getSoLuong()) voucher.setTrangThai(false);
                        maGiamGiaRepo.save(voucher);
                    }
                }

                List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");
                BigDecimal giaTamTinh = BigDecimal.ZERO;

                if (cart != null && hdctRepo.findByHoaDonId(hd.getId()).isEmpty()) {
                    for (Map<String, Object> item : cart) {
                        SanPhamChiTiet spct = variantRepo.findById(Integer.valueOf(item.get("id").toString())).get();
                        int qty = Integer.valueOf(item.get("qty").toString());
                        BigDecimal price = new BigDecimal(item.get("price").toString());

                        spct.setSoLuong(spct.getSoLuong() - qty);
                        if(spct.getSoLuong() <= 0) spct.setTrangThai(false);
                        variantRepo.save(spct);

                        hdctRepo.save(HoaDonChiTiet.builder().hoaDon(hd).sanPhamChiTiet(spct).soLuong(qty).giaTien(price).build());
                        giaTamTinh = giaTamTinh.add(price.multiply(BigDecimal.valueOf(qty)));
                    }
                }

                BigDecimal totalAmount = new BigDecimal(payload.get("amount").toString());
                hd.setGiaTamThoi(giaTamTinh);
                hd.setGiaTong(totalAmount);
                hd.setNgayThanhToan(new Date());
                hd.setTrangThaiHoaDon(trangThaiRepo.findByMa("HOAN_THANH"));
                hoaDonRepo.save(hd);

                thanhToanRepo.save(ThanhToan.builder()
                        .hoaDon(hd).soTien(totalAmount).phuongThuc(hd.getPhuongThucThanhToan())
                        .trangThai("THANH_CONG").nhanVien(getLoggedInNhanVien()).build());

                lichSuRepo.save(LichSuHoaDon.builder()
                        .hoaDon(hd).trangThaiHoaDon(hd.getTrangThaiHoaDon())
                        .ghiChu(hd.getPhuongThucThanhToan() + ": Thanh toán Online thành công").nhanVien(getLoggedInNhanVien()).build());
            }
            pendingCarts.remove(maHD);
        }
        return ResponseEntity.ok(Map.of("message", "Đã xác nhận thanh toán"));
    }

    @PostMapping("/momo-callback")
    @Transactional
    public ResponseEntity<Void> momoCallback(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Object resultCodeObj = params.get("resultCode");
        String resultCode = (resultCodeObj != null) ? resultCodeObj.toString() : "-1";
        String fullOrderId = (String) params.get("orderId");
        String queryString = request.getQueryString();
        String redirectUrl = "http://localhost:8080/admin?" + (queryString != null ? queryString : "");

        if (fullOrderId != null) {
            String maHD = fullOrderId.split("_")[0];
            HoaDon hd = hoaDonRepo.findByMa(maHD);

            if (hd != null && "0".equals(resultCode) && !"HOAN_THANH".equals(hd.getTrangThaiHoaDon().getMa())) {
                Map<String, Object> payload = pendingCarts.get(maHD);
                if (payload != null) {
                    if (hdctRepo.findByHoaDonId(hd.getId()).isEmpty()) {
                        List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");
                        if (cart != null) {
                            for (Map<String, Object> item : cart) {
                                SanPhamChiTiet spct = variantRepo.findById(Integer.valueOf(item.get("id").toString())).get();
                                int qty = Integer.valueOf(item.get("qty").toString());
                                spct.setSoLuong(spct.getSoLuong() - qty);
                                if(spct.getSoLuong() <= 0) spct.setTrangThai(false);
                                variantRepo.save(spct);
                                hdctRepo.save(HoaDonChiTiet.builder().hoaDon(hd).sanPhamChiTiet(spct).soLuong(qty).giaTien(new BigDecimal(item.get("price").toString())).build());
                            }
                        }
                    }

                    BigDecimal totalAmount = new BigDecimal(payload.get("amount").toString());
                    hd.setTrangThaiHoaDon(trangThaiRepo.findByMa("HOAN_THANH"));
                    hd.setGiaTong(totalAmount);
                    hd.setNgayThanhToan(new Date());
                    hoaDonRepo.save(hd);

                    String transId = params.get("transId") != null ? params.get("transId").toString() : "";
                    thanhToanRepo.save(ThanhToan.builder().hoaDon(hd).soTien(hd.getGiaTong()).phuongThuc("MOMO").maGiaoDich(transId).trangThai("THANH_CONG").nhanVien(getLoggedInNhanVien()).build());
                    lichSuRepo.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(hd.getTrangThaiHoaDon()).ghiChu("MoMo: Thanh toán thành công").nhanVien(getLoggedInNhanVien()).build());
                }
            }
            pendingCarts.remove(maHD);
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
    }

    @GetMapping("/vnpay-callback")
    @Transactional
    public ResponseEntity<Void> vnpayCallback(@RequestParam Map<String, String> params, HttpServletRequest request) {
        String responseCode = params.get("vnp_ResponseCode");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String queryString = request.getQueryString();
        String redirectUrl = "http://localhost:8080/admin?" + (queryString != null ? queryString : "");

        if (vnp_TxnRef != null) {
            String maHD = vnp_TxnRef.split("_")[0];
            HoaDon hd = hoaDonRepo.findByMa(maHD);

            if ("00".equals(responseCode) && hd != null && !"HOAN_THANH".equals(hd.getTrangThaiHoaDon().getMa())) {
                Map<String, Object> payload = pendingCarts.get(maHD);
                if (payload != null) {
                    if (hdctRepo.findByHoaDonId(hd.getId()).isEmpty()) {
                        List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");
                        if (cart != null) {
                            for (Map<String, Object> item : cart) {
                                SanPhamChiTiet spct = variantRepo.findById(Integer.valueOf(item.get("id").toString())).get();
                                int qty = Integer.valueOf(item.get("qty").toString());
                                spct.setSoLuong(spct.getSoLuong() - qty);
                                if(spct.getSoLuong() <= 0) spct.setTrangThai(false);
                                variantRepo.save(spct);
                                hdctRepo.save(HoaDonChiTiet.builder().hoaDon(hd).sanPhamChiTiet(spct).soLuong(qty).giaTien(new BigDecimal(item.get("price").toString())).build());
                            }
                        }
                    }

                    BigDecimal totalAmount = new BigDecimal(payload.get("amount").toString());
                    hd.setTrangThaiHoaDon(trangThaiRepo.findByMa("HOAN_THANH"));
                    hd.setGiaTong(totalAmount);
                    hd.setNgayThanhToan(new Date());
                    hoaDonRepo.save(hd);

                    thanhToanRepo.save(ThanhToan.builder().hoaDon(hd).soTien(hd.getGiaTong()).phuongThuc("VNPAY").maGiaoDich(params.get("vnp_TransactionNo")).trangThai("THANH_CONG").nhanVien(getLoggedInNhanVien()).build());
                    lichSuRepo.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(hd.getTrangThaiHoaDon()).ghiChu("VNPAY: Thanh toán thành công (Mã GD: " + params.get("vnp_TransactionNo") + ")").nhanVien(getLoggedInNhanVien()).build());
                }
            }
            pendingCarts.remove(maHD);
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
    }

    // ==========================================================
    // 6. CÁC HÀM TIỆN ÍCH KHÁC (VOUCHER, CUSTOMER, DELETE)
    // ==========================================================

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

    @DeleteMapping("/invoices/{id}")
    @Transactional
    public ResponseEntity<?> deleteInvoice(@PathVariable Integer id) {
        Optional<HoaDon> hdOpt = hoaDonRepo.findById(id);

        if (hdOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Hóa đơn không còn tồn tại trên hệ thống"));
        }

        HoaDon hd = hdOpt.get();
        TrangThaiHoaDon trangThaiHuy = trangThaiRepo.findByMa("DA_HUY");
        if (trangThaiHuy == null) {
            trangThaiHuy = trangThaiRepo.findByMa("HUY_DON");
        }
        hd.setTrangThaiHoaDon(trangThaiHuy);
        // HOÀN TOÀN KHÔNG HOÀN KHO DB NỮA DO DB CHƯA TỪNG BỊ TRỪ

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

        pendingCarts.remove(hd.getMa());

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

    @GetMapping("/attributes")
    public Map<String, Object> getAttributes() {
        return Map.of(
                "colors", mauSacRepo.findAll(),
                "sizes", kichThuocRepo.findAll()
        );
    }
}