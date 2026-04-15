//package com.example.bee.controllers.api;
//
//import com.example.bee.entities.order.HoaDon;
//import com.example.bee.repositories.order.HoaDonRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import javax.crypto.Mac;
//import javax.crypto.spec.SecretKeySpec;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//
//@RestController
//@RequestMapping("/api/payment/sepay")
//public class SePayPaymentApi {
//
//    // 🔴 BẠN HÃY COPY MÃ MERCHANT VÀ SECRET KEY TRÊN MÀN HÌNH SEPAY DÁN VÀO 2 DÒNG NÀY:
//    private static final String MERCHANT_ID = "SP-TEST-XXXXXXXX"; // Ví dụ: SP-TEST-123456
//    private static final String SECRET_KEY = "spsk_test_xxxxxxxxxxqE2z5EFvH8MBUj9ZMbHr3EN"; // Ví dụ: spsk_test_abc123...
//
//    // Đường dẫn gốc của SePay Sandbox
//    private static final String SEPAY_INIT_URL = "https://pay-sandbox.sepay.vn/v1/checkout/init";
//
//    @Autowired
//    private HoaDonRepository hdRepo;
//
//    @PostMapping("/create/{invoiceId}")
//    public ResponseEntity<?> createPaymentLink(@PathVariable Integer invoiceId) {
//        try {
//            HoaDon hd = hdRepo.findById(invoiceId).orElse(null);
//            if (hd == null) {
//                return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy hóa đơn"));
//            }
//
//            // Trang web sẽ tự quay về sau khi khách thanh toán xong hoặc hủy
//            String callbackUrl = "https://beemate.store/customer/#account";
//
//            // 1. Chuẩn bị các tham số bắt buộc để gửi sang SePay
//            Map<String, String> fields = new HashMap<>();
//            fields.put("order_amount", String.valueOf(hd.getTongTien().intValue()));
//            fields.put("merchant", MERCHANT_ID);
//            fields.put("currency", "VND");
//            fields.put("operation", "PURCHASE");
//            fields.put("order_description", "Thanh toan don hang HD" + hd.getId());
//            fields.put("order_invoice_number", "HD" + hd.getId());
//            fields.put("payment_method", "BANK_TRANSFER");
//            fields.put("success_url", callbackUrl);
//            fields.put("error_url", callbackUrl);
//            fields.put("cancel_url", callbackUrl);
//
//            // 2. Trích xuất đúng thứ tự các trường để tạo Chữ ký bảo mật (Signature)
//            String[] allowedFields = {
//                    "order_amount", "merchant", "currency", "operation", "order_description",
//                    "order_invoice_number", "customer_id", "payment_method", "success_url", "error_url", "cancel_url"
//            };
//
//            List<String> signedParams = new ArrayList<>();
//            for (String field : allowedFields) {
//                if (fields.containsKey(field) && fields.get(field) != null) {
//                    signedParams.add(field + "=" + fields.get(field));
//                }
//            }
//            String signedString = String.join(",", signedParams);
//
//            // 3. Mã hóa HMAC-SHA256 theo chuẩn tài liệu SePay
//            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
//            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
//            sha256HMAC.init(secretKey);
//            byte[] hash = sha256HMAC.doFinal(signedString.getBytes(StandardCharsets.UTF_8));
//            String signature = Base64.getEncoder().encodeToString(hash);
//
//            fields.put("signature", signature);
//
//            // 4. Tạo một thẻ Form HTML ẩn để Trình duyệt tự động submit (Giống hệt luồng VNPay)
//            StringBuilder html = new StringBuilder();
//            html.append("<form id='sepayForm' action='").append(SEPAY_INIT_URL).append("' method='POST'>");
//            for (Map.Entry<String, String> entry : fields.entrySet()) {
//                html.append("<input type='hidden' name='").append(entry.getKey()).append("' value='").append(entry.getValue()).append("'/>");
//            }
//            html.append("</form>");
//            html.append("<script>document.getElementById('sepayForm').submit();</script>");
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "html", html.toString()
//            ));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi tạo link SePay: " + e.getMessage()));
//        }
//    }
//}