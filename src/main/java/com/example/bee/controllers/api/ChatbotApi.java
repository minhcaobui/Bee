package com.example.bee.controllers.api;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotApi {

    @Autowired
    private EntityManager entityManager;

    private final String[] API_KEYS = {
            "AIzaSyBwNQTRdPiX0eaKnsRHHN9ZTCJHJ4q9A9M", // Key 1 (Key hiện tại của bạn)
            "AIzaSyAL1Hx_Gi9HDD5hiZKIJuXb5LI1JOzOD8M",            // Key 2 (Tạo từ Gmail khác)
            "AIzaSyCkNEFWilwtJoi8nRENh7-iaQH1TQw68MU"             // Key 3 (Tạo từ Gmail khác)
    };
//    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askBot(@RequestBody Map<String, String> payload) {
        String userMessage = payload.get("message");

        // 2. RANDOM LẤY 1 KEY TRONG MẢNG ĐỂ VƯỢT GIỚI HẠN (RATE LIMIT)
        int randomIndex = new java.util.Random().nextInt(API_KEYS.length);
        String selectedKey = API_KEYS[randomIndex];
        String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + selectedKey;

        // ==========================================
        // BƯỚC 1: LẤY SẢN PHẨM TỪ DATABASE (Đã sửa LIMIT thành TOP cho SQL Server)
        // ==========================================
        StringBuilder productData = new StringBuilder("DANH SÁCH SẢN PHẨM HIỆN CÓ CỦA SHOP:\n");
        try {
            // Sửa LIMIT 15 thành SELECT TOP 15
            String sql = "SELECT TOP 15 sp.id, sp.ten, MAX(spct.gia_ban) FROM san_pham sp " +
                    "JOIN san_pham_chi_tiet spct ON sp.id = spct.id_san_pham " +
                    "WHERE spct.trang_thai = 1 " +
                    "GROUP BY sp.id, sp.ten";

            Query query = entityManager.createNativeQuery(sql);
            List<Object[]> listSp = query.getResultList();

            for (Object[] row : listSp) {
                productData.append("- ID: ").append(row[0])
                        .append(" | Tên: ").append(row[1])
                        .append(" | Giá: ").append(row[2]).append(" VNĐ\n");
            }
        } catch (Exception e) {
            System.out.println("Lỗi truy vấn DB Sản phẩm: " + e.getMessage());
        }

        // ==========================================
        // BƯỚC 2: LẤY MÃ GIẢM GIÁ (Bảng ma_giam_gia)
        // ==========================================
        StringBuilder voucherData = new StringBuilder("DANH SÁCH MÃ GIẢM GIÁ (VOUCHER) ĐANG CÓ:\n");
        try {
            String sqlVoucher = "SELECT ma_code, ten FROM ma_giam_gia WHERE trang_thai = 1 AND so_luong > luot_su_dung";
            Query queryVoucher = entityManager.createNativeQuery(sqlVoucher);
            List<Object[]> listVoucher = queryVoucher.getResultList();

            if (listVoucher.isEmpty()) {
                voucherData.append("- Hiện tại shop chưa có mã voucher nào mới.\n");
            } else {
                for (Object[] row : listVoucher) {
                    voucherData.append("- Mã: ").append(row[0])
                            .append(" | Ưu đãi: ").append(row[1]).append("\n");
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi truy vấn DB Voucher: " + e.getMessage());
        }

        // ==========================================
        // BƯỚC 3: LẤY ĐỢT KHUYẾN MÃI + SẢN PHẨM ÁP DỤNG (Sửa GROUP_CONCAT thành STRING_AGG)
        // ==========================================
        StringBuilder promotionData = new StringBuilder("DANH SÁCH ĐỢT KHUYẾN MÃI VÀ CÁC SẢN PHẨM ĐƯỢC ÁP DỤNG:\n");
        try {
            // Sửa GROUP_CONCAT thành STRING_AGG chuẩn SQL Server
            String sqlPromo = "SELECT km.ten, km.loai, km.gia_tri, STRING_AGG(sp.ten, ', ') " +
                    "FROM khuyen_mai km " +
                    "LEFT JOIN khuyen_mai_san_pham kmsp ON km.id = kmsp.id_khuyen_mai " +
                    "LEFT JOIN san_pham sp ON kmsp.id_san_pham = sp.id " +
                    "WHERE km.trang_thai = 1 " +
                    "GROUP BY km.id, km.ten, km.loai, km.gia_tri";

            Query queryPromo = entityManager.createNativeQuery(sqlPromo);
            List<Object[]> listPromo = queryPromo.getResultList();

            if (listPromo.isEmpty()) {
                promotionData.append("- Hiện tại chưa có đợt sale sản phẩm nào diễn ra.\n");
            } else {
                for (Object[] row : listPromo) {
                    String loaiGiam = String.valueOf(row[1]).equalsIgnoreCase("PERCENTAGE") || String.valueOf(row[1]).contains("%") ? "%" : " VNĐ";
                    String appliedProducts = (row[3] != null && !String.valueOf(row[3]).trim().isEmpty())
                            ? String.valueOf(row[3])
                            : "Đang cập nhật danh sách sản phẩm";

                    promotionData.append("- Đợt Sale: ").append(row[0])
                            .append(" | Mức giảm: ").append(row[2]).append(loaiGiam)
                            .append(" | Các sản phẩm trong đợt sale: ").append(appliedProducts).append("\n");
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi truy vấn DB KhuyenMai: " + e.getMessage());
        }

        // ==========================================
        // BƯỚC 4: "TRÓI" AI BẰNG LUẬT HIỂN THỊ MỚI
        // ==========================================
        String systemInstruction = "Bạn là nhân viên chăm sóc khách hàng của shop thời trang BeeMate. " +
                "Phí ship là 30k-50k toàn quốc, mua trên 500k freeship. " +
                "Chính sách đổi trả trong 7 ngày nếu giữ nguyên tem mác. " +
                "Hãy trả lời thật ngắn gọn, súc tích, thân thiện, xưng 'shop' hoặc 'mình' và gọi khách là 'bạn' hoặc 'cậu'. " +
                "QUY TẮC TRẢ LỜI VÀ HIỂN THỊ (VÔ CÙNG QUAN TRỌNG): " +
                "1. TUYỆT ĐỐI KHÔNG SỬ DỤNG MARKDOWN (không dùng dấu ** để in đậm, không dùng ngoặc vuông để tạo link). " +
                "2. Nếu muốn in đậm, hãy dùng thẻ HTML <b>nội dung</b>. " +
                "3. Khi khách hỏi TÌM SẢN PHẨM, tạo link bằng cú pháp: <a href='#detail?id=ID_SẢN_PHẨM' style='color:#3D5A4C; font-weight:bold; text-decoration:underline;'>Tên Sản Phẩm</a> " +
                "4. Phân biệt rõ ràng 2 loại ưu đãi: " +
                "  - Nếu khách hỏi 'MÃ GIẢM GIÁ' hay 'VOUCHER', lấy thông tin từ phần 'MÃ GIẢM GIÁ (VOUCHER)' và dặn khách nhập mã ở bước thanh toán. " +
                "  - Nếu khách hỏi 'ĐỢT KHUYẾN MÃI', 'ĐỢT GIẢM GIÁ' hay 'CÓ SALE KHÔNG', lấy thông tin từ phần 'ĐỢT KHUYẾN MÃI VÀ CÁC SẢN PHẨM ĐƯỢC ÁP DỤNG'. " +
                "5. Nếu khách hỏi đợt khuyến mãi/sale đó áp dụng cho những sản phẩm nào, BẮT BUỘC liệt kê các tên sản phẩm có trong cột 'Các sản phẩm trong đợt sale'.\n\n" +
                productData.toString() + "\n\n" + voucherData.toString() + "\n\n" + promotionData.toString();

        String prompt = systemInstruction + "\n\nKhách hàng hỏi: " + userMessage;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // DÙNG MAP ĐỂ CHỐNG LỖI KÝ TỰ JSON
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", Collections.singletonList(textPart));

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("contents", Collections.singletonList(parts));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBodyMap, headers);
        Map<String, String> result = new HashMap<>();

        try {
            // Gửi API sang Google
            ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_URL, request, Map.class);
            Map<String, Object> body = response.getBody();

            // Bóc tách JSON
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> responseParts = (List<Map<String, Object>>) content.get("parts");
            String botReply = (String) responseParts.get(0).get("text");

            result.put("reply", botReply);
            return ResponseEntity.ok(result);

        } catch (HttpClientErrorException.TooManyRequests e) {
            // XỬ LÝ LỖI 429: SPAM QUÁ NHIỀU
            System.out.println("Lỗi 429: Hết lượt gọi API miễn phí trong phút này.");
            result.put("reply", "Ui chà, cậu nhắn nhanh quá làm mình bị quá tải mất rồi! Cậu vui lòng đợi khoảng 1 phút rồi nhắn lại cho mình nhé! 🐝");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // XỬ LÝ CÁC LỖI MẠNG KHÁC
            e.printStackTrace();
            result.put("reply", "Xin lỗi cậu, hệ thống chat đang bảo trì một xíu. Cậu liên hệ hotline hoặc Fanpage BeeMate nhé!");
            return ResponseEntity.ok(result);
        }
    }
}