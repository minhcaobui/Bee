package com.example.bee.entities.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThongTinGiaoHang implements Serializable {
    private String tenNguoiNhan;
    private String sdtNhan;
    private String emailNhan;
    private String diaChiChiTiet;

    private Integer maTinh;
    private Integer maHuyen;
    private String maXa;
}