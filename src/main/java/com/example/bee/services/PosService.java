package com.example.bee.services;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.account.VaiTro;
import com.example.bee.entities.cart.GioHang;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.order.*;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.entities.staff.NhanVien;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.account.VaiTroRepository;
import com.example.bee.repositories.cart.GioHangRepository;
import com.example.bee.repositories.catalog.KichThuocRepository;
import com.example.bee.repositories.catalog.MauSacRepository;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.order.*;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import com.example.bee.repositories.staff.NhanVienRepository;
import com.example.bee.utils.MomoSecurity;
import com.example.bee.utils.VnPayUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosService {

    private final SanPhamChiTietRepository variantRepo;
    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository hdctRepo;
    private final KhachHangRepository khachHangRepo;
    private final ThanhToanRepository thanhToanRepo;
    private final LichSuHoaDonRepository lichSuRepo;
    private final TrangThaiHoaDonRepository trangThaiRepo;
    private final MaGiamGiaRepository maGiamGiaRepo;
    private final NhanVienRepository nvRepo;

    public NhanVien getLoggedInNhanVien() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            return nvRepo.findByTaiKhoan_TenDangNhap(username).orElse(null);
        }
        return null;
    }

    @Transactional
    public void updateHoldStock(Integer spctId, Integer delta) {
        SanPhamChiTiet spct = variantRepo.findById(spctId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        if (delta > 0 && spct.getSoLuongKhaDung() < delta) {
            throw new IllegalArgumentException("Kho không đủ đáp ứng!");
        }

        spct.setSoLuongTamGiu(Math.max(0, spct.getSoLuongTamGiu() + delta));
        variantRepo.save(spct);
    }

    @Transactional
    public HoaDon createOrderToDB(Map<String, Object> payload, String method, String statusMa) {
        Integer customerId = (Integer) payload.get("customerId");
        List<Map<String, Object>> cart = (List<Map<String, Object>>) payload.get("cart");
        Integer voucherId = (Integer) payload.get("voucherId");
        BigDecimal totalAmount = new BigDecimal(payload.get("amount").toString());

        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống!");
        }

        String maHD = ("HD" + System.currentTimeMillis());
        NhanVien nvChotDon = getLoggedInNhanVien();

        HoaDon hd = HoaDon.builder()
                .ma(maHD)
                .giaTamThoi(BigDecimal.ZERO)
                .giaTong(BigDecimal.ZERO)
                .loaiHoaDon(0) // 0 là đơn tại quầy
                .hinhThucGiaoHang("NHAN_TAI_CUA_HANG") // Phân biệt với GIAO_TAN_NOI
                .ngayTao(new Date())
                .nhanVien(nvChotDon)
                .build();

        if (customerId != null) hd.setKhachHang(khachHangRepo.findById(customerId).orElse(null));
        hd = hoaDonRepo.save(hd);

        BigDecimal giaTamTinh = BigDecimal.ZERO;
        BigDecimal tongTienNguyenGia = BigDecimal.ZERO;

        for (Map<String, Object> item : cart) {
            Integer spctId = Integer.valueOf(item.get("id").toString());
            Integer qty = Integer.valueOf(item.get("qty").toString());
            BigDecimal price = new BigDecimal(item.get("price").toString());

            SanPhamChiTiet spct = variantRepo.findById(spctId).orElseThrow(() -> new IllegalArgumentException("Lỗi kho"));

            if (spct.getSoLuong() < qty) {
                throw new IllegalArgumentException("Sản phẩm " + spct.getSanPham().getTen() + " không đủ số lượng trong kho!");
            }

            BigDecimal giaGoc = spct.getGiaBan();
            BigDecimal thanhTienItem = price.multiply(BigDecimal.valueOf(qty));
            giaTamTinh = giaTamTinh.add(thanhTienItem);

            if (price.compareTo(giaGoc) >= 0) {
                tongTienNguyenGia = tongTienNguyenGia.add(thanhTienItem);
            }

            spct.setSoLuong(spct.getSoLuong() - qty);
            spct.setSoLuongTamGiu(Math.max(0, spct.getSoLuongTamGiu() - qty));

            if (spct.getSoLuong() <= 0) spct.setTrangThai(false);
            variantRepo.save(spct);

            hdctRepo.save(HoaDonChiTiet.builder().hoaDon(hd).sanPhamChiTiet(spct).soLuong(qty).giaTien(price).build());
        }

        BigDecimal chietKhauNV = BigDecimal.ZERO;
        if (hd.getKhachHang() != null && hd.getKhachHang().getSoDienThoai() != null) {
            boolean isEmployee = nvRepo.existsBySoDienThoaiAndTrangThaiTrue(hd.getKhachHang().getSoDienThoai());
            if (isEmployee) {
                chietKhauNV = giaTamTinh.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
            }
        }

        BigDecimal giamGiaVoucher = BigDecimal.ZERO;
        if (voucherId != null) {
            MaGiamGia voucher = maGiamGiaRepo.findById(voucherId).orElse(null);
            if (voucher != null) {
                if (!voucher.getTrangThai() ||
                        voucher.getLuotSuDung() >= voucher.getSoLuong() ||
                        (voucher.getNgayKetThuc() != null && voucher.getNgayKetThuc().isBefore(java.time.LocalDateTime.now()))) {
                    throw new IllegalArgumentException("Mã giảm giá này đã hết hạn hoặc hết lượt sử dụng!");
                }

                if (giaTamTinh.compareTo(voucher.getDieuKien()) < 0) {
                    throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getDieuKien() + "đ để dùng mã này!");
                }

                if (Boolean.FALSE.equals(voucher.getChoPhepCongDon()) && !"FREESHIP".equalsIgnoreCase(voucher.getLoaiGiamGia())) {
                    if (tongTienNguyenGia.compareTo(BigDecimal.ZERO) == 0) {
                        throw new IllegalArgumentException("Mã giảm giá này KHÔNG hỗ trợ áp dụng cho các sản phẩm đang chạy Sale!");
                    }
                }

                if (hd.getKhachHang() != null) {
                    boolean daSuDung = hoaDonRepo.existsByKhachHangIdAndMaGiamGiaIdAndTrangThaiHoaDon_MaNot(hd.getKhachHang().getId(), voucher.getId(), "DA_HUY");
                    if (daSuDung) {
                        throw new IllegalArgumentException("Khách hàng này đã sử dụng mã giảm giá này rồi!");
                    }
                }

                hd.setMaGiamGia(voucher);
                BigDecimal baseForVoucher = giaTamTinh;
                if (Boolean.FALSE.equals(voucher.getChoPhepCongDon()) && !"FREESHIP".equalsIgnoreCase(voucher.getLoaiGiamGia())) {
                    baseForVoucher = tongTienNguyenGia;
                }
                boolean isPercent = voucher.getLoaiGiamGia() != null && (voucher.getLoaiGiamGia().toUpperCase().contains("PERCENT") || voucher.getLoaiGiamGia().contains("%"));
                if (isPercent) {
                    giamGiaVoucher = baseForVoucher.multiply(voucher.getGiaTriGiamGia().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
                    if (voucher.getGiaTriGiamGiaToiDa() != null && giamGiaVoucher.compareTo(voucher.getGiaTriGiamGiaToiDa()) > 0) {
                        giamGiaVoucher = voucher.getGiaTriGiamGiaToiDa();
                    }
                } else {
                    giamGiaVoucher = voucher.getGiaTriGiamGia();
                    if (giamGiaVoucher.compareTo(baseForVoucher) > 0) giamGiaVoucher = baseForVoucher;
                }
                int luotMoi = voucher.getLuotSuDung() + 1;
                voucher.setLuotSuDung(luotMoi);
                if (luotMoi >= voucher.getSoLuong()) voucher.setTrangThai(false);
                maGiamGiaRepo.save(voucher);
            }
        }

        BigDecimal tongKhuyenMai = chietKhauNV.add(giamGiaVoucher);
        BigDecimal maxGiamGiaChoPhep = giaTamTinh.multiply(new BigDecimal("0.70")).setScale(0, RoundingMode.HALF_UP);
        if (tongKhuyenMai.compareTo(maxGiamGiaChoPhep) > 0) tongKhuyenMai = maxGiamGiaChoPhep;

        hd.setGiaTamThoi(giaTamTinh);
        hd.setGiaTriKhuyenMai(tongKhuyenMai);

        BigDecimal totalFinal = giaTamTinh.subtract(tongKhuyenMai);
        if (totalFinal.compareTo(BigDecimal.ZERO) < 0) totalFinal = BigDecimal.ZERO;
        hd.setGiaTong(totalAmount);

        TrangThaiHoaDon tthd = trangThaiRepo.findByMa(statusMa);
        hd.setTrangThaiHoaDon(tthd);
        if (statusMa.equals("HOAN_THANH")) {
            hd.setNgayThanhToan(new Date());
            hd.setNgayHangSanSang(new Date()); // Khách lấy hàng luôn
            hd.setNgayHenLayHang(new Date());
        }
        hoaDonRepo.save(hd);

        String ttStatus = statusMa.equals("HOAN_THANH") ? "THANH_CONG" : "CHO_THANH_TOAN";
        String ghiChuLichSu = statusMa.equals("HOAN_THANH") ? "Thanh toán " + method + " trực tiếp" : "Đang chờ thanh toán online " + method;

        thanhToanRepo.save(ThanhToan.builder()
                .hoaDon(hd)
                .soTien(hd.getGiaTong())
                .phuongThuc(method)
                .trangThai(ttStatus)
                .nhanVien(nvChotDon)
                .build());

        lichSuRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(tthd)
                .ghiChu(ghiChuLichSu)
                .nhanVien(nvChotDon)
                .build());

        return hd;
    }

    @Transactional
    public Map<String, Object> finishOrder(Map<String, Object> payload) {
        String method = payload.get("method") != null ? payload.get("method").toString() : "TIEN_MAT";
        HoaDon hd = createOrderToDB(payload, method, "HOAN_THANH");
        return Map.of("message", "Thành công!", "ma", hd.getMa(), "id", hd.getId());
    }

    @Transactional
    public Map<String, Object> confirmOnlinePayment(String maHD) {
        HoaDon hd = hoaDonRepo.findByMa(maHD);
        if (hd == null) throw new IllegalArgumentException("Không tìm thấy hóa đơn");
        if ("HOAN_THANH".equals(hd.getTrangThaiHoaDon().getMa())) {
            throw new IllegalArgumentException("Đã in hóa đơn trước đó rồi");
        }

        TrangThaiHoaDon ttHoanThanh = trangThaiRepo.findByMa("HOAN_THANH");
        hd.setTrangThaiHoaDon(ttHoanThanh);
        hd.setNgayThanhToan(new Date());
        hd.setNgayHangSanSang(new Date());
        hoaDonRepo.save(hd);

        List<ThanhToan> tts = thanhToanRepo.findByHoaDon_Id(hd.getId());
        if (tts != null && !tts.isEmpty()) {
            ThanhToan tt = tts.get(0);
            tt.setTrangThai("THANH_CONG");
            tt.setNgayThanhToan(new Date());
            thanhToanRepo.save(tt);
        }

        lichSuRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(ttHoanThanh)
                .ghiChu("Thanh toán Online thành công tại quầy")
                .nhanVien(getLoggedInNhanVien())
                .build());

        return Map.of("message", "Thành công", "id", hd.getId());
    }
}