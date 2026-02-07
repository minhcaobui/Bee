package com.example.bee.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PromotionRequest {
    private Integer id;
    private String ma;
    private String ten;
    private String loai; // PERCENT / AMOUNT
    private BigDecimal giaTri;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Boolean choPhepCongDon = false;
    private Boolean trangThai = true;

    private List<Integer> idSanPhams;
}