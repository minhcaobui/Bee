package com.example.bee.converters;

import com.example.bee.entities.order.ThongTinGiaoHang;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ThongTinGiaoHangConverter implements AttributeConverter<ThongTinGiaoHang, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ThongTinGiaoHang attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Lỗi khi chuyển đối tượng ThongTinGiaoHang thành chuỗi JSON", e);
        }
    }

    @Override
    public ThongTinGiaoHang convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, ThongTinGiaoHang.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Lỗi khi parse chuỗi JSON từ DB thành đối tượng ThongTinGiaoHang", e);
        }
    }
}