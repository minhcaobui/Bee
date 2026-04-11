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

    private final SanPhamChiTietRepository sanPhamChiTietRepo;
    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository hoaDonChiTietRepo;
    private final KhachHangRepository khachHangRepo;
    private final ThanhToanRepository thanhToanRepo;
    private final LichSuHoaDonRepository lichSuHoaDonRepo;
    private final TrangThaiHoaDonRepository trangThaiHoaDonRepo;
    private final MaGiamGiaRepository maGiamGiaRepo;
    private final NhanVienRepository nhanVienRepo;

    public NhanVien layNhanVienDangDangNhap() {
        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        if (xacThuc != null && xacThuc.isAuthenticated() && !xacThuc.getPrincipal().equals("anonymousUser")) {
            String tenDangNhap = xacThuc.getName();
            return nhanVienRepo.findByTaiKhoan_TenDangNhap(tenDangNhap).orElse(null);
        }
        return null;
    }

    @Transactional
    public void capNhatGiuKho(Integer idSanPhamChiTiet, Integer soLuongThayDoi) {
        SanPhamChiTiet spct = sanPhamChiTietRepo.findById(idSanPhamChiTiet)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        if (soLuongThayDoi > 0 && spct.getSoLuongKhaDung() < soLuongThayDoi) {
            throw new IllegalArgumentException("Kho không đủ đáp ứng!");
        }

        spct.setSoLuongTamGiu(Math.max(0, spct.getSoLuongTamGiu() + soLuongThayDoi));
        sanPhamChiTietRepo.save(spct);
    }

    @Transactional
    public HoaDon taoHoaDon(Map<String, Object> duLieu, String phuongThucThanhToan, String maTrangThai) {
        Integer idKhachHang = (Integer) duLieu.get("idKhachHang");
        List<Map<String, Object>> gioHang = (List<Map<String, Object>>) duLieu.get("gioHang");
        Integer idMaGiamGia = (Integer) duLieu.get("idMaGiamGia");
        BigDecimal tongTien = new BigDecimal(duLieu.get("tongTien").toString());

        if (gioHang == null || gioHang.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống!");
        }

        String maHD = ("HD" + System.currentTimeMillis());
        NhanVien nvChotDon = layNhanVienDangDangNhap();

        HoaDon hoaDon = HoaDon.builder()
                .ma(maHD)
                .giaTamThoi(BigDecimal.ZERO)
                .giaTong(BigDecimal.ZERO)
                .loaiHoaDon(0) // 0 là đơn tại quầy
                .hinhThucGiaoHang("NHAN_TAI_CUA_HANG") // Phân biệt với GIAO_TAN_NOI
                .ngayTao(new Date())
                .nhanVien(nvChotDon)
                .build();

        if (idKhachHang != null) {
            hoaDon.setKhachHang(khachHangRepo.findById(idKhachHang).orElse(null));
        }
        hoaDon = hoaDonRepo.save(hoaDon);

        BigDecimal giaTamTinh = BigDecimal.ZERO;
        BigDecimal tongTienNguyenGia = BigDecimal.ZERO;

        for (Map<String, Object> sanPham : gioHang) {
            Integer idSPCT = Integer.valueOf(sanPham.get("id").toString());
            Integer soLuong = Integer.valueOf(sanPham.get("soLuong").toString());
            BigDecimal giaTien = new BigDecimal(sanPham.get("giaTien").toString());

            SanPhamChiTiet spct = sanPhamChiTietRepo.findById(idSPCT)
                    .orElseThrow(() -> new IllegalArgumentException("Lỗi kho"));

            if (spct.getSoLuong() < soLuong) {
                throw new IllegalArgumentException("Sản phẩm " + spct.getSanPham().getTen() + " không đủ số lượng trong kho!");
            }

            BigDecimal giaGoc = spct.getGiaBan();
            BigDecimal thanhTienSanPham = giaTien.multiply(BigDecimal.valueOf(soLuong));
            giaTamTinh = giaTamTinh.add(thanhTienSanPham);

            if (giaTien.compareTo(giaGoc) >= 0) {
                tongTienNguyenGia = tongTienNguyenGia.add(thanhTienSanPham);
            }

            spct.setSoLuong(spct.getSoLuong() - soLuong);
            spct.setSoLuongTamGiu(Math.max(0, spct.getSoLuongTamGiu() - soLuong));

            if (spct.getSoLuong() <= 0) spct.setTrangThai(false);
            sanPhamChiTietRepo.save(spct);

            hoaDonChiTietRepo.save(HoaDonChiTiet.builder()
                    .hoaDon(hoaDon)
                    .sanPhamChiTiet(spct)
                    .soLuong(soLuong)
                    .giaTien(giaTien)
                    .build());
        }

        BigDecimal chietKhauNhanVien = BigDecimal.ZERO;
        if (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getSoDienThoai() != null) {
            boolean laNhanVien = nhanVienRepo.existsBySoDienThoaiAndTrangThaiTrue(hoaDon.getKhachHang().getSoDienThoai());
            if (laNhanVien) {
                chietKhauNhanVien = giaTamTinh.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
            }
        }

        BigDecimal giamGiaTuMa = BigDecimal.ZERO;
        if (idMaGiamGia != null) {
            MaGiamGia maGiamGia = maGiamGiaRepo.findById(idMaGiamGia).orElse(null);
            if (maGiamGia != null) {
                if (!maGiamGia.getTrangThai() ||
                        maGiamGia.getLuotSuDung() >= maGiamGia.getSoLuong() ||
                        (maGiamGia.getNgayKetThuc() != null && maGiamGia.getNgayKetThuc().isBefore(java.time.LocalDateTime.now()))) {
                    throw new IllegalArgumentException("Mã giảm giá này đã hết hạn hoặc hết lượt sử dụng!");
                }

                if (giaTamTinh.compareTo(maGiamGia.getDieuKien()) < 0) {
                    throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu " + maGiamGia.getDieuKien() + "đ để dùng mã này!");
                }

                if (Boolean.FALSE.equals(maGiamGia.getChoPhepCongDon()) && !"FREESHIP".equalsIgnoreCase(maGiamGia.getLoaiGiamGia())) {
                    if (tongTienNguyenGia.compareTo(BigDecimal.ZERO) == 0) {
                        throw new IllegalArgumentException("Mã giảm giá này KHÔNG hỗ trợ áp dụng cho các sản phẩm đang chạy Sale!");
                    }
                }

                if (hoaDon.getKhachHang() != null) {
                    boolean daSuDung = hoaDonRepo.existsByKhachHangIdAndMaGiamGiaIdAndTrangThaiHoaDon_MaNot(hoaDon.getKhachHang().getId(), maGiamGia.getId(), "DA_HUY");
                    if (daSuDung) {
                        throw new IllegalArgumentException("Khách hàng này đã sử dụng mã giảm giá này rồi!");
                    }
                }

                hoaDon.setMaGiamGia(maGiamGia);
                BigDecimal giaTriTinhGiam = giaTamTinh;
                if (Boolean.FALSE.equals(maGiamGia.getChoPhepCongDon()) && !"FREESHIP".equalsIgnoreCase(maGiamGia.getLoaiGiamGia())) {
                    giaTriTinhGiam = tongTienNguyenGia;
                }

                boolean laPhanTram = maGiamGia.getLoaiGiamGia() != null && (maGiamGia.getLoaiGiamGia().toUpperCase().contains("PERCENT") || maGiamGia.getLoaiGiamGia().contains("%"));
                if (laPhanTram) {
                    giamGiaTuMa = giaTriTinhGiam.multiply(maGiamGia.getGiaTriGiamGia().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
                    if (maGiamGia.getGiaTriGiamGiaToiDa() != null && giamGiaTuMa.compareTo(maGiamGia.getGiaTriGiamGiaToiDa()) > 0) {
                        giamGiaTuMa = maGiamGia.getGiaTriGiamGiaToiDa();
                    }
                } else {
                    giamGiaTuMa = maGiamGia.getGiaTriGiamGia();
                    if (giamGiaTuMa.compareTo(giaTriTinhGiam) > 0) giamGiaTuMa = giaTriTinhGiam;
                }

                int luotMoi = maGiamGia.getLuotSuDung() + 1;
                maGiamGia.setLuotSuDung(luotMoi);
                if (luotMoi >= maGiamGia.getSoLuong()) maGiamGia.setTrangThai(false);
                maGiamGiaRepo.save(maGiamGia);
            }
        }

        BigDecimal tongKhuyenMai = chietKhauNhanVien.add(giamGiaTuMa);
        BigDecimal giamGiaToiDaChoPhep = giaTamTinh.multiply(new BigDecimal("0.70")).setScale(0, RoundingMode.HALF_UP);
        if (tongKhuyenMai.compareTo(giamGiaToiDaChoPhep) > 0) tongKhuyenMai = giamGiaToiDaChoPhep;

        hoaDon.setGiaTamThoi(giaTamTinh);
        hoaDon.setGiaTriKhuyenMai(tongKhuyenMai);

        BigDecimal tongTienCuoiCung = giaTamTinh.subtract(tongKhuyenMai);
        if (tongTienCuoiCung.compareTo(BigDecimal.ZERO) < 0) tongTienCuoiCung = BigDecimal.ZERO;
        hoaDon.setGiaTong(tongTien);

        TrangThaiHoaDon tthd = trangThaiHoaDonRepo.findByMa(maTrangThai);
        hoaDon.setTrangThaiHoaDon(tthd);
        if (maTrangThai.equals("HOAN_THANH")) {
            hoaDon.setNgayThanhToan(new Date());
            hoaDon.setNgayHangSanSang(new Date()); // Khách lấy hàng luôn
            hoaDon.setNgayHenLayHang(new Date());
        }
        hoaDonRepo.save(hoaDon);

        String trangThaiThanhToan = maTrangThai.equals("HOAN_THANH") ? "THANH_CONG" : "CHO_THANH_TOAN";
        String ghiChuLichSu = maTrangThai.equals("HOAN_THANH") ? "Thanh toán " + phuongThucThanhToan + " trực tiếp" : "Đang chờ thanh toán online " + phuongThucThanhToan;

        thanhToanRepo.save(ThanhToan.builder()
                .hoaDon(hoaDon)
                .soTien(hoaDon.getGiaTong())
                .phuongThuc(phuongThucThanhToan)
                .trangThai(trangThaiThanhToan)
                .nhanVien(nvChotDon)
                .build());

        lichSuHoaDonRepo.save(LichSuHoaDon.builder()
                .hoaDon(hoaDon)
                .trangThaiHoaDon(tthd)
                .ghiChu(ghiChuLichSu)
                .nhanVien(nvChotDon)
                .build());

        return hoaDon;
    }

    @Transactional
    public Map<String, Object> hoanThanhDonHang(Map<String, Object> duLieu) {
        String phuongThuc = duLieu.get("phuongThuc") != null ? duLieu.get("phuongThuc").toString() : "TIEN_MAT";
        HoaDon hd = taoHoaDon(duLieu, phuongThuc, "HOAN_THANH");
        return Map.of("message", "Thành công!", "ma", hd.getMa(), "id", hd.getId());
    }

    @Transactional
    public Map<String, Object> xacNhanThanhToanOnline(String maHD) {
        HoaDon hd = hoaDonRepo.findByMa(maHD);
        if (hd == null) throw new IllegalArgumentException("Không tìm thấy hóa đơn");
        if ("HOAN_THANH".equals(hd.getTrangThaiHoaDon().getMa())) {
            throw new IllegalArgumentException("Đã in hóa đơn trước đó rồi");
        }

        TrangThaiHoaDon ttHoanThanh = trangThaiHoaDonRepo.findByMa("HOAN_THANH");
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

        lichSuHoaDonRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(ttHoanThanh)
                .ghiChu("Thanh toán Online thành công tại quầy")
                .nhanVien(layNhanVienDangDangNhap())
                .build());

        return Map.of("message", "Thành công", "id", hd.getId());
    }
}