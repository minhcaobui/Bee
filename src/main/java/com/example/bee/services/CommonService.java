package com.example.bee.services;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.account.VaiTro;
import com.example.bee.entities.cart.GioHang;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.order.HoaDon;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.account.VaiTroRepository;
import com.example.bee.repositories.cart.GioHangRepository;
import com.example.bee.repositories.catalog.KichThuocRepository;
import com.example.bee.repositories.catalog.MauSacRepository;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.order.HoaDonRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import com.example.bee.repositories.staff.NhanVienRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommonService {

    private final HoaDonRepository hoaDonRepo;
    private final KhachHangRepository khachHangRepo;
    private final NhanVienRepository nhanVienRepo;
    private final SanPhamChiTietRepository sanPhamChiTietRepo;
    private final KhuyenMaiRepository khuyenMaiRepo;
    private final MauSacRepository mauSacRepo;
    private final KichThuocRepository kichThuocRepo;
    private final MaGiamGiaRepository maGiamGiaRepo;
    private final TaiKhoanRepository taiKhoanRepo;
    private final VaiTroRepository vaiTroRepo;
    private final PasswordEncoder passwordEncoder;
    private final GioHangRepository gioHangRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    private static final Map<String, String> boNhoLuuOtp = new HashMap<>();

    // ==========================================
    // 1. CÁC HÀM XỬ LÝ OTP & KIỂM TRA ĐÃ GỘP
    // ==========================================

    public ResponseEntity<?> guiOtp(String email) {
        try {
            String otp = String.format("%06d", new Random().nextInt(999999));
            boNhoLuuOtp.put(email, otp);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(email);
            message.setSubject("[BeeMate] MÃ XÁC THỰC OTP HỆ THỐNG");
            message.setText("Chào bạn,\n\nBạn đang thực hiện thao tác xác thực trên hệ thống BeeMate.\n"
                    + "Mã xác thực OTP của bạn là: " + otp + "\n\n"
                    + "Vui lòng nhập mã này vào hệ thống để hoàn tất.\n"
                    + "Trân trọng,\nBan Quản Trị Hệ Thống BeeMate.");
            mailSender.send(message);
            return ResponseEntity.ok(Map.of("message", "Đã gửi mã OTP thành công đến email: " + email));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi gửi Email: Sai tài khoản cấu hình hoặc mất mạng!"));
        }
    }

    public ResponseEntity<?> xacThucOtp(String email, String maXacThuc) {
        String savedOtp = boNhoLuuOtp.get(email);
        if (savedOtp != null && savedOtp.equals(maXacThuc)) {
            boNhoLuuOtp.remove(email);
            return ResponseEntity.ok(Map.of("message", "Xác thực OTP thành công!"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP không chính xác, vui lòng nhập lại!"));
    }

    public ResponseEntity<?> kiemTraEmail(String email, Integer id, String loaiTaiKhoan) {
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email không đúng định dạng!"));
        }

        boolean daTonTai = false;
        if ("NHAN_VIEN".equalsIgnoreCase(loaiTaiKhoan)) {
            daTonTai = (id == null) ? nhanVienRepo.existsByEmail(email) : nhanVienRepo.existsByEmailAndIdNot(email, id);
        } else {
            daTonTai = (id == null) ? khachHangRepo.existsByEmail(email) : khachHangRepo.existsByEmailAndIdNot(email, id);
        }

        if (daTonTai) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email này đã tồn tại trong hệ thống!"));
        }
        return ResponseEntity.ok(Map.of("message", "Email hợp lệ"));
    }

    public ResponseEntity<?> kiemTraSdt(String soDienThoai, Integer id, String loaiTaiKhoan) {
        if (!soDienThoai.matches("^0[0-9]{9}$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "SĐT phải đủ 10 số và bắt đầu bằng 0!"));
        }

        boolean daTonTai = false;
        if ("NHAN_VIEN".equalsIgnoreCase(loaiTaiKhoan)) {
            daTonTai = (id == null) ? nhanVienRepo.existsBySoDienThoai(soDienThoai) : nhanVienRepo.existsBySoDienThoaiAndIdNot(soDienThoai, id);
        } else {
            daTonTai = (id == null) ? khachHangRepo.existsBySoDienThoai(soDienThoai) : khachHangRepo.existsBySoDienThoaiAndIdNot(soDienThoai, id);
        }

        if (daTonTai) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại này đã tồn tại trong hệ thống!"));
        }
        return ResponseEntity.ok(Map.of("message", "SĐT hợp lệ"));
    }

    // ==========================================
    // 2. CÁC HÀM TIỆN ÍCH DÙNG CHUNG
    // ==========================================

    public List<SanPhamChiTiet> timKiemSanPhamChoThanhToan(String tuKhoa, Integer idMauSac, Integer idKichThuoc) {
        List<SanPhamChiTiet> danhSach = sanPhamChiTietRepo.findAvailableProducts(tuKhoa, idMauSac, idKichThuoc);
        for (SanPhamChiTiet spct : danhSach) {
            spct.setSoLuong(spct.getSoLuongKhaDung());
            BigDecimal giaGoc = spct.getGiaBan();
            BigDecimal giaSauKM = giaGoc;
            List<KhuyenMai> khuyenMaiHoatDong = khuyenMaiRepo.findActiveKhuyenMaiBySanPhamId(spct.getSanPham().getId());
            if (khuyenMaiHoatDong != null && !khuyenMaiHoatDong.isEmpty()) {
                KhuyenMai km = khuyenMaiHoatDong.get(0);
                boolean laPhanTram = km.getLoai() != null &&
                        (km.getLoai().toUpperCase().contains("PERCENT") || km.getLoai().contains("%"));
                if (laPhanTram) {
                    BigDecimal tyLe = km.getGiaTri().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    BigDecimal tienGiam = giaGoc.multiply(tyLe).setScale(0, RoundingMode.HALF_UP);
                    giaSauKM = giaGoc.subtract(tienGiam);
                } else {
                    giaSauKM = giaGoc.subtract(km.getGiaTri());
                }
            }
            if (giaSauKM.compareTo(BigDecimal.ZERO) < 0) giaSauKM = BigDecimal.ZERO;
            spct.setGiaSauKhuyenMai(giaSauKM);
        }
        return danhSach;
    }

    public Map<String, Object> layThuocTinh() {
        return Map.of("danhSachMauSac", mauSacRepo.findAll(), "danhSachKichThuoc", kichThuocRepo.findAll());
    }

    public List<KhachHang> timKiemKhachHang(String tuKhoa) {
        return khachHangRepo.findBySoDienThoaiContainingOrHoTenContaining(tuKhoa, tuKhoa);
    }

    @Transactional
    public KhachHang taoMoiKhachHangNhanh(KhachHang kh) {
        String sdt = kh.getSoDienThoai();
        if (sdt == null || sdt.trim().isEmpty()) throw new IllegalArgumentException("Vui lòng nhập số điện thoại của khách hàng!");
        if (taiKhoanRepo.existsByTenDangNhap(sdt)) throw new IllegalArgumentException("Số điện thoại này đã được đăng ký trên hệ thống!");

        VaiTro quyenKhachHang = vaiTroRepo.findByMa("ROLE_CUSTOMER").orElseThrow(() -> new IllegalArgumentException("Chưa cấu hình quyền ROLE_CUSTOMER"));
        String matKhauMacDinh = "123456";
        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap(sdt);
        tk.setMatKhau(passwordEncoder.encode(matKhauMacDinh));
        tk.setVaiTro(quyenKhachHang);
        tk.setTrangThai(true);
        TaiKhoan taiKhoanDaLuu = taiKhoanRepo.save(tk);

        GioHang gioHang = new GioHang();
        gioHang.setTaiKhoan(taiKhoanDaLuu);
        gioHangRepository.save(gioHang);

        long dem = khachHangRepo.count();
        String maKhachHang;
        do {
            dem++;
            maKhachHang = String.format("KH%08d", dem);
        } while (khachHangRepo.existsByMaIgnoreCase(maKhachHang));

        kh.setMa(maKhachHang);
        kh.setHoTen(kh.getHoTen() == null || kh.getHoTen().trim().isEmpty() ? "Khách vãng lai" : kh.getHoTen());
        kh.setTaiKhoan(taiKhoanDaLuu);
        kh.setTrangThai(true);

        return khachHangRepo.save(kh);
    }

    public MaGiamGia apDungMaGiamGia(Map<String, Object> duLieu) {
        String maCode = (String) duLieu.get("maCode");
        List<Map<String, Object>> gioHang = (List<Map<String, Object>>) duLieu.get("gioHang");
        Object idKhachHangObj = duLieu.get("idKhachHang");

        Optional<MaGiamGia> voucherOpt = maGiamGiaRepo.findByMaCode(maCode);
        if (voucherOpt.isEmpty()) throw new IllegalArgumentException("Mã không tồn tại!");

        MaGiamGia maGiamGia = voucherOpt.get();
        if (maGiamGia.getNgayKetThuc().isBefore(LocalDateTime.now()) || maGiamGia.getSoLuong() <= maGiamGia.getLuotSuDung() || !maGiamGia.getTrangThai()) {
            throw new IllegalArgumentException("MÃ GIẢM GIÁ ĐÃ HẾT HẠN HOẶC HẾT LƯỢT SỬ DỤNG");
        }

        if (idKhachHangObj != null && !idKhachHangObj.toString().trim().isEmpty()) {
            Integer idKhach = Integer.valueOf(idKhachHangObj.toString());
            boolean daSuDung = hoaDonRepo.existsByKhachHangIdAndMaGiamGiaIdAndTrangThaiHoaDon_MaNot(idKhach, maGiamGia.getId(), "DA_HUY");
            if (daSuDung) throw new IllegalArgumentException("Khách hàng này đã sử dụng mã giảm giá này rồi!");
        }

        BigDecimal giaTamTinh = BigDecimal.ZERO;
        BigDecimal tongTienNguyenGia = BigDecimal.ZERO;

        if (gioHang != null) {
            for (Map<String, Object> sanPham : gioHang) {
                Integer idSPCT = Integer.valueOf(sanPham.get("idSanPhamChiTiet").toString());
                Integer soLuong = Integer.valueOf(sanPham.get("soLuong").toString());
                BigDecimal giaTien = new BigDecimal(sanPham.get("giaTien").toString());

                SanPhamChiTiet spct = sanPhamChiTietRepo.findById(idSPCT).orElseThrow(() -> new IllegalArgumentException("Lỗi kho"));
                BigDecimal giaGoc = spct.getGiaBan();
                BigDecimal thanhTienItem = giaTien.multiply(BigDecimal.valueOf(soLuong));
                giaTamTinh = giaTamTinh.add(thanhTienItem);

                if (giaTien.compareTo(giaGoc) >= 0) {
                    tongTienNguyenGia = tongTienNguyenGia.add(thanhTienItem);
                }
            }
        }

        if (giaTamTinh.compareTo(maGiamGia.getDieuKien()) < 0) {
            throw new IllegalArgumentException("Chưa đủ điều kiện (Tối thiểu " + new java.text.DecimalFormat("#,###").format(maGiamGia.getDieuKien()) + "đ)");
        }

        if (Boolean.FALSE.equals(maGiamGia.getChoPhepCongDon()) && !"FREESHIP".equalsIgnoreCase(maGiamGia.getLoaiGiamGia())) {
            if (tongTienNguyenGia.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Mã giảm giá này KHÔNG hỗ trợ áp dụng cho các sản phẩm đang chạy Sale!");
            }
        }
        return maGiamGia;
    }

    public List<Integer> layMaGiamGiaDaDungCuaKhach(Integer idKhachHang) {
        return hoaDonRepo.findByKhachHangIdOrderByNgayTaoDesc(idKhachHang).stream()
                .filter(hd -> !"DA_HUY".equals(hd.getTrangThaiHoaDon().getMa()) && hd.getMaGiamGia() != null)
                .map(hd -> hd.getMaGiamGia().getId())
                .distinct().collect(Collectors.toList());
    }
}