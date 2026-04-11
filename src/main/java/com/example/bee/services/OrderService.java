package com.example.bee.services;

import com.example.bee.dtos.request.CheckoutItemRequest;
import com.example.bee.dtos.request.CheckoutRequest;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.notification.ThongBao;
import com.example.bee.entities.order.*;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.entities.staff.NhanVien;
import com.example.bee.repositories.cart.GioHangChiTietRepository;
import com.example.bee.repositories.cart.GioHangRepository;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
import com.example.bee.repositories.order.*;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import com.example.bee.repositories.staff.NhanVienRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final HoaDonRepository hoaDonRepository;
    private final TrangThaiHoaDonRepository trangThaiHoaDonRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final NhanVienRepository nhanVienRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final KhachHangRepository khachHangRepository;
    private final EmailService emailService;
    private final ThongBaoRepository thongBaoRepository;
    private final MaGiamGiaRepository maGiamGiaRepository;
    private final YeuCauDoiTraRepository yeuCauDoiTraRepository;
    private final ChiTietDoiTraRepository chiTietDoiTraRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final GhnService ghnService;

    public Page<HoaDon> searchDonHangChoXuLy(String q, Integer statusId, Integer loaiHoaDon, String phuongThucThanhToan, Date startDate, Date endDate, Pageable pageable) {
        return hoaDonRepository.searchDonHangChoXuLy(q, statusId, loaiHoaDon, phuongThucThanhToan, startDate, endDate, pageable);
    }

    public Page<HoaDon> searchLichSuHoaDon(String q, Integer statusId, Integer nhanVienId, Integer loaiHoaDon, String phuongThucThanhToan, Date startDate, Date endDate, Pageable pageable) {
        return hoaDonRepository.searchLichSuHoaDon(q, statusId, nhanVienId, loaiHoaDon, phuongThucThanhToan, startDate, endDate, pageable);
    }

    public Map<String, Object> getDetail(Integer id) {
        HoaDon hd = hoaDonRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        List<HoaDonChiTiet> listHdct = hoaDonChiTietRepository.findByHoaDonId(id);

        Map<Integer, Integer> returnedItems = new HashMap<>();
        Date ngayCapNhatCuoi = hd.getNgayThanhToan();
        BigDecimal tongTienHoan = BigDecimal.ZERO;

        List<YeuCauDoiTra> ycs = yeuCauDoiTraRepository.findAll().stream().filter(y -> y.getHoaDon().getId().equals(id) && "HOAN_THANH".equals(y.getTrangThai())).collect(Collectors.toList());
        for (YeuCauDoiTra yc : ycs) {
            if (ngayCapNhatCuoi == null || (yc.getNgayXuLy() != null && yc.getNgayXuLy().after(ngayCapNhatCuoi))) {
                ngayCapNhatCuoi = yc.getNgayXuLy();
            }
            if (yc.getSoTienHoan() != null) {
                tongTienHoan = tongTienHoan.add(yc.getSoTienHoan());
            }
            List<ChiTietDoiTra> cts = chiTietDoiTraRepository.findByYeuCauDoiTraId(yc.getId());
            for (ChiTietDoiTra ct : cts) {
                Integer hdctId = ct.getHoaDonChiTiet().getId();
                returnedItems.put(hdctId, returnedItems.getOrDefault(hdctId, 0) + ct.getSoLuong());
            }
        }

        BigDecimal tongTienHangThucTe = BigDecimal.ZERO;
        List<Map<String, Object>> chiTietResponses = new ArrayList<>();

        for (HoaDonChiTiet ct : listHdct) {
            BigDecimal giaBan = ct.getGiaTien();
            Integer soLuong = ct.getSoLuong();
            Integer soLuongTra = returnedItems.getOrDefault(ct.getId(), 0);
            tongTienHangThucTe = tongTienHangThucTe.add(giaBan.multiply(BigDecimal.valueOf(soLuong)));

            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", ct.getId());
            itemMap.put("idSanPhamChiTiet", ct.getSanPhamChiTiet().getId());
            itemMap.put("idSanPham", ct.getSanPhamChiTiet().getSanPham().getId());
            itemMap.put("tenSanPham", ct.getSanPhamChiTiet().getSanPham().getTen());
            itemMap.put("sku", ct.getSanPhamChiTiet().getSku());
            itemMap.put("thuocTinh", ct.getSanPhamChiTiet().getKichThuoc().getTen() + " - " + ct.getSanPhamChiTiet().getMauSac().getTen());
            itemMap.put("hinhAnh", ct.getSanPhamChiTiet().getHinhAnh());
            itemMap.put("soLuong", soLuong);
            itemMap.put("donGia", ct.getSanPhamChiTiet().getGiaBan());
            itemMap.put("giaBan", giaBan);
            itemMap.put("soLuongTra", soLuongTra);
            chiTietResponses.add(itemMap);
        }

        BigDecimal phiShip = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal tongPhaiTra = hd.getGiaTong() != null ? hd.getGiaTong() : BigDecimal.ZERO;
        BigDecimal voucherCalculated = tongTienHangThucTe.add(phiShip).subtract(tongPhaiTra);
        if (voucherCalculated.compareTo(BigDecimal.ZERO) < 0) voucherCalculated = BigDecimal.ZERO;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String tenNhanVienHienTai = "Hệ thống";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            var nhanVienDangNhap = nhanVienRepository.findByTaiKhoan_TenDangNhap(username).orElse(null);
            if (nhanVienDangNhap != null) {
                tenNhanVienHienTai = nhanVienDangNhap.getHoTen();
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", hd.getId());
        response.put("ma", hd.getMa());

        // CẬP NHẬT TỪ OBJECT JSON THONG_TIN_GIAO_HANG
        String tenNhan = (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getTenNguoiNhan() != null)
                ? hd.getThongTinGiaoHang().getTenNguoiNhan()
                : (hd.getKhachHang() != null ? hd.getKhachHang().getHoTen() : "Khách vãng lai");

        String sdtNhan = (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getSdtNhan() != null)
                ? hd.getThongTinGiaoHang().getSdtNhan()
                : (hd.getKhachHang() != null ? hd.getKhachHang().getSoDienThoai() : "");

        String dcNhan = (hd.getThongTinGiaoHang() != null) ? hd.getThongTinGiaoHang().getDiaChiChiTiet() : "";

        response.put("tenKhachHang", tenNhan);
        response.put("sdtKhachHang", sdtNhan);
        response.put("email", hd.getKhachHang() != null ? hd.getKhachHang().getEmail() : "");
        response.put("diaChiGiaoHang", dcNhan);

        String rawGhiChu = hd.getGhiChu() != null ? hd.getGhiChu() : "";
        if (rawGhiChu.contains("[BUY_NOW]")) {
            rawGhiChu = rawGhiChu.replace("[BUY_NOW]", "").trim();
        }
        response.put("ghiChu", rawGhiChu);
        response.put("ngayThanhToan", hd.getNgayThanhToan() != null ? sdf.format(hd.getNgayThanhToan()) : null);
        response.put("ngayCapNhat", ngayCapNhatCuoi != null ? sdf.format(ngayCapNhatCuoi) : null);
        response.put("tenNhanVien", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : tenNhanVienHienTai);

        List<ThanhToan> ttList = thanhToanRepository.findByHoaDon_Id(hd.getId());
        String phuongThuc = "TIEN_MAT";
        if (ttList != null && !ttList.isEmpty()) {
            phuongThuc = ttList.get(0).getPhuongThuc();
        }
        response.put("phuongThucThanhToan", phuongThuc);
        response.put("trangThaiMa", hd.getTrangThaiHoaDon().getMa());
        response.put("trangThaiTen", hd.getTrangThaiHoaDon().getTen());
        response.put("loaiHoaDon", hd.getLoaiHoaDon());
        response.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : null);
        response.put("tienHang", tongTienHangThucTe);
        response.put("phiVanChuyen", phiShip);
        response.put("tienGiamVoucher", voucherCalculated);
        response.put("tongTien", tongPhaiTra);
        response.put("tienHoan", tongTienHoan);
        response.put("chiTiets", chiTietResponses);

        return response;
    }

    @Transactional
    public Map<String, Object> nextStatus(Integer id, Map<String, String> req) {
        HoaDon hd = hoaDonRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        String currentMa = hd.getTrangThaiHoaDon().getMa();
        String nextMa;
        String ghiChu = req.getOrDefault("ghiChu", "Cập nhật trạng thái tự động");
        switch (currentMa) {
            case "CHO_XAC_NHAN" -> nextMa = "CHO_GIAO";
            case "CHO_GIAO" -> nextMa = "DANG_GIAO";
            case "DANG_GIAO" -> {
                nextMa = "HOAN_THANH";
                hd.setNgayThanhToan(new Date());
            }
            default -> throw new IllegalArgumentException("Trạng thái hiện tại không thể chuyển tiếp");
        }
        TrangThaiHoaDon nextStatus = trangThaiHoaDonRepository.findByMa(nextMa);
        hd.setTrangThaiHoaDon(nextStatus);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                ? nhanVienRepository.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null) : null;

        if (hd.getNhanVien() == null && nhanVienThaoTac != null) {
            hd.setNhanVien(nhanVienThaoTac);
        }
        hoaDonRepository.save(hd);

        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            ThongBao tb = new ThongBao();
            tb.setTaiKhoanId(hd.getKhachHang().getTaiKhoan().getId());
            tb.setLoaiThongBao("ORDER");
            tb.setDaDoc(false);
            if ("CHO_GIAO".equals(nextMa)) {
                tb.setTieuDe("Đơn hàng đã được xác nhận");
                tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã đóng gói xong và đang chờ giao cho đơn vị vận chuyển.");
            } else if ("DANG_GIAO".equals(nextMa)) {
                tb.setTieuDe("Đơn hàng đang trên đường giao");
                tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đang được vận chuyển đến bạn. Vui lòng chú ý điện thoại nhé!");
            } else if ("HOAN_THANH".equals(nextMa)) {
                tb.setTieuDe("Giao hàng thành công");
                tb.setNoiDung("Tuyệt vời! Đơn hàng #" + hd.getMa() + " đã được giao thành công. Cảm ơn bạn đã mua sắm tại Beemate.");
            }
            thongBaoRepository.save(tb);
        }
        lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(nextStatus).nhanVien(nhanVienThaoTac).ghiChu(ghiChu).build());

        return Map.of("message", "Cập nhật thành công", "nextStatus", nextStatus.getTen(), "nextStatusMa", nextStatus.getMa());
    }

    @Transactional
    public Map<String, Object> requestPayment(Integer id) {
        HoaDon hd = hoaDonRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        List<ThanhToan> tts = thanhToanRepository.findByHoaDon_Id(hd.getId());
        String phuongThuc = "TIEN_MAT";
        if (tts != null && !tts.isEmpty()) {
            phuongThuc = tts.get(0).getPhuongThuc();
        }
        if (!"CHUYEN_KHOAN".equalsIgnoreCase(phuongThuc)) {
            throw new IllegalArgumentException("Chỉ áp dụng cho đơn Chuyển khoản");
        }
        TrangThaiHoaDon ttChoThanhToan = trangThaiHoaDonRepository.findByMa("CHO_THANH_TOAN");
        hd.setTrangThaiHoaDon(ttChoThanhToan);
        hoaDonRepository.save(hd);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                ? nhanVienRepository.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null) : null;

        lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(ttChoThanhToan).nhanVien(nhanVienThaoTac).ghiChu("Nhân viên xác nhận chưa nhận được tiền, chuyển về Chờ thanh toán").build());
        return Map.of("message", "Đã chuyển về Chờ thanh toán");
    }

    @Transactional
    public Map<String, Object> huyDon(Integer id, Map<String, String> req) {
        HoaDon hd = hoaDonRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        String currentStatus = hd.getTrangThaiHoaDon().getMa();

        List<ThanhToan> ttList = thanhToanRepository.findByHoaDon_Id(hd.getId());
        boolean daThanhToanOnline = ttList.stream().anyMatch(tt ->
                ("VNPAY".equals(tt.getPhuongThuc()) || "MOMO".equals(tt.getPhuongThuc())) && "THANH_CONG".equals(tt.getTrangThai())
        );

        if (daThanhToanOnline) throw new IllegalArgumentException("Đơn hàng đã được thanh toán trực tuyến. Vui lòng liên hệ Hotline CSKH để yêu cầu hủy và hoàn tiền!");
        if ("HOAN_THANH".equals(currentStatus) || "DANG_GIAO".equals(currentStatus) || "DA_TRA".equals(currentStatus) || "DA_DOI".equals(currentStatus)) {
            throw new IllegalArgumentException("Không thể hủy đơn hàng đã giao hoặc đang giao!");
        }
        if (!"CHO_XAC_NHAN".equals(currentStatus) && !"CHO_THANH_TOAN".equals(currentStatus)) {
            throw new IllegalArgumentException("Không thể hủy đơn hàng đang xử lý hoặc đã hoàn thành!");
        }
        if ("DA_HUY".equals(hd.getTrangThaiHoaDon().getMa())) {
            throw new IllegalArgumentException("Đơn hàng này đã bị hủy từ trước!");
        }

        TrangThaiHoaDon ttHuy = trangThaiHoaDonRepository.findByMa("DA_HUY");
        hd.setTrangThaiHoaDon(ttHuy);

        List<HoaDonChiTiet> hdctList = hoaDonChiTietRepository.findByHoaDonId(hd.getId());
        for (HoaDonChiTiet ct : hdctList) {
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            if (spct != null) {
                spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong());
                sanPhamChiTietRepository.save(spct);
            }
        }

        if (hd.getMaGiamGia() != null) {
            MaGiamGia voucher = hd.getMaGiamGia();
            int luotMoi = voucher.getLuotSuDung() - 1;
            if (luotMoi >= 0) {
                voucher.setLuotSuDung(luotMoi);
                if (!voucher.getTrangThai() && voucher.getNgayKetThuc().isAfter(java.time.LocalDateTime.now())) {
                    voucher.setTrangThai(true);
                }
                maGiamGiaRepository.save(voucher);
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                ? nhanVienRepository.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null) : null;
        if (hd.getNhanVien() == null && nhanVienThaoTac != null) hd.setNhanVien(nhanVienThaoTac);

        hoaDonRepository.save(hd);

        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            ThongBao tb = new ThongBao();
            tb.setTaiKhoanId(hd.getKhachHang().getTaiKhoan().getId());
            tb.setLoaiThongBao("ORDER");
            tb.setDaDoc(false);
            tb.setTieuDe("Đơn hàng đã bị hủy");
            tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã bị hủy. Lý do: " + req.getOrDefault("ghiChu", "Khách hàng / Admin yêu cầu hủy đơn."));
            thongBaoRepository.save(tb);
        }

        lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(ttHuy).nhanVien(nhanVienThaoTac).ghiChu(req.getOrDefault("ghiChu", "Khách hàng/Admin yêu cầu hủy đơn")).build());
        return Map.of("message", "Đơn hàng đã được hủy và hoàn sản phẩm vào kho!");
    }

    @Transactional
    public Map<String, Object> checkout(CheckoutRequest req) {
        if (req.chiTietDonHangs == null || req.chiTietDonHangs.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống, không thể tạo đơn hàng!");
        }
        HoaDon hd = new HoaDon();
        hd.setMa("HD" + System.currentTimeMillis());
        hd.setLoaiHoaDon(1);
        hd.setNgayTao(new Date());

        ThongTinGiaoHang thongTin = ThongTinGiaoHang.builder()
                .tenNguoiNhan(req.tenNguoiNhan)
                .sdtNhan(req.soDienThoai)
                .diaChiChiTiet(req.diaChiGiaoHang)
                .maTinh(req.maTinh)
                .maHuyen(req.maHuyen)
                .maXa(req.maXa)
                .build();
        hd.setThongTinGiaoHang(thongTin);
        hd.setHinhThucGiaoHang(req.hinhThucGiaoHang != null ? req.hinhThucGiaoHang : "GIAO_TAN_NOI");

        if ("GIAO_TAN_NOI".equals(hd.getHinhThucGiaoHang()) && req.maHuyen != null && req.maXa != null) {
            Integer serviceId = 53320;
            Date expectedDate = ghnService.calculateExpectedDeliveryDate(req.maHuyen, req.maXa, serviceId);
            if (expectedDate != null) hd.setNgayNhanHangDuKien(expectedDate);
        } else if ("NHAN_TAI_CUA_HANG".equals(hd.getHinhThucGiaoHang()) && req.ngayHenLayHang != null) {
            hd.setNgayHenLayHang(req.ngayHenLayHang);
        }

        BigDecimal chietKhauNV = BigDecimal.ZERO;
        String ghiChu = req.ghiChu != null ? req.ghiChu : "";
        if (req.isBuyNow != null && req.isBuyNow) ghiChu += " [BUY_NOW]";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaffOrAdminLoggedIn = false;
        String loggedInUsername = "";

        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String role = auth.getAuthorities().iterator().next().getAuthority();
            loggedInUsername = auth.getName();
            if ("ROLE_STAFF".equals(role) || "ROLE_ADMIN".equals(role)) {
                isStaffOrAdminLoggedIn = true;
            } else {
                KhachHang kh = khachHangRepository.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null);
                hd.setKhachHang(kh);
            }
        }

        if (isStaffOrAdminLoggedIn) {
            chietKhauNV = req.tienHang.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
            NhanVien nvMua = nhanVienRepository.findByTaiKhoan_TenDangNhap(loggedInUsername).orElse(null);
            String tenNv = nvMua != null ? nvMua.getHoTen() : loggedInUsername;
            if (!ghiChu.isEmpty()) ghiChu += " - ";
            ghiChu += "[Đơn mua nội bộ bởi: " + tenNv + "]";
            hd.setNhanVien(nvMua);
        }

        hd.setGhiChu(ghiChu.trim());

        if (req.voucherId != null) {
            MaGiamGia voucher = maGiamGiaRepository.findById(req.voucherId).orElse(null);
            if (voucher != null) {
                if (!voucher.getTrangThai() || voucher.getLuotSuDung() >= voucher.getSoLuong() || (voucher.getNgayKetThuc() != null && voucher.getNgayKetThuc().isBefore(java.time.LocalDateTime.now()))) {
                    throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng hoặc hết hạn!");
                }
                if (hd.getKhachHang() != null) {
                    boolean daSuDung = hoaDonRepository.existsByKhachHangIdAndMaGiamGiaIdAndTrangThaiHoaDon_MaNot(hd.getKhachHang().getId(), voucher.getId(), "DA_HUY");
                    if (daSuDung) throw new IllegalArgumentException("Bạn đã sử dụng mã giảm giá này cho một đơn hàng khác rồi!");
                }
                hd.setMaGiamGia(voucher);
                int luotMoi = voucher.getLuotSuDung() + 1;
                voucher.setLuotSuDung(luotMoi);
                if (luotMoi >= voucher.getSoLuong()) voucher.setTrangThai(false);
                maGiamGiaRepository.save(voucher);
            }
        }

        BigDecimal tongTienCuoi = req.tienHang.subtract(req.tienGiam != null ? req.tienGiam : BigDecimal.ZERO).subtract(chietKhauNV).add(req.phiShip != null ? req.phiShip : BigDecimal.ZERO);
        if (tongTienCuoi.compareTo(BigDecimal.ZERO) < 0) tongTienCuoi = BigDecimal.ZERO;

        hd.setGiaTamThoi(req.tienHang);
        hd.setGiaTriKhuyenMai((req.tienGiam != null ? req.tienGiam : BigDecimal.ZERO).add(chietKhauNV));
        hd.setPhiVanChuyen(req.phiShip);
        hd.setGiaTong(tongTienCuoi);

        TrangThaiHoaDon ttBanDau;
        if ("MOMO".equalsIgnoreCase(req.phuongThucThanhToan) || "VNPAY".equalsIgnoreCase(req.phuongThucThanhToan) || "CHUYEN_KHOAN".equalsIgnoreCase(req.phuongThucThanhToan)) {
            ttBanDau = trangThaiHoaDonRepository.findByMa("CHO_THANH_TOAN");
        } else {
            ttBanDau = trangThaiHoaDonRepository.findByMa("CHO_XAC_NHAN");
        }

        hd.setTrangThaiHoaDon(ttBanDau);
        HoaDon savedHd = hoaDonRepository.save(hd);
        BigDecimal tongTienHangChuan = BigDecimal.ZERO;

        for (CheckoutItemRequest itemReq : req.chiTietDonHangs) {
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(itemReq.chiTietSanPhamId).orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
            if (spct.getSoLuong() < itemReq.soLuong) throw new IllegalArgumentException("Sản phẩm " + spct.getSanPham().getTen() + " không đủ số lượng!");

            BigDecimal giaThucTe = (spct.getGiaSauKhuyenMai() != null && spct.getGiaSauKhuyenMai().compareTo(BigDecimal.ZERO) > 0) ? spct.getGiaSauKhuyenMai() : spct.getGiaBan();
            tongTienHangChuan = tongTienHangChuan.add(giaThucTe.multiply(BigDecimal.valueOf(itemReq.soLuong)));

            spct.setSoLuong(spct.getSoLuong() - itemReq.soLuong);
            sanPhamChiTietRepository.save(spct);

            HoaDonChiTiet hdct = new HoaDonChiTiet();
            hdct.setHoaDon(savedHd);
            hdct.setSanPhamChiTiet(spct);
            hdct.setSoLuong(itemReq.soLuong);
            hdct.setGiaTien(giaThucTe);
            hoaDonChiTietRepository.save(hdct);
        }

        BigDecimal tongTienCuoiThucTe = tongTienHangChuan.subtract(req.tienGiam != null ? req.tienGiam : BigDecimal.ZERO).subtract(chietKhauNV).add(req.phiShip != null ? req.phiShip : BigDecimal.ZERO);
        if (tongTienCuoiThucTe.compareTo(BigDecimal.ZERO) < 0) tongTienCuoiThucTe = BigDecimal.ZERO;

        savedHd.setGiaTamThoi(tongTienHangChuan);
        savedHd.setGiaTong(tongTienCuoiThucTe);
        hoaDonRepository.save(savedHd);

        ThanhToan tt = new ThanhToan();
        tt.setHoaDon(savedHd);
        tt.setSoTien(tongTienCuoiThucTe);
        tt.setPhuongThuc(req.phuongThucThanhToan != null ? req.phuongThucThanhToan : "TIEN_MAT");
        tt.setLoaiThanhToan("THANH_TOAN");
        tt.setTrangThai(ttBanDau.getMa().equals("CHO_THANH_TOAN") ? "CHO_THANH_TOAN" : "THANH_CONG");
        tt.setNgayThanhToan(new Date());
        thanhToanRepository.save(tt);

        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(savedHd);
        ls.setTrangThaiHoaDon(ttBanDau);
        ls.setGhiChu(ttBanDau.getMa().equals("CHO_THANH_TOAN") ? "Đang chờ thanh toán online" : "Khách hàng đặt đơn Online");
        ls.setNgayTao(new Date());
        lichSuHoaDonRepository.save(ls);

        if ((req.isBuyNow == null || !req.isBuyNow) && savedHd.getKhachHang() != null && savedHd.getKhachHang().getTaiKhoan() != null) {
            com.example.bee.entities.cart.GioHang gh = gioHangRepository.findByTaiKhoan_Id(savedHd.getKhachHang().getTaiKhoan().getId()).orElse(null);
            if (gh != null) {
                List<com.example.bee.entities.cart.GioHangChiTiet> ghcts = gioHangChiTietRepository.findByGioHang_Id(gh.getId());
                gioHangChiTietRepository.deleteAll(ghcts);
            }
        }

        try {
            if (savedHd.getKhachHang() != null && savedHd.getKhachHang().getTaiKhoan() != null) {
                ThongBao tb = new ThongBao();
                tb.setTaiKhoanId(savedHd.getKhachHang().getTaiKhoan().getId());
                tb.setTieuDe("Đặt hàng thành công");
                tb.setNoiDung("Đơn hàng #" + savedHd.getMa() + " đã được ghi nhận hệ thống.");
                tb.setLoaiThongBao("ORDER");
                tb.setDaDoc(false);
                thongBaoRepository.save(tb);
            }
            if (req.email != null && !req.email.isBlank()) {
                emailService.sendOrderConfirmationEmail(savedHd, req.email);
            }
        } catch (Exception e) {
            System.out.println("Lỗi gửi Email: " + e.getMessage());
        }

        return Map.of("message", "Đặt hàng thành công", "maHoaDon", savedHd.getMa(), "id", savedHd.getId(), "tongTienThucTe", tongTienCuoiThucTe);
    }

    public List<Map<String, Object>> getMyOrders() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) throw new IllegalArgumentException("Vui lòng đăng nhập");
        KhachHang kh = khachHangRepository.findByTaiKhoan_TenDangNhap(auth.getName()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

        List<HoaDon> myOrders = hoaDonRepository.findByKhachHangIdOrderByNgayTaoDesc(kh.getId());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        List<Map<String, Object>> response = new ArrayList<>();
        for (HoaDon hd : myOrders) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", hd.getId());
            map.put("ma", hd.getMa());
            map.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : "");
            map.put("tongTien", hd.getGiaTong());
            map.put("trangThai", hd.getTrangThaiHoaDon().getTen());
            map.put("trangThaiMa", hd.getTrangThaiHoaDon().getMa());
            response.add(map);
        }
        return response;
    }

    public Map<String, Object> traCuuNhanh(String ma) {
        HoaDon hoaDon = hoaDonRepository.findByMa(ma);
        if (hoaDon == null) throw new IllegalArgumentException("Không tìm thấy Hóa đơn");

        Map<Integer, Integer> returnedItems = new HashMap<>();
        BigDecimal tongTienHoan = BigDecimal.ZERO;
        List<YeuCauDoiTra> ycs = yeuCauDoiTraRepository.findAll().stream().filter(y -> y.getHoaDon().getId().equals(hoaDon.getId()) && "HOAN_THANH".equals(y.getTrangThai())).collect(Collectors.toList());
        for (YeuCauDoiTra yc : ycs) {
            if (yc.getSoTienHoan() != null) tongTienHoan = tongTienHoan.add(yc.getSoTienHoan());
            List<ChiTietDoiTra> cts = chiTietDoiTraRepository.findByYeuCauDoiTraId(yc.getId());
            for (ChiTietDoiTra ct : cts) returnedItems.put(ct.getHoaDonChiTiet().getId(), returnedItems.getOrDefault(ct.getHoaDonChiTiet().getId(), 0) + ct.getSoLuong());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> result = new HashMap<>();
        result.put("ma", hoaDon.getMa());
        result.put("ngayTao", hoaDon.getNgayTao() != null ? sdf.format(hoaDon.getNgayTao()) : null);
        result.put("ngayThanhToan", hoaDon.getNgayThanhToan() != null ? sdf.format(hoaDon.getNgayThanhToan()) : null);

        String tenNhan = (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getTenNguoiNhan() != null) ? hoaDon.getThongTinGiaoHang().getTenNguoiNhan() : (hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getHoTen() : "Khách hàng");
        String sdtNhan = (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getSdtNhan() != null) ? hoaDon.getThongTinGiaoHang().getSdtNhan() : (hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getSoDienThoai() : "");
        String dcNhan = (hoaDon.getThongTinGiaoHang() != null) ? hoaDon.getThongTinGiaoHang().getDiaChiChiTiet() : "";

        result.put("tenNguoiNhan", tenNhan);
        result.put("sdtKhachHang", sdtNhan);
        result.put("email", hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getEmail() : "");
        result.put("diaChiGiaoHang", dcNhan);

        List<ThanhToan> ttListTC = thanhToanRepository.findByHoaDon_Id(hoaDon.getId());
        result.put("phuongThucThanhToan", (ttListTC != null && !ttListTC.isEmpty()) ? ttListTC.get(0).getPhuongThuc() : "TIEN_MAT");

        String rawGhiChu = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : "";
        if (rawGhiChu.contains("[BUY_NOW]")) rawGhiChu = rawGhiChu.replace("[BUY_NOW]", "").trim();
        result.put("ghiChu", rawGhiChu);
        result.put("giaTong", hoaDon.getGiaTong());
        result.put("phiVanChuyen", hoaDon.getPhiVanChuyen());
        result.put("giaTriKhuyenMai", hoaDon.getGiaTriKhuyenMai());
        result.put("trangThaiHoaDon", hoaDon.getTrangThaiHoaDon());
        result.put("tienHoan", tongTienHoan);

        List<HoaDonChiTiet> danhSachChiTiet = hoaDonChiTietRepository.findByHoaDon(hoaDon);
        List<Map<String, Object>> listChiTiet = new ArrayList<>();
        if (danhSachChiTiet != null && !danhSachChiTiet.isEmpty()) {
            for (HoaDonChiTiet hdct : danhSachChiTiet) {
                Map<String, Object> item = new HashMap<>();
                item.put("soLuong", hdct.getSoLuong());
                item.put("giaBan", hdct.getGiaTien());
                item.put("soLuongTra", returnedItems.getOrDefault(hdct.getId(), 0));
                if (hdct.getSanPhamChiTiet() != null) {
                    item.put("hinhAnh", hdct.getSanPhamChiTiet().getHinhAnh());
                    if (hdct.getSanPhamChiTiet().getSanPham() != null) item.put("tenSanPham", hdct.getSanPhamChiTiet().getSanPham().getTen());
                    if (hdct.getSanPhamChiTiet().getMauSac() != null && hdct.getSanPhamChiTiet().getKichThuoc() != null) {
                        item.put("thuocTinh", hdct.getSanPhamChiTiet().getMauSac().getTen() + " - " + hdct.getSanPhamChiTiet().getKichThuoc().getTen());
                    }
                }
                listChiTiet.add(item);
            }
        }
        result.put("chiTiets", listChiTiet);
        return result;
    }

    public Map<String, Object> getInvoicePrintData(Integer id) {
        HoaDon hd = hoaDonRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        List<HoaDonChiTiet> listHdct = hoaDonChiTietRepository.findByHoaDonId(id);

        Map<String, Object> storeInfo = new HashMap<>();
        storeInfo.put("tenCuaHang", "BEEMATE STORE");
        storeInfo.put("diaChi", "13 phố Phan Tây Nhạc, phường Xuân Phương, TP Hà Nội");
        storeInfo.put("soDienThoai", "1900 3636");

        Map<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("maHoaDon", hd.getMa());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        orderInfo.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : sdf.format(new Date()));
        orderInfo.put("thuNgan", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Hệ thống");

        String tenNhan = (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getTenNguoiNhan() != null) ? hd.getThongTinGiaoHang().getTenNguoiNhan() : "Khách vãng lai";
        String sdtNhan = (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getSdtNhan() != null) ? hd.getThongTinGiaoHang().getSdtNhan() : "";

        if (hd.getKhachHang() != null) {
            orderInfo.put("tenKhachHang", !tenNhan.equals("Khách vãng lai") ? tenNhan : hd.getKhachHang().getHoTen());
            orderInfo.put("sdtKhachHang", !sdtNhan.isEmpty() ? sdtNhan : hd.getKhachHang().getSoDienThoai());
            orderInfo.put("inThongTinTaiKhoan", true);
        } else {
            orderInfo.put("tenKhachHang", tenNhan);
            orderInfo.put("sdtKhachHang", sdtNhan);
            orderInfo.put("inThongTinTaiKhoan", false);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal tongTienHang = BigDecimal.ZERO;
        for (HoaDonChiTiet ct : listHdct) {
            Map<String, Object> item = new HashMap<>();
            String tenSP = ct.getSanPhamChiTiet().getSanPham().getTen();
            String thuocTinh = ct.getSanPhamChiTiet().getMauSac().getTen() + " - " + ct.getSanPhamChiTiet().getKichThuoc().getTen();
            item.put("ten", tenSP + " (" + thuocTinh + ")");
            item.put("soLuong", ct.getSoLuong());
            item.put("donGia", ct.getGiaTien());
            BigDecimal thanhTien = ct.getGiaTien().multiply(BigDecimal.valueOf(ct.getSoLuong()));
            item.put("thanhTien", thanhTien);
            tongTienHang = tongTienHang.add(thanhTien);
            items.add(item);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("tongTienHang", tongTienHang);
        BigDecimal phiShip = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        summary.put("phiVanChuyen", phiShip);
        BigDecimal tongPhaiTra = hd.getGiaTong() != null ? hd.getGiaTong() : BigDecimal.ZERO;
        BigDecimal giamGia = tongTienHang.add(phiShip).subtract(tongPhaiTra);
        if (giamGia.compareTo(BigDecimal.ZERO) < 0) giamGia = BigDecimal.ZERO;
        summary.put("giamGia", giamGia);
        summary.put("tongThanhToan", tongPhaiTra);

        List<ThanhToan> ttListPrint = thanhToanRepository.findByHoaDon_Id(hd.getId());
        summary.put("phuongThuc", (ttListPrint != null && !ttListPrint.isEmpty()) ? ttListPrint.get(0).getPhuongThuc() : "TIEN_MAT");

        return Map.of("store", storeInfo, "order", orderInfo, "items", items, "summary", summary);
    }

    public List<Map<String, Object>> getNewOnlineOrders() {
        List<String> pendingStatuses = Arrays.asList("CHO_XAC_NHAN", "CHO_GIAO", "CHO_THANH_TOAN");
        List<HoaDon> list = hoaDonRepository.findTop5ByLoaiHoaDonAndTrangThaiHoaDon_MaInOrderByNgayTaoDesc(1, pendingStatuses);
        List<Map<String, Object>> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (HoaDon hd : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", hd.getId());
            map.put("ma", hd.getMa());
            String tenNhan = (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getTenNguoiNhan() != null) ? hd.getThongTinGiaoHang().getTenNguoiNhan() : "Khách hàng";
            map.put("khachHang", tenNhan);
            map.put("tongTien", hd.getGiaTong());
            map.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : "");
            result.add(map);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> confirmTransferPayment(Integer id) {
        HoaDon hd = hoaDonRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        TrangThaiHoaDon ttChoXacNhan = trangThaiHoaDonRepository.findByMa("CHO_XAC_NHAN");
        hd.setTrangThaiHoaDon(ttChoXacNhan);
        hd.setNgayThanhToan(new Date());
        hoaDonRepository.save(hd);

        boolean isBuyNow = hd.getGhiChu() != null && hd.getGhiChu().contains("[BUY_NOW]");
        if (!isBuyNow && hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            com.example.bee.entities.cart.GioHang gh = gioHangRepository.findByTaiKhoan_Id(hd.getKhachHang().getTaiKhoan().getId()).orElse(null);
            if (gh != null) gioHangChiTietRepository.deleteAll(gioHangChiTietRepository.findByGioHang_Id(gh.getId()));
        }

        lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hd).trangThaiHoaDon(ttChoXacNhan).ghiChu("Khách hàng xác nhận đã chuyển khoản qua QR code").ngayTao(new Date()).build());
        return Map.of("message", "Đã cập nhật trạng thái chờ xác nhận");
    }

    public List<Integer> getMyUsedVouchers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) return Collections.emptyList();
        Optional<KhachHang> khOpt = khachHangRepository.findByTaiKhoan_TenDangNhap(auth.getName());
        if (khOpt.isEmpty()) return Collections.emptyList();

        return hoaDonRepository.findByKhachHangIdOrderByNgayTaoDesc(khOpt.get().getId()).stream()
                .filter(hd -> !"DA_HUY".equals(hd.getTrangThaiHoaDon().getMa()) && hd.getMaGiamGia() != null)
                .map(hd -> hd.getMaGiamGia().getId())
                .distinct().collect(Collectors.toList());
    }
}