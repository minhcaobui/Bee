package com.example.bee.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionRequest {
    private String ma;
    private String ten;
    private String loai; // "PERCENT" hoặc "AMOUNT"
    private BigDecimal giaTri;
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer soLuong;

    @NotNull(message = "Hình thức không được để trống")
    private Boolean hinhThuc; // true = Theo sản phẩm, false = Theo hóa đơn
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Boolean trangThai;

    private List<Integer> idSanPhams; // Danh sách ID sản phẩm (nếu hinhThuc = true)

    private BigDecimal giamToiDa;

    private BigDecimal dieuKienToiThieu;
}