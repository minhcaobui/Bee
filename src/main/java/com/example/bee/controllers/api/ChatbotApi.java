package com.example.bee.controllers.api;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotApi {

    // 🌟 LƯU Ý: CHỈ COPY ĐOẠN CHỮ API KEY, KHÔNG COPY DÍNH ĐỊNH DẠNG LINK
    private final String[] API_KEYS = {
            "AIzaSyB0ZZfC88hr1fGAU0TMZH7Ribx1bi0JcxQ"
    };

    @Autowired
    private EntityManager entityManager;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askBot(@RequestBody Map<String, String> payload) {
        String userMessage = payload.getOrDefault("message", "").trim();
        Map<String, String> result = new HashMap<>();

        if (userMessage.isEmpty()) {
            result.put("reply", "Cậu muốn hỏi gì cứ nhắn mình nha! \uD83D\uDC1D");
            return ResponseEntity.ok(result);
        }

        int randomIndex = new java.util.Random().nextInt(API_KEYS.length);
        String selectedKey = API_KEYS[randomIndex].trim(); // Trim khoảng trắng thừa
        String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + selectedKey;

        String categoriesContext = fetchCategories();
        String attributesContext = fetchColorsAndSizes();
        String productsContext = fetchProductsContext();
        String voucherContext = fetchVouchers();
        String promotionContext = fetchPromotions();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Bạn là 'Bee', nhân viên tư vấn cực kỳ dễ thương, chuyên nghiệp của shop thời trang nam nữ BeeMate. ");
        promptBuilder.append("Xưng hô là 'mình' hoặc 'shop' và gọi khách là 'cậu' hoặc 'bạn'. ");
        promptBuilder.append("Giọng điệu: Ngắn gọn (tối đa 4-5 câu), súc tích, nhiệt tình, thỉnh thoảng dùng icon \uD83D\uDC1D, ✨, \uD83D\uDC57, \uD83D\uDC56.\n\n");

        promptBuilder.append("QUY TẮC BẮT BUỘC (TUYỆT ĐỐI TUÂN THỦ):\n");
        promptBuilder.append("1. KHÔNG dùng Markdown (*, **, [], etc) để tạo link. Muốn in đậm hãy dùng thẻ <b>nội dung</b>.\n");
        promptBuilder.append("2. CỰC KỲ QUAN TRỌNG: Khi giới thiệu sản phẩm, BẮT BUỘC dùng thẻ RAW HTML chính xác theo cú pháp sau (KHÔNG bọc trong markdown code block, KHÔNG dùng &lt;):\n");
        promptBuilder.append("<a href='/customer#detail?id=ID_SẢN_PHẨM' target='_blank' style='color:#2563eb; font-weight:bold; text-decoration:underline;'>Tên Sản Phẩm</a>\n");
        promptBuilder.append("3. Chỉ tư vấn dựa trên thông tin Data dưới đây. Nếu khách hỏi sản phẩm không có trong Data, hãy xin lỗi và gợi ý sản phẩm khác trong danh sách.\n");
        promptBuilder.append("4. Phí ship: 30k-50k toàn quốc. FREESHIP cho đơn hàng từ 5000k trở lên. Chính sách đổi trả: Trong vòng 7 ngày (yêu cầu giữ nguyên tem mác).\n\n");

        promptBuilder.append("====== DATA TỪ DATABASE CỦA BEEMATE ======\n");
        promptBuilder.append(categoriesContext).append("\n");
        promptBuilder.append(attributesContext).append("\n");
        promptBuilder.append(productsContext).append("\n");
        promptBuilder.append(voucherContext).append("\n");
        promptBuilder.append(promotionContext).append("\n");
        promptBuilder.append("==========================================\n\n");

        promptBuilder.append("Khách hàng nhắn: \"").append(userMessage).append("\"\n");
        promptBuilder.append("Bee trả lời:");

        // 🌟 SỬ DỤNG HASHMAP ĐỂ CHỐNG GÃY CHUỖI JSON
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", promptBuilder.toString());

        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", Collections.singletonList(textPart));

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("contents", Collections.singletonList(parts));

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBodyMap, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_URL, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    if (content != null && content.containsKey("parts")) {
                        List<Map<String, Object>> responseParts = (List<Map<String, Object>>) content.get("parts");
                        if (!responseParts.isEmpty()) {
                            String botReply = (String) responseParts.get(0).get("text");

                            // 🌟 TIỀN XỬ LÝ (CLEAN) KẾT QUẢ TỪ AI
                            // 1. Xóa markdown code block nếu AI lỡ bọc vào
                            botReply = botReply.replace("```html", "").replace("```", "");

                            // 2. Mở khóa thẻ HTML nếu bị AI escape thành &lt;
                            botReply = botReply.replace("&lt;", "<").replace("&gt;", ">");

                            // 3. Chuyển in đậm markdown (**text**) sang thẻ <b>text</b>
                            botReply = botReply.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");

                            // 4. Chuyển gạch đầu dòng dạng * sang •
                            botReply = botReply.replaceAll("(^|\\n)\\*\\s", "$1<br>• ");

                            // 5. Biến các dấu xuống dòng thực tế thành <br>
                            botReply = botReply.replace("\n", "<br>");

                            result.put("reply", botReply.trim());
                            return ResponseEntity.ok(result);
                        }
                    }
                }
            }

            result.put("reply", "Hệ thống AI đang lọc câu trả lời. Cậu thử hỏi câu khác nhé! \uD83D\uDC1D");
            return ResponseEntity.ok(result);

        } catch (HttpClientErrorException.TooManyRequests e) {
            result.put("reply", "Ui chà, cậu nhắn nhanh quá làm mình bị quá tải mất rồi! Đợi mình 1 phút rồi nhắn lại nha! \uD83D\uDC1D");
            return ResponseEntity.ok(result);
        } catch (HttpServerErrorException.ServiceUnavailable e) {
            result.put("reply", "Xin lỗi cậu, hệ thống trí tuệ nhân tạo (AI) đang bị quá tải nên mình chưa trả lời được ngay. Cậu đợi một chút xíu rồi nhắn lại nhé! \uD83D\uDEE0️");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("reply", "Xin lỗi cậu, hệ thống chat đang bảo trì. Cậu liên hệ hotline hoặc Fanpage BeeMate nhé! \uD83D\uDEE0️");
            return ResponseEntity.ok(result);
        }
    }

    private String fetchCategories() {
        try {
            String sql = "SELECT ten FROM danh_muc WHERE trang_thai = 1";
            List<String> list = entityManager.createNativeQuery(sql).getResultList();
            if (list.isEmpty()) return "";
            return "1. DANH MỤC SHOP BÁN: " + String.join(", ", list) + ".";
        } catch (Exception e) {
            return "";
        }
    }

    private String fetchColorsAndSizes() {
        try {
            String sqlColor = "SELECT ten FROM mau_sac WHERE trang_thai = 1";
            List<String> colors = entityManager.createNativeQuery(sqlColor).getResultList();

            String sqlSize = "SELECT ten FROM kich_thuoc WHERE trang_thai = 1";
            List<String> sizes = entityManager.createNativeQuery(sqlSize).getResultList();

            return "2. THUỘC TÍNH SẢN PHẨM HIỆN CÓ:\n" +
                    "- Các Màu sắc: " + String.join(", ", colors) + "\n" +
                    "- Các Kích cỡ (Size): " + String.join(", ", sizes) + ".";
        } catch (Exception e) {
            return "";
        }
    }

    private String fetchProductsContext() {
        StringBuilder sb = new StringBuilder("3. TOP SẢN PHẨM BÁN CHẠY & MỚI NHẤT:\n");
        try {
            // 🌟 ĐÃ FIX LỖI SQL GROUP BY / ORDER BY
            String sql = "SELECT TOP 40 sp.id, sp.ten, dm.ten AS danh_muc, MIN(spct.gia_ban) AS gia_min, MAX(spct.gia_ban) AS gia_max " +
                    "FROM san_pham sp " +
                    "JOIN san_pham_chi_tiet spct ON sp.id = spct.id_san_pham " +
                    "LEFT JOIN danh_muc dm ON sp.id_danh_muc = dm.id " +
                    "WHERE sp.trang_thai = 1 AND spct.trang_thai = 1 " +
                    "GROUP BY sp.id, sp.ten, dm.ten, sp.ngay_tao " +
                    "ORDER BY sp.ngay_tao DESC";

            List<Object[]> listSp = entityManager.createNativeQuery(sql).getResultList();
            if (listSp.isEmpty()) return sb.append("- Tạm thời hết hàng.\n").toString();

            for (Object[] row : listSp) {
                sb.append("- ID: ").append(row[0])
                        .append(" | Tên: ").append(row[1])
                        .append(" | Loại: ").append(row[2] != null ? row[2] : "Khác")
                        .append(" | Giá: Từ ").append(row[3]).append(" - ").append(row[4]).append(" VNĐ\n");
            }
        } catch (Exception e) {
            System.out.println("Lỗi DB Sản phẩm: " + e.getMessage());
        }
        return sb.toString();
    }

    private String fetchVouchers() {
        StringBuilder sb = new StringBuilder("4. MÃ GIẢM GIÁ (VOUCHER) ĐANG HOẠT ĐỘNG:\n");
        try {
            // 🌟 ĐÃ FIX LỖI TÊN CỘT THEO ĐÚNG DATABASE
            String sql = "SELECT ma_code, ten, dieu_kien, gia_tri_giam_gia, loai_giam_gia FROM ma_giam_gia " +
                    "WHERE trang_thai = 1 AND so_luong > luot_su_dung AND (ngay_ket_thuc IS NULL OR ngay_ket_thuc >= GETDATE())";
            List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
            if (list.isEmpty()) return sb.append("- Hiện không có mã giảm giá nào.\n").toString();

            for (Object[] row : list) {
                String loaiGiam = String.valueOf(row[4]).contains("PERCENT") ? "%" : " VNĐ";
                sb.append("- Mã: ").append(row[0])
                        .append(" | Giảm: ").append(row[3]).append(loaiGiam)
                        .append(" | Điều kiện: Đơn tối thiểu ").append(row[2]).append(" VNĐ\n");
            }
        } catch (Exception e) {
            System.out.println("Lỗi DB Voucher: " + e.getMessage());
        }
        return sb.toString();
    }

    private String fetchPromotions() {
        StringBuilder sb = new StringBuilder("5. ĐỢT KHUYẾN MÃI (SALE) & SẢN PHẨM ÁP DỤNG:\n");
        try {
            String sql = "SELECT km.ten, km.loai, km.gia_tri, STRING_AGG(sp.ten, ', ') " +
                    "FROM khuyen_mai km " +
                    "LEFT JOIN khuyen_mai_san_pham kmsp ON km.id = kmsp.id_khuyen_mai " +
                    "LEFT JOIN san_pham sp ON kmsp.id_san_pham = sp.id " +
                    "WHERE km.trang_thai = 1 AND (km.ngay_ket_thuc IS NULL OR km.ngay_ket_thuc >= GETDATE()) " +
                    "GROUP BY km.id, km.ten, km.loai, km.gia_tri";
            List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
            if (list.isEmpty()) return sb.append("- Không có chương trình Sale nào.\n").toString();

            for (Object[] row : list) {
                String loaiGiam = String.valueOf(row[1]).equalsIgnoreCase("PERCENTAGE") || String.valueOf(row[1]).contains("%") ? "%" : " VNĐ";
                String spApDung = (row[3] != null && !String.valueOf(row[3]).trim().isEmpty()) ? String.valueOf(row[3]) : "Tất cả sản phẩm";
                sb.append("- Tên chương trình: ").append(row[0])
                        .append(" | Mức giảm: ").append(row[2]).append(loaiGiam)
                        .append(" | Áp dụng cho: ").append(spApDung).append("\n");
            }
        } catch (Exception e) {
            System.out.println("Lỗi DB Sale: " + e.getMessage());
        }
        return sb.toString();
    }
}