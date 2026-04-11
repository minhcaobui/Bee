package com.example.bee.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class GhnService {

    @Value("${ghn.api.url.leadtime}")
    private String ghnLeadtimeUrl;

    @Value("${ghn.api.token}")
    private String ghnToken;

    @Value("${ghn.shop.id}")
    private Integer ghnShopId;

    @Value("${ghn.from.district.id}")
    private Integer fromDistrictId;

    @Value("${ghn.from.ward.code}")
    private String fromWardCode;

    // Sử dụng RestTemplate mặc định của Spring
    private final RestTemplate restTemplate;

    public GhnService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Tính toán ngày dự kiến nhận hàng từ GHN
     *
     * @param toDistrictId ID Quận/Huyện của khách
     * @param toWardCode   Mã Phường/Xã của khách
     * @param serviceId    ID Dịch vụ giao hàng (VD: 53320 cho Standard, 53321 cho Nhanh...)
     * @return java.util.Date ngày dự kiến nhận
     */
    public Date calculateExpectedDeliveryDate(Integer toDistrictId, String toWardCode, Integer serviceId) {
        try {
            // 1. Khởi tạo Headers theo yêu cầu của GHN
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Token", ghnToken);
            headers.set("ShopId", String.valueOf(ghnShopId));

            // 2. Khởi tạo Body Payload
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from_district_id", fromDistrictId);
            requestBody.put("from_ward_code", fromWardCode);
            requestBody.put("to_district_id", toDistrictId);
            requestBody.put("to_ward_code", toWardCode);
            requestBody.put("service_id", serviceId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 3. Gọi API POST
            ResponseEntity<Map> response = restTemplate.exchange(
                    ghnLeadtimeUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // 4. Bóc tách dữ liệu JSON trả về
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.get("code").equals(200)) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                if (data != null && data.get("leadtime") != null) {

                    // GHN trả về Unix Timestamp (tính bằng giây). Trong Java, Date dùng mili-giây
                    long leadtimeSeconds = Long.parseLong(data.get("leadtime").toString());
                    long leadtimeMillis = leadtimeSeconds * 1000L;

                    return new Date(leadtimeMillis);
                }
            }
        } catch (Exception e) {
            // Log lỗi ra console hoặc file log để debug
            System.err.println("Lỗi khi gọi API GHN Leadtime: " + e.getMessage());
            e.printStackTrace();
        }

        // Nếu gọi API thất bại hoặc có lỗi, trả về null để luồng đặt hàng không bị sập
        return null;
    }
}