package com.example.bee.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class MomoSecurity {

    public static String signHmacSHA256(String data, String secretKey) throws Exception {
        try {
            // 1. Khởi tạo thuật toán HmacSHA256 với SecretKey
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);

            // 2. Thực hiện băm dữ liệu (Raw bytes)
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // 3. Chuyển đổi bytes sang Hex string và ép về CHỮ THƯỜNG (Lowercase)
            // MoMo bắt buộc chữ ký phải là chữ thường.
            return toHexString(rawHmac).toLowerCase();
        } catch (Exception e) {
            throw new Exception("Lỗi khi tạo chữ ký MoMo: " + e.getMessage());
        }
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // Cách chuyển byte sang hex chuẩn nhất, tránh mọi lỗi định dạng
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}