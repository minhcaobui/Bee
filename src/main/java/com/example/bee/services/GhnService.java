package com.example.bee.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GhnService {

    // 🌟 ĐÃ SỬA: Đổi từ dev-online-gateway sang online-gateway (Môi trường thật)
    @Value("${ghn.api.url.leadtime:https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/leadtime}")
    private String urlLeadtime;

    // Gắn mặc định token của bạn từ checkout.html để đồng bộ
    @Value("${ghn.api.token:5c6b4e08-f30a-11f0-9c2d-8ebaf800da9f}")
    private String token;

    @Value("${ghn.shop.id:6219646}")
    private Integer shopId;

    @Value("${ghn.from.district.id:3440}") // Quận Nam Từ Liêm
    private Integer fromDistrictId;

    @Value("${ghn.from.ward.code:13010}") // Phường Phương Canh
    private String fromWardCode;

    private final RestTemplate restTemplate = new RestTemplate();

    public Date calculateExpectedDeliveryDate(Integer toDistrictId, String toWardCode, Integer serviceId) {
        try {
            System.out.println("GHN: Bắt đầu tính ngày giao hàng cho Huyện " + toDistrictId + ", Xã " + toWardCode);

            // 1. NẾU KHÔNG TRUYỀN SERVICE_ID -> TỰ ĐỘNG TÌM DỊCH VỤ KHẢ DỤNG CHO TUYẾN ĐƯỜNG NÀY
            if (serviceId == null) {
                serviceId = getAvailableServiceId(toDistrictId);
                if (serviceId == null) {
                    System.err.println("GHN: Không tìm thấy dịch vụ giao hàng nào cho tuyến đường này.");
                    return null;
                }
            }

            // 2. GỌI API LẤY NGÀY DỰ KIẾN (LEADTIME)
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Token", token);
            headers.set("ShopId", String.valueOf(shopId));

            Map<String, Object> body = new HashMap<>();
            body.put("from_district_id", fromDistrictId);
            body.put("from_ward_code", fromWardCode);
            body.put("to_district_id", toDistrictId);
            body.put("to_ward_code", toWardCode);
            body.put("service_id", serviceId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(urlLeadtime, HttpMethod.POST, entity, Map.class);
            Map<String, Object> resBody = response.getBody();

            if (resBody != null && String.valueOf(resBody.get("code")).equals("200")) {
                Map<String, Object> data = (Map<String, Object>) resBody.get("data");
                if (data != null && data.get("leadtime") != null) {
                    long leadtimeSecs = Long.parseLong(data.get("leadtime").toString());
                    System.out.println("GHN: Tính toán THÀNH CÔNG! Ngày nhận dự kiến: " + new Date(leadtimeSecs * 1000L));
                    return new Date(leadtimeSecs * 1000L); // Convert giây sang mili-giây
                }
            } else {
                System.err.println("GHN Leadtime API Error: " + resBody);
            }
        } catch (RestClientResponseException e) {
            System.err.println("GHN TỪ CHỐI KẾT NỐI (Lỗi " + e.getRawStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Lỗi gọi GHN Leadtime: " + e.getMessage());
        }
        return null;
    }

    // Tự động hỏi GHN xem tuyến đường này hỗ trợ gói nào
    private Integer getAvailableServiceId(Integer toDistrictId) {
        try {
            String urlServices = urlLeadtime.replace("leadtime", "available-services");
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Token", token);

            Map<String, Object> body = new HashMap<>();
            body.put("shop_id", shopId);
            body.put("from_district", fromDistrictId);
            body.put("to_district", toDistrictId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(urlServices, HttpMethod.POST, entity, Map.class);
            Map<String, Object> resBody = response.getBody();

            if (resBody != null && String.valueOf(resBody.get("code")).equals("200")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) resBody.get("data");
                if (data != null && !data.isEmpty()) {
                    System.out.println("GHN: Tìm thấy dịch vụ ID = " + data.get(0).get("service_id"));
                    return Integer.parseInt(data.get(0).get("service_id").toString());
                }
            }
        } catch (RestClientResponseException e) {
            System.err.println("GHN LỖI TÌM DỊCH VỤ: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Lỗi lấy GHN Services: " + e.getMessage());
        }
        return null;
    }
}