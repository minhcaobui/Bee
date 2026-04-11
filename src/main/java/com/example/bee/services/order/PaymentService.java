package com.example.bee.services;

import com.example.bee.entities.cart.GioHang;
import com.example.bee.entities.cart.GioHangChiTiet;
import com.example.bee.entities.notification.ThongBao;
import com.example.bee.entities.order.HoaDon;
import com.example.bee.entities.order.HoaDonChiTiet;
import com.example.bee.entities.order.LichSuHoaDon;
import com.example.bee.entities.order.ThanhToan;
import com.example.bee.entities.order.TrangThaiHoaDon;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.repositories.cart.GioHangChiTietRepository;
import com.example.bee.repositories.cart.GioHangRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
import com.example.bee.repositories.order.HoaDonChiTietRepository;
import com.example.bee.repositories.order.HoaDonRepository;
import com.example.bee.repositories.order.LichSuHoaDonRepository;
import com.example.bee.repositories.order.ThanhToanRepository;
import com.example.bee.repositories.order.TrangThaiHoaDonRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import com.example.bee.utils.MomoSecurity;
import com.example.bee.utils.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final HoaDonRepository hoaDonRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final TrangThaiHoaDonRepository trangThaiHoaDonRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final MaGiamGiaRepository maGiamGiaRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;

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

    // Thay vì fix cứng returnUrl, ta định nghĩa url callback về API chung
    private final String DOMAIN = "http://beemate.store";

    @Transactional
    public String getMomoUrl(Integer invoiceId) throws Exception {
        HoaDon hd = hoaDonRepository.findById(invoiceId).orElseThrow(() -> new RuntimeException("Không tìm thấy Hóa đơn"));
        BigDecimal totalAmount = hd.getGiaTong();

        List<ThanhToan> ttMomoList = thanhToanRepository.findByHoaDon_Id(hd.getId());
        if (ttMomoList != null && !ttMomoList.isEmpty()) {
            ThanhToan ttMomo = ttMomoList.get(0);
            ttMomo.setPhuongThuc("MOMO");
            thanhToanRepository.save(ttMomo);
        } else {
            ThanhToan ttMomo = new ThanhToan();
            ttMomo.setHoaDon(hd);
            ttMomo.setSoTien(totalAmount);
            ttMomo.setPhuongThuc("MOMO");
            ttMomo.setLoaiThanhToan("THANH_TOAN");
            ttMomo.setTrangThai("CHO_THANH_TOAN");
            ttMomo.setNgayThanhToan(new Date());
            thanhToanRepository.save(ttMomo);
        }

        String rId = String.valueOf(System.currentTimeMillis());
        String oId = hd.getMa() + "_" + rId;
        String amountStr = totalAmount.setScale(0, RoundingMode.HALF_UP).toString();

        // Cả online và POS đều trỏ về 1 callback duy nhất này
        String returnUrl = DOMAIN + "/api/payment/momo-callback";

        String rawHash = "accessKey=" + accessKey.trim() + "&amount=" + amountStr + "&extraData=&ipnUrl=" + notifyUrl.trim() + "&orderId=" + oId + "&orderInfo=ThanhToan_" + hd.getMa() + "&partnerCode=" + partnerCode.trim() + "&redirectUrl=" + returnUrl + "&requestId=" + rId + "&requestType=captureWallet";
        String signature = MomoSecurity.signHmacSHA256(rawHash, secretKey.trim());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode.trim());
        body.put("accessKey", accessKey.trim());
        body.put("requestId", rId);
        body.put("amount", Long.valueOf(amountStr));
        body.put("orderId", oId);
        body.put("orderInfo", "ThanhToan_" + hd.getMa());
        body.put("redirectUrl", returnUrl);
        body.put("ipnUrl", notifyUrl.trim());
        body.put("extraData", "");
        body.put("requestType", "captureWallet");
        body.put("signature", signature);
        body.put("lang", "vi");

        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.postForObject(endpoint.trim(), body, Map.class);
        return response.get("payUrl").toString();
    }

    @Transactional
    public String momoCallback(HttpServletRequest request) {
        String resultCode = request.getParameter("resultCode");
        String orderId = request.getParameter("orderId");
        String redirectUrl = DOMAIN + "/customer#home"; // Default fallback

        try {
            if (orderId != null && orderId.contains("_")) {
                String maHD = orderId.split("_")[0];
                HoaDon hd = hoaDonRepository.findByMa(maHD);

                if (hd != null) {
                    boolean isPos = (hd.getLoaiHoaDon() != null && hd.getLoaiHoaDon() == 0);

                    if ("CHO_THANH_TOAN".equals(hd.getTrangThaiHoaDon().getMa())) {
                        if ("0".equals(resultCode)) {
                            // THANH TOÁN THÀNH CÔNG
                            TrangThaiHoaDon nextStatus = isPos ? trangThaiHoaDonRepository.findByMa("HOAN_THANH") : trangThaiHoaDonRepository.findByMa("CHO_XAC_NHAN");
                            hd.setTrangThaiHoaDon(nextStatus);
                            hd.setNgayThanhToan(new Date());
                            if(isPos) hd.setNgayHangSanSang(new Date());
                            hoaDonRepository.save(hd);

                            List<ThanhToan> ttMomoCb = thanhToanRepository.findByHoaDon_Id(hd.getId());
                            if (ttMomoCb != null && !ttMomoCb.isEmpty()) {
                                ThanhToan tt = ttMomoCb.get(0);
                                tt.setTrangThai("THANH_CONG");
                                tt.setMaGiaoDich(orderId);
                                tt.setNgayThanhToan(new Date());
                                thanhToanRepository.save(tt);
                            }

                            // Xóa giỏ hàng nếu là đơn Online
                            boolean isBuyNow = hd.getGhiChu() != null && hd.getGhiChu().contains("[BUY_NOW]");
                            if (!isPos && !isBuyNow && hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                                GioHang gh = gioHangRepository.findByTaiKhoan_Id(hd.getKhachHang().getTaiKhoan().getId()).orElse(null);
                                if (gh != null) {
                                    List<GioHangChiTiet> ghcts = gioHangChiTietRepository.findByGioHang_Id(gh.getId());
                                    gioHangChiTietRepository.deleteAll(ghcts);
                                }
                            }

                            lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(nextStatus).ghiChu("Thanh toán MoMo thành công").build());

                            if (!isPos && hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                                ThongBao tb = new ThongBao();
                                tb.setTaiKhoanId(hd.getKhachHang().getTaiKhoan().getId());
                                tb.setTieuDe("Thanh toán thành công");
                                tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã được thanh toán qua MoMo.");
                                tb.setLoaiThongBao("ORDER");
                                tb.setDaDoc(false);
                                thongBaoRepository.save(tb);
                            }

                            // ĐIỀU HƯỚNG TÙY THEO LOẠI ĐƠN HÀNG
                            redirectUrl = isPos ? (DOMAIN + "/admin#pos") : (DOMAIN + "/customer#account");

                        } else {
                            // THANH TOÁN THẤT BẠI
                            TrangThaiHoaDon ttHuy = trangThaiHoaDonRepository.findByMa("DA_HUY");
                            hd.setTrangThaiHoaDon(ttHuy);
                            hoaDonRepository.save(hd);

                            List<ThanhToan> ttMomoCbFail = thanhToanRepository.findByHoaDon_Id(hd.getId());
                            if (ttMomoCbFail != null && !ttMomoCbFail.isEmpty()) {
                                ThanhToan tt = ttMomoCbFail.get(0);
                                tt.setTrangThai("THAT_BAI");
                                thanhToanRepository.save(tt);
                            }

                            // Hoàn tồn kho
                            List<HoaDonChiTiet> hdctList = hoaDonChiTietRepository.findByHoaDonId(hd.getId());
                            for (HoaDonChiTiet ct : hdctList) {
                                SanPhamChiTiet spct = ct.getSanPhamChiTiet();
                                spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong());
                                sanPhamChiTietRepository.save(spct);
                            }

                            // Hoàn mã giảm giá
                            if (hd.getMaGiamGia() != null) {
                                MaGiamGia voucher = hd.getMaGiamGia();
                                int luotMoi = voucher.getLuotSuDung() - 1;
                                if (luotMoi >= 0) {
                                    voucher.setLuotSuDung(luotMoi);
                                    if (!voucher.getTrangThai() && voucher.getNgayKetThuc().isAfter(java.time.LocalDateTime.now())) {
                                        voucher.setTrangThai(true);
                                    }
                                    maGiamGiaRepository.save(voucher);
                                }
                            }

                            lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(ttHuy).ghiChu("Hủy đơn do khách hàng không hoàn tất thanh toán MoMo").build());

                            // ĐIỀU HƯỚNG TÙY THEO LOẠI ĐƠN HÀNG
                            redirectUrl = isPos ? (DOMAIN + "/admin#pos") : (DOMAIN + "/customer#checkout");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return redirectUrl;
    }

    @Transactional
    public String getVnPayUrl(Integer invoiceId) throws Exception {
        HoaDon hd = hoaDonRepository.findById(invoiceId).orElseThrow(() -> new RuntimeException("Không tìm thấy Hóa đơn"));
        BigDecimal totalAmount = hd.getGiaTong();

        List<ThanhToan> ttVnpayList = thanhToanRepository.findByHoaDon_Id(hd.getId());
        if (ttVnpayList != null && !ttVnpayList.isEmpty()) {
            ThanhToan ttVnpay = ttVnpayList.get(0);
            ttVnpay.setPhuongThuc("VNPAY");
            thanhToanRepository.save(ttVnpay);
        } else {
            ThanhToan ttVnpay = new ThanhToan();
            ttVnpay.setHoaDon(hd);
            ttVnpay.setSoTien(totalAmount);
            ttVnpay.setPhuongThuc("VNPAY");
            ttVnpay.setLoaiThanhToan("THANH_TOAN");
            ttVnpay.setTrangThai("CHO_THANH_TOAN");
            ttVnpay.setNgayThanhToan(new Date());
            thanhToanRepository.save(ttVnpay);
        }

        String vnp_TxnRef = hd.getMa() + "_" + System.currentTimeMillis();
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode.trim());
        vnp_Params.put("vnp_Amount", String.valueOf(totalAmount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "ThanhToan_" + hd.getMa());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");

        // Cả online và POS đều trỏ về chung 1 url callback
        String returnUrl = DOMAIN + "/api/payment/vnpay-callback";
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
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
        return vnp_PayUrl.trim() + "?" + query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;
    }

    @Transactional
    public String vnpayCallback(HttpServletRequest request) {
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String redirectUrl = DOMAIN + "/customer#home";

        try {
            if (vnp_TxnRef != null && vnp_TxnRef.contains("_")) {
                String maHD = vnp_TxnRef.split("_")[0];
                HoaDon hd = hoaDonRepository.findByMa(maHD);

                if (hd != null) {
                    boolean isPos = (hd.getLoaiHoaDon() != null && hd.getLoaiHoaDon() == 0);

                    if ("CHO_THANH_TOAN".equals(hd.getTrangThaiHoaDon().getMa())) {
                        if ("00".equals(vnp_ResponseCode)) {
                            // THÀNH CÔNG
                            TrangThaiHoaDon nextStatus = isPos ? trangThaiHoaDonRepository.findByMa("HOAN_THANH") : trangThaiHoaDonRepository.findByMa("CHO_XAC_NHAN");
                            hd.setTrangThaiHoaDon(nextStatus);
                            hd.setNgayThanhToan(new Date());
                            if(isPos) hd.setNgayHangSanSang(new Date());
                            hoaDonRepository.save(hd);

                            List<ThanhToan> ttVnCb = thanhToanRepository.findByHoaDon_Id(hd.getId());
                            if (ttVnCb != null && !ttVnCb.isEmpty()) {
                                ThanhToan tt = ttVnCb.get(0);
                                tt.setTrangThai("THANH_CONG");
                                tt.setMaGiaoDich(vnp_TxnRef);
                                tt.setNgayThanhToan(new Date());
                                thanhToanRepository.save(tt);
                            }

                            boolean isBuyNow = hd.getGhiChu() != null && hd.getGhiChu().contains("[BUY_NOW]");
                            if (!isPos && !isBuyNow && hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                                GioHang gh = gioHangRepository.findByTaiKhoan_Id(hd.getKhachHang().getTaiKhoan().getId()).orElse(null);
                                if (gh != null) {
                                    List<GioHangChiTiet> ghcts = gioHangChiTietRepository.findByGioHang_Id(gh.getId());
                                    gioHangChiTietRepository.deleteAll(ghcts);
                                }
                            }

                            lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(nextStatus).ghiChu("Thanh toán VNPay thành công").build());

                            if (!isPos && hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                                ThongBao tb = new ThongBao();
                                tb.setTaiKhoanId(hd.getKhachHang().getTaiKhoan().getId());
                                tb.setTieuDe("Thanh toán thành công");
                                tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã được thanh toán qua VNPay.");
                                tb.setLoaiThongBao("ORDER");
                                tb.setDaDoc(false);
                                thongBaoRepository.save(tb);
                            }

                            redirectUrl = isPos ? (DOMAIN + "/admin#pos") : (DOMAIN + "/customer#account");
                        } else {
                            // THẤT BẠI
                            TrangThaiHoaDon ttHuy = trangThaiHoaDonRepository.findByMa("DA_HUY");
                            hd.setTrangThaiHoaDon(ttHuy);
                            hoaDonRepository.save(hd);

                            List<ThanhToan> ttVnCbFail = thanhToanRepository.findByHoaDon_Id(hd.getId());
                            if (ttVnCbFail != null && !ttVnCbFail.isEmpty()) {
                                ThanhToan tt = ttVnCbFail.get(0);
                                tt.setTrangThai("THAT_BAI");
                                thanhToanRepository.save(tt);
                            }

                            List<HoaDonChiTiet> hdctList = hoaDonChiTietRepository.findByHoaDonId(hd.getId());
                            for (HoaDonChiTiet ct : hdctList) {
                                SanPhamChiTiet spct = ct.getSanPhamChiTiet();
                                spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong());
                                sanPhamChiTietRepository.save(spct);
                            }

                            if (hd.getMaGiamGia() != null) {
                                MaGiamGia voucher = hd.getMaGiamGia();
                                int luotMoi = voucher.getLuotSuDung() - 1;
                                if (luotMoi >= 0) {
                                    voucher.setLuotSuDung(luotMoi);
                                    if (!voucher.getTrangThai() && voucher.getNgayKetThuc().isAfter(java.time.LocalDateTime.now())) {
                                        voucher.setTrangThai(true);
                                    }
                                    maGiamGiaRepository.save(voucher);
                                }
                            }

                            lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(ttHuy).ghiChu("Hủy đơn do khách hàng không hoàn tất thanh toán VNPay").build());
                            redirectUrl = isPos ? (DOMAIN + "/admin#pos") : (DOMAIN + "/customer#checkout");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return redirectUrl;
    }
}