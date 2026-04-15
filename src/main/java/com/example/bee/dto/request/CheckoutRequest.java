package com.example.bee.dto.request;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class CheckoutRequest {
    public String tenNguoiNhan;
    public String soDienThoai;
    public String email;
    public String diaChiGiaoHang;

    // GHN & Pick up
    public Integer maTinh;
    public Integer maHuyen;
    public String maXa;
    public String hinhThucGiaoHang;
    public Date ngayHenLayHang;

    public String ghiChu;
    public String phuongThucThanhToan;
    public BigDecimal tienHang;
    public BigDecimal phiShip;
    public BigDecimal tienGiam;
    public BigDecimal tongTien;
    public Integer voucherId;
    public Boolean isBuyNow;
    public List<CheckoutItemRequest> chiTietDonHangs;
}