package com.example.bee.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VariantRequest {
    private Integer idMauSac;
    private Integer idKichThuoc;
    private BigDecimal giaBan;
    private String hinhAnh;
}