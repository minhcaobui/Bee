package com.example.bee.entities.promotion;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ma_giam_gia")
public class MaGiamGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_code", nullable = false, length = 100, unique = true)
    private String maCode;

    @Column(name = "ten", nullable = false, length = 100)
    private String ten;

    @Column(name = "loai_giam_gia", nullable = false)
    private String loaiGiamGia;

    @Column(name = "gia_tri_giam_gia", nullable = false)
    private BigDecimal giaTriGiamGia;

    @Column(name = "gia_tri_giam_gia_toi_da")
    private BigDecimal giaTriGiamGiaToiDa;

    @Column(name = "dieu_kien", nullable = false)
    private BigDecimal dieuKien;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "luot_su_dung", nullable = false)
    private Integer luotSuDung = 0;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "cho_phep_cong_don")
    private Boolean choPhepCongDon = false;

    @Column(name = "trang_thai")
    private Boolean trangThai = true;

    private void validateVoucher(MaGiamGia body) {
        if (body.getTen() == null || body.getTen().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên hiển thị mã giảm giá không được để trống.");
        }
        if (body.getNgayBatDau() == null || body.getNgayKetThuc() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng thiết lập đầy đủ thời gian bắt đầu và kết thúc.");
        }
        if (body.getNgayKetThuc().isBefore(body.getNgayBatDau().plusMinutes(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời hạn sử dụng mã phải có độ dài tối thiểu 5 phút.");
        }
        if (body.getSoLuong() == null || body.getSoLuong() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng phát hành tối thiểu phải từ 1 mã trở lên.");
        }
        if (body.getDieuKien() == null || body.getDieuKien().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị đơn hàng tối thiểu không hợp lệ.");
        }

        // =========================================================
        // 🌟 RULE CHỐNG LỖ: KIỂM SOÁT TỶ LỆ GIẢM GIÁ TỐI ĐA
        // =========================================================
        if ("PERCENTAGE".equalsIgnoreCase(body.getLoaiGiamGia())) {
            // Sàn thương mại thường khóa trần ở mức 50% hoặc 70%
            BigDecimal maxPercent = new BigDecimal("70");

            if (body.getGiaTriGiamGia().compareTo(maxPercent) > 0 || body.getGiaTriGiamGia().compareTo(BigDecimal.ONE) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tỷ lệ chiết khấu không hợp lệ. Để tránh lỗ, không được giảm quá " + maxPercent + "%.");
            }
            if (body.getGiaTriGiamGiaToiDa() == null || body.getGiaTriGiamGiaToiDa().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BẮT BUỘC thiết lập 'Mức giảm tối đa' (VNĐ) khi dùng khuyến mãi theo % để tránh bị lạm dụng với đơn hàng lớn.");
            }
        } else {
            // LOẠI GIẢM TIỀN MẶT (FIXED)
            if (body.getGiaTriGiamGia().compareTo(new BigDecimal("1000")) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị chiết khấu tối thiểu phải từ 1.000 VNĐ.");
            }

            // Nếu đơn tối thiểu là 100k, thì voucher tiền mặt chỉ được tối đa 50k (50%)
            if (body.getDieuKien().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal limit = body.getDieuKien().multiply(new BigDecimal("0.5")); // Ngưỡng 50%
                if (body.getGiaTriGiamGia().compareTo(limit) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Để đảm bảo lợi nhuận, mức giảm tiền mặt không được vượt quá 50% giá trị đơn hàng tối thiểu (Tối đa " + limit.longValue() + "đ cho đơn " + body.getDieuKien().longValue() + "đ).");
                }
            }
            body.setGiaTriGiamGiaToiDa(null); // Clear data rác
        }
    }
}