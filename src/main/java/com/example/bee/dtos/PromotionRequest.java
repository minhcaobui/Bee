package com.example.bee.dtos;

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
    private String loai;
    private BigDecimal giaTri;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Boolean choPhepCongDon = false;
    private Boolean trangThai = true;
    private List<Integer> idSanPhams;
}