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
    private String urlThoiGianGiaoHangGhn;

    @Value("${ghn.api.token}")
    private String maXacThucGhn;

    @Value("${ghn.shop.id}")
    private Integer idCuaHangGhn;

    @Value("${ghn.from.district.id}")
    private Integer idQuanHuyenGui;

    @Value("${ghn.from.ward.code}")
    private String maPhuongXaGui;

    // Sử dụng RestTemplate mặc định của Spring
    private final RestTemplate restTemplate;

    public GhnService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Tính toán ngày dự kiến nhận hàng từ GHN
     *
     * @param idQuanHuyenNhan ID Quận/Huyện của khách
     * @param maPhuongXaNhan  Mã Phường/Xã của khách
     * @param idDichVu        ID Dịch vụ giao hàng (VD: 53320 cho Standard, 53321 cho Nhanh...)
     * @return java.util.Date ngày dự kiến nhận
     */
    public Date tinhNgayNhanHangDuKien(Integer idQuanHuyenNhan, String maPhuongXaNhan, Integer idDichVu) {
        try {
            // 1. Khởi tạo Headers theo yêu cầu của GHN
            HttpHeaders tieuDe = new HttpHeaders();
            tieuDe.set("Content-Type", "application/json");
            tieuDe.set("Token", maXacThucGhn);
            tieuDe.set("ShopId", String.valueOf(idCuaHangGhn));

            // 2. Khởi tạo Body Payload
            Map<String, Object> duLieuGui = new HashMap<>();
            duLieuGui.put("from_district_id", idQuanHuyenGui);
            duLieuGui.put("from_ward_code", maPhuongXaGui);
            duLieuGui.put("to_district_id", idQuanHuyenNhan);
            duLieuGui.put("to_ward_code", maPhuongXaNhan);
            duLieuGui.put("service_id", idDichVu);

            HttpEntity<Map<String, Object>> thucThe = new HttpEntity<>(duLieuGui, tieuDe);

            // 3. Gọi API POST
            ResponseEntity<Map> phanHoi = restTemplate.exchange(
                    urlThoiGianGiaoHangGhn,
                    HttpMethod.POST,
                    thucThe,
                    Map.class
            );

            // 4. Bóc tách dữ liệu JSON trả về
            Map<String, Object> noiDungPhanHoi = phanHoi.getBody();
            if (noiDungPhanHoi != null && noiDungPhanHoi.get("code").equals(200)) {
                Map<String, Object> duLieu = (Map<String, Object>) noiDungPhanHoi.get("data");
                if (duLieu != null && duLieu.get("leadtime") != null) {

                    // GHN trả về Unix Timestamp (tính bằng giây). Trong Java, Date dùng mili-giây
                    long thoiGianGiaoGiay = Long.parseLong(duLieu.get("leadtime").toString());
                    long thoiGianGiaoMiliGiay = thoiGianGiaoGiay * 1000L;

                    return new Date(thoiGianGiaoMiliGiay);
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