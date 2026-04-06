package com.example.bee.controllers.api.order;

import com.example.bee.dto.HoaDonChiTietResponse;
import com.example.bee.dto.HoaDonResponse;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.notification.ThongBao;
import com.example.bee.entities.order.HoaDon;
import com.example.bee.entities.order.HoaDonChiTiet;
import com.example.bee.entities.order.LichSuHoaDon;
import com.example.bee.entities.order.TrangThaiHoaDon;
import com.example.bee.entities.order.YeuCauDoiTra;
import com.example.bee.entities.order.ChiTietDoiTra;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.entities.user.NhanVien;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
import com.example.bee.repositories.order.HoaDonChiTietRepository;
import com.example.bee.repositories.order.HoaDonRepository;
import com.example.bee.repositories.order.LichSuHoaDonRepository;
import com.example.bee.repositories.order.TrangThaiHoaDonRepository;
import com.example.bee.repositories.order.YeuCauDoiTraRepository;
import com.example.bee.repositories.order.ChiTietDoiTraRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import com.example.bee.repositories.role.NhanVienRepository;
import com.example.bee.services.EmailService;
import com.example.bee.utils.MomoSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hoa-don")
@RequiredArgsConstructor
public class OrderApi {

    private final HoaDonRepository hdRepo;
    private final TrangThaiHoaDonRepository ttRepo;
    private final LichSuHoaDonRepository lsRepo;
    private final HoaDonChiTietRepository hdctRepo;
    private final NhanVienRepository nvRepo;
    private final SanPhamChiTietRepository spctRepo;
    private final KhachHangRepository khRepo;
    private final EmailService emailService;
    private final ThongBaoRepository thongBaoRepository;
    private final MaGiamGiaRepository maGiamGiaRepo;

    private final YeuCauDoiTraRepository ycRepo;
    private final ChiTietDoiTraRepository ctRepo;

    @Value("${momo.partnerCode}")
    private String partnerCode;
    @Value("${momo.accessKey}")
    private String accessKey;
    @Value("${momo.secretKey}")
    private String secretKey;
    @Value("${momo.endpoint}")
    private String endpoint;
    @Value("${momo.notifyUrl}")
    private String notifyUrl;

    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;
    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;
    @Value("${vnpay.payUrl}")
    private String vnp_PayUrl;

    @GetMapping("/don-hang")
    public ResponseEntity<?> getDonHangChoXuLy(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) Integer loaiHoaDon,
            @RequestParam(required = false) String phuongThucThanhToan,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        q = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        phuongThucThanhToan = (phuongThucThanhToan != null && !phuongThucThanhToan.trim().isEmpty()) ? phuongThucThanhToan : null;

        if (endDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            endDate = cal.getTime();
        }

        Page<HoaDon> result = hdRepo.searchDonHangChoXuLy(q, statusId, loaiHoaDon, phuongThucThanhToan, startDate, endDate, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/lich-su")
    public ResponseEntity<?> getLichSu(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) Integer nhanVienId,
            @RequestParam(required = false) Integer loaiHoaDon,
            @RequestParam(required = false) String phuongThucThanhToan,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        q = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        phuongThucThanhToan = (phuongThucThanhToan != null && !phuongThucThanhToan.trim().isEmpty()) ? phuongThucThanhToan : null;

        if (endDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            endDate = cal.getTime();
        }

        Page<HoaDon> result = hdRepo.searchLichSuHoaDon(q, statusId, nhanVienId, loaiHoaDon, phuongThucThanhToan, startDate, endDate, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Integer id) {
        HoaDon hd = hdRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<HoaDonChiTiet> listHdct = hdctRepo.findByHoaDonId(id);

        Map<Integer, Integer> returnedItems = new HashMap<>();
        Date ngayCapNhatCuoi = hd.getNgayThanhToan();

        // 🌟 LẤY TỔNG TIỀN HOÀN LẠI CHO KHÁCH
        BigDecimal tongTienHoan = BigDecimal.ZERO;

        List<YeuCauDoiTra> ycs = ycRepo.findAll().stream().filter(y -> y.getHoaDon().getId().equals(id) && "HOAN_THANH".equals(y.getTrangThai())).collect(Collectors.toList());
        for (YeuCauDoiTra yc : ycs) {
            if (ngayCapNhatCuoi == null || (yc.getNgayXuLy() != null && yc.getNgayXuLy().after(ngayCapNhatCuoi))) {
                ngayCapNhatCuoi = yc.getNgayXuLy();
            }
            if (yc.getSoTienHoan() != null) {
                tongTienHoan = tongTienHoan.add(yc.getSoTienHoan());
            }
            List<ChiTietDoiTra> cts = ctRepo.findByYeuCauDoiTraId(yc.getId());
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
            var nhanVienDangNhap = nvRepo.findByTaiKhoan_TenDangNhap(username).orElse(null);
            if (nhanVienDangNhap != null) {
                tenNhanVienHienTai = nhanVienDangNhap.getHoTen();
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", hd.getId());
        response.put("ma", hd.getMa());
        response.put("tenKhachHang", hd.getKhachHang() != null ? hd.getKhachHang().getHoTen() : (hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "Khách vãng lai"));

        response.put("sdtKhachHang", hd.getSdtNhan() != null ? hd.getSdtNhan() : (hd.getKhachHang() != null ? hd.getKhachHang().getSoDienThoai() : ""));
        response.put("email", hd.getKhachHang() != null ? hd.getKhachHang().getEmail() : "");
        response.put("ghiChu", hd.getGhiChu());
        response.put("ngayThanhToan", hd.getNgayThanhToan() != null ? sdf.format(hd.getNgayThanhToan()) : null);
        response.put("ngayCapNhat", ngayCapNhatCuoi != null ? sdf.format(ngayCapNhatCuoi) : null);

        response.put("tenNhanVien", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : tenNhanVienHienTai);
        response.put("diaChiGiaoHang", hd.getDiaChiGiaoHang());
        response.put("phuongThucThanhToan", hd.getPhuongThucThanhToan());
        response.put("trangThaiMa", hd.getTrangThaiHoaDon().getMa());
        response.put("trangThaiTen", hd.getTrangThaiHoaDon().getTen());
        response.put("loaiHoaDon", hd.getLoaiHoaDon());
        response.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : null);
        response.put("tienHang", tongTienHangThucTe);
        response.put("phiVanChuyen", phiShip);
        response.put("tienGiamVoucher", voucherCalculated);
        response.put("tongTien", tongPhaiTra);
        response.put("tienHoan", tongTienHoan); // 🌟 Truyền dữ liệu tiền hoàn
        response.put("chiTiets", chiTietResponses);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/next-status")
    @Transactional
    public ResponseEntity<?> nextStatus(@PathVariable Integer id, @RequestBody Map<String, String> req) {
        HoaDon hd = hdRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));
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
            default ->
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái hiện tại không thể chuyển tiếp");
        }
        TrangThaiHoaDon nextStatus = ttRepo.findByMa(nextMa);
        hd.setTrangThaiHoaDon(nextStatus);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                ? nvRepo.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null)
                : null;
        if (hd.getNhanVien() == null && nhanVienThaoTac != null) {
            hd.setNhanVien(nhanVienThaoTac);
        }
        hdRepo.save(hd);
        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            ThongBao tb = new ThongBao();
            tb.setTaiKhoanId(hd.getKhachHang().getTaiKhoan().getId());
            tb.setLoaiThongBao("ORDER");
            tb.setDaDoc(false);
            if ("CHO_GIAO".equals(nextMa)) {
                tb.setTieuDe("Đơn hàng đã được xác nhận");
                tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã đóng gói xong và đang chờ giao cho đơn vị vận chuyển.");
                thongBaoRepository.save(tb);
            } else if ("DANG_GIAO".equals(nextMa)) {
                tb.setTieuDe("Đơn hàng đang trên đường giao");
                tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đang được vận chuyển đến bạn. Vui lòng chú ý điện thoại nhé!");
                thongBaoRepository.save(tb);
            } else if ("HOAN_THANH".equals(nextMa)) {
                tb.setTieuDe("Giao hàng thành công");
                tb.setNoiDung("Tuyệt vời! Đơn hàng #" + hd.getMa() + " đã được giao thành công. Cảm ơn bạn đã mua sắm tại Beemate.");
                thongBaoRepository.save(tb);
            }
        }
        lsRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(nextStatus)
                .nhanVien(nhanVienThaoTac)
                .ghiChu(ghiChu)
                .build());
        return ResponseEntity.ok(Map.of(
                "message", "Cập nhật thành công",
                "nextStatus", nextStatus.getTen(),
                "nextStatusMa", nextStatus.getMa()
        ));
    }

    @PostMapping("/{id}/request-payment")
    @Transactional
    public ResponseEntity<?> requestPayment(@PathVariable Integer id) {
        try {
            HoaDon hd = hdRepo.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));

            if (!"CHUYEN_KHOAN".equalsIgnoreCase(hd.getPhuongThucThanhToan())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Chỉ áp dụng cho đơn Chuyển khoản"));
            }

            TrangThaiHoaDon ttChoThanhToan = ttRepo.findByMa("CHO_THANH_TOAN");
            if (ttChoThanhToan == null) throw new RuntimeException("Chưa cấu hình trạng thái CHO_THANH_TOAN trong DB");

            hd.setTrangThaiHoaDon(ttChoThanhToan);
            hdRepo.save(hd);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            var nhanVienThaoTac = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                    ? nvRepo.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null)
                    : null;

            lsRepo.save(LichSuHoaDon.builder()
                    .hoaDon(hd)
                    .trangThaiHoaDon(ttChoThanhToan)
                    .nhanVien(nhanVienThaoTac)
                    .ghiChu("Nhân viên xác nhận chưa nhận được tiền, chuyển về Chờ thanh toán")
                    .build());

            return ResponseEntity.ok(Map.of("message", "Đã chuyển về Chờ thanh toán"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/huy")
    @Transactional
    public ResponseEntity<?> huyDon(@PathVariable Integer id, @RequestBody Map<String, String> req) {
        HoaDon hd = hdRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));
        String currentStatus = hd.getTrangThaiHoaDon().getMa();
        if ("HOAN_THANH".equals(currentStatus) || "DANG_GIAO".equals(currentStatus) || "DA_TRA".equals(currentStatus) || "DA_DOI".equals(currentStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể hủy đơn hàng đã giao hoặc đang giao!"));
        }
        if (!"CHO_XAC_NHAN".equals(currentStatus) && !"CHO_THANH_TOAN".equals(currentStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể hủy đơn hàng đang xử lý hoặc đã hoàn thành!"));
        }
        // Tránh tình trạng bấm hủy 2 lần làm kho bị cộng dồn sai
        if ("DA_HUY".equals(hd.getTrangThaiHoaDon().getMa())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Đơn hàng này đã bị hủy từ trước!"));
        }

        TrangThaiHoaDon ttHuy = ttRepo.findByMa("DA_HUY");
        hd.setTrangThaiHoaDon(ttHuy);

        // 🌟 BỔ SUNG LOGIC QUAN TRỌNG: HỒI LẠI SỐ LƯỢNG VÀO KHO
        List<HoaDonChiTiet> hdctList = hdctRepo.findByHoaDonId(hd.getId());
        for (HoaDonChiTiet ct : hdctList) {
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            if (spct != null) {
                // Cộng trả lại số lượng khách đã mua vào tồn kho
                spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong());
                spctRepo.save(spct);
            }
        }

        // Hoàn lại lượt sử dụng cho Voucher (nếu có)
        if (hd.getMaGiamGia() != null) {
            MaGiamGia voucher = hd.getMaGiamGia();
            int luotMoi = voucher.getLuotSuDung() - 1;
            if (luotMoi >= 0) {
                voucher.setLuotSuDung(luotMoi);
                if (!voucher.getTrangThai() && voucher.getNgayKetThuc().isAfter(java.time.LocalDateTime.now())) {
                    voucher.setTrangThai(true);
                }
                maGiamGiaRepo.save(voucher);
            }
        }

        // Lưu thông tin nhân viên thao tác
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                ? nvRepo.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null)
                : null;

        if (hd.getNhanVien() == null && nhanVienThaoTac != null) {
            hd.setNhanVien(nhanVienThaoTac);
        }

        hdRepo.save(hd);

        // Bắn thông báo cho khách hàng
        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            ThongBao tb = new ThongBao();
            tb.setTaiKhoanId(hd.getKhachHang().getTaiKhoan().getId());
            tb.setLoaiThongBao("ORDER");
            tb.setDaDoc(false);
            tb.setTieuDe("Đơn hàng đã bị hủy");
            tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã bị hủy. Lý do: " + req.getOrDefault("ghiChu", "Khách hàng / Admin yêu cầu hủy đơn."));
            thongBaoRepository.save(tb);
        }

        // Ghi lại lịch sử
        lsRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(ttHuy)
                .nhanVien(nhanVienThaoTac)
                .ghiChu(req.getOrDefault("ghiChu", "Khách hàng/Admin yêu cầu hủy đơn"))
                .build());

        return ResponseEntity.ok(Map.of("message", "Đơn hàng đã được hủy và hoàn sản phẩm vào kho!"));
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest req) {
        try {
            HoaDon hd = new HoaDon();
            hd.setMa("HD" + System.currentTimeMillis());
            hd.setLoaiHoaDon(1);
            hd.setNgayTao(new Date());

            hd.setTenNguoiNhan(req.tenNguoiNhan);
            hd.setSdtNhan(req.soDienThoai);
            hd.setDiaChiGiaoHang(req.diaChiGiaoHang);
            hd.setPhuongThucThanhToan(req.phuongThucThanhToan);

            BigDecimal chietKhauNV = BigDecimal.ZERO;
            String ghiChu = req.ghiChu != null ? req.ghiChu : "";

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isStaffOrAdminLoggedIn = false;
            String loggedInUsername = "";

            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                String role = auth.getAuthorities().iterator().next().getAuthority();
                loggedInUsername = auth.getName();

                if ("ROLE_STAFF".equals(role) || "ROLE_ADMIN".equals(role)) {
                    isStaffOrAdminLoggedIn = true;
                } else {
                    KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null);
                    hd.setKhachHang(kh);
                }
            }

            if (isStaffOrAdminLoggedIn) {
                chietKhauNV = req.tienHang.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);

                NhanVien nvMua = nvRepo.findByTaiKhoan_TenDangNhap(loggedInUsername).orElse(null);
                String tenNv = nvMua != null ? nvMua.getHoTen() : loggedInUsername;

                if (!ghiChu.isEmpty()) ghiChu += " - ";
                ghiChu += "[Đơn mua nội bộ bởi: " + tenNv + "]";

                hd.setNhanVien(nvMua);
            }

            hd.setGhiChu(ghiChu.trim());

            if (req.voucherId != null) {
                MaGiamGia voucher = maGiamGiaRepo.findById(req.voucherId).orElse(null);
                if (voucher != null) {
                    // 🌟 VALIDATE VOUCHER
                    if (!voucher.getTrangThai() ||
                            voucher.getLuotSuDung() >= voucher.getSoLuong() ||
                            (voucher.getNgayKetThuc() != null && voucher.getNgayKetThuc().isBefore(java.time.LocalDateTime.now()))) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Mã giảm giá đã hết lượt sử dụng hoặc hết hạn!"));
                    }

                    // 🌟 BỔ SUNG: KIỂM TRA MỖI KHÁCH CHỈ ĐƯỢC DÙNG 1 LẦN
                    if (hd.getKhachHang() != null) {
                        boolean daSuDung = hdRepo.existsByKhachHangIdAndMaGiamGiaIdAndTrangThaiHoaDon_MaNot(hd.getKhachHang().getId(), voucher.getId(), "DA_HUY");
                        if (daSuDung) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Bạn đã sử dụng mã giảm giá này cho một đơn hàng khác rồi!"));
                        }
                    }

                    hd.setMaGiamGia(voucher);
                    int luotMoi = voucher.getLuotSuDung() + 1;
                    voucher.setLuotSuDung(luotMoi);
                    if (luotMoi >= voucher.getSoLuong()) {
                        voucher.setTrangThai(false);
                    }
                    maGiamGiaRepo.save(voucher);
                }
            }

            BigDecimal tongTienCuoi = req.tienHang
                    .subtract(req.tienGiam != null ? req.tienGiam : BigDecimal.ZERO)
                    .subtract(chietKhauNV)
                    .add(req.phiShip != null ? req.phiShip : BigDecimal.ZERO);

            if (tongTienCuoi.compareTo(BigDecimal.ZERO) < 0) {
                tongTienCuoi = BigDecimal.ZERO;
            }

            hd.setGiaTamThoi(req.tienHang);
            hd.setGiaTriKhuyenMai((req.tienGiam != null ? req.tienGiam : BigDecimal.ZERO).add(chietKhauNV));
            hd.setPhiVanChuyen(req.phiShip);
            hd.setGiaTong(tongTienCuoi);

            TrangThaiHoaDon ttBanDau;
            if ("MOMO".equalsIgnoreCase(req.phuongThucThanhToan) || "VNPAY".equalsIgnoreCase(req.phuongThucThanhToan)) {
                ttBanDau = ttRepo.findByMa("CHO_THANH_TOAN");
            } else {
                ttBanDau = ttRepo.findByMa("CHO_XAC_NHAN");
            }

            if (ttBanDau == null) throw new RuntimeException("Lỗi cấu hình trạng thái trong DB");
            hd.setTrangThaiHoaDon(ttBanDau);

            HoaDon savedHd = hdRepo.save(hd);
            BigDecimal tongTienHangChuan = BigDecimal.ZERO;
            for (CheckoutItemRequest itemReq : req.chiTietDonHangs) {
                SanPhamChiTiet spct = spctRepo.findById(itemReq.chiTietSanPhamId)
                        .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

                if (spct.getSoLuong() < itemReq.soLuong) {
                    throw new RuntimeException("Sản phẩm " + spct.getSanPham().getTen() + " không đủ số lượng!");
                }

                BigDecimal giaThucTe = (spct.getGiaSauKhuyenMai() != null && spct.getGiaSauKhuyenMai().compareTo(BigDecimal.ZERO) > 0)
                        ? spct.getGiaSauKhuyenMai() : spct.getGiaBan();
                tongTienHangChuan = tongTienHangChuan.add(giaThucTe.multiply(BigDecimal.valueOf(itemReq.soLuong)));

                spct.setSoLuong(spct.getSoLuong() - itemReq.soLuong);
                spctRepo.save(spct);

                HoaDonChiTiet hdct = new HoaDonChiTiet();
                hdct.setHoaDon(savedHd);
                hdct.setSanPhamChiTiet(spct);
                hdct.setSoLuong(itemReq.soLuong);
                hdct.setGiaTien(giaThucTe);
                hdctRepo.save(hdct);
            }
            BigDecimal tongTienCuoiThucTe = tongTienHangChuan
                    .subtract(req.tienGiam != null ? req.tienGiam : BigDecimal.ZERO)
                    .subtract(chietKhauNV)
                    .add(req.phiShip != null ? req.phiShip : BigDecimal.ZERO);
            if (tongTienCuoiThucTe.compareTo(BigDecimal.ZERO) < 0) tongTienCuoiThucTe = BigDecimal.ZERO;
            savedHd.setGiaTamThoi(tongTienHangChuan);
            savedHd.setGiaTong(tongTienCuoiThucTe);
            hdRepo.save(savedHd);

            LichSuHoaDon ls = new LichSuHoaDon();
            ls.setHoaDon(savedHd);
            ls.setTrangThaiHoaDon(ttBanDau);
            ls.setGhiChu(ttBanDau.getMa().equals("CHO_THANH_TOAN") ? "Đang chờ thanh toán qua MoMo" : "Khách hàng đặt đơn Online (COD)");
            ls.setNgayTao(new Date());
            lsRepo.save(ls);

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

            return ResponseEntity.ok(Map.of("message", "Đặt hàng thành công", "maHoaDon", savedHd.getMa(), "id", savedHd.getId(), "tongTienThucTe", tongTienCuoi));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập"));
        }
        String username = auth.getName();
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        List<HoaDon> myOrders = hdRepo.findByKhachHangIdOrderByNgayTaoDesc(kh.getId());
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
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tra-cuu/{ma}")
    public ResponseEntity<?> traCuuNhanh(@PathVariable String ma) {
        HoaDon hoaDon = hdRepo.findByMa(ma);
        if (hoaDon == null) {
            return ResponseEntity.notFound().build();
        }

        Map<Integer, Integer> returnedItems = new HashMap<>();
        BigDecimal tongTienHoan = BigDecimal.ZERO;
        List<YeuCauDoiTra> ycs = ycRepo.findAll().stream().filter(y -> y.getHoaDon().getId().equals(hoaDon.getId()) && "HOAN_THANH".equals(y.getTrangThai())).collect(Collectors.toList());
        for (YeuCauDoiTra yc : ycs) {
            if (yc.getSoTienHoan() != null) {
                tongTienHoan = tongTienHoan.add(yc.getSoTienHoan());
            }
            List<ChiTietDoiTra> cts = ctRepo.findByYeuCauDoiTraId(yc.getId());
            for (ChiTietDoiTra ct : cts) {
                Integer hdctId = ct.getHoaDonChiTiet().getId();
                returnedItems.put(hdctId, returnedItems.getOrDefault(hdctId, 0) + ct.getSoLuong());
            }
        }
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("ma", hoaDon.getMa());
        result.put("ngayTao", hoaDon.getNgayTao() != null ? sdf.format(hoaDon.getNgayTao()) : null);
        result.put("ngayThanhToan", hoaDon.getNgayThanhToan() != null ? sdf.format(hoaDon.getNgayThanhToan()) : null);
        result.put("tenNguoiNhan", hoaDon.getTenNguoiNhan() != null ? hoaDon.getTenNguoiNhan() : (hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getHoTen() : "Khách hàng"));
        result.put("sdtKhachHang", hoaDon.getSdtNhan() != null ? hoaDon.getSdtNhan() : (hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getSoDienThoai() : ""));
        result.put("email", hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getEmail() : "");
        result.put("diaChiGiaoHang", hoaDon.getDiaChiGiaoHang());
        result.put("phuongThucThanhToan", hoaDon.getPhuongThucThanhToan());
        result.put("ghiChu", hoaDon.getGhiChu());
        result.put("giaTong", hoaDon.getGiaTong());
        result.put("phiVanChuyen", hoaDon.getPhiVanChuyen());
        result.put("giaTriKhuyenMai", hoaDon.getGiaTriKhuyenMai());
        result.put("trangThaiHoaDon", hoaDon.getTrangThaiHoaDon());
        result.put("tienHoan", tongTienHoan); // 🌟 Thêm tiền hoàn

        java.util.List<com.example.bee.entities.order.HoaDonChiTiet> danhSachChiTiet = hdctRepo.findByHoaDon(hoaDon);
        java.util.List<java.util.Map<String, Object>> listChiTiet = new java.util.ArrayList<>();
        if (danhSachChiTiet != null && !danhSachChiTiet.isEmpty()) {
            for (com.example.bee.entities.order.HoaDonChiTiet hdct : danhSachChiTiet) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("soLuong", hdct.getSoLuong());
                item.put("giaBan", hdct.getGiaTien());
                item.put("soLuongTra", returnedItems.getOrDefault(hdct.getId(), 0)); // 🌟 Thêm số lượng trả
                if (hdct.getSanPhamChiTiet() != null) {
                    item.put("hinhAnh", hdct.getSanPhamChiTiet().getHinhAnh());
                    if (hdct.getSanPhamChiTiet().getSanPham() != null) {
                        item.put("tenSanPham", hdct.getSanPhamChiTiet().getSanPham().getTen());
                    }
                    if (hdct.getSanPhamChiTiet().getMauSac() != null && hdct.getSanPhamChiTiet().getKichThuoc() != null) {
                        item.put("thuocTinh", hdct.getSanPhamChiTiet().getMauSac().getTen() + " - " + hdct.getSanPhamChiTiet().getKichThuoc().getTen());
                    }
                }
                listChiTiet.add(item);
            }
        }
        result.put("chiTiets", listChiTiet);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/print-data")
    public ResponseEntity<?> getInvoicePrintData(@PathVariable Integer id) {
        HoaDon hd = hdRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));
        List<HoaDonChiTiet> listHdct = hdctRepo.findByHoaDonId(id);
        Map<String, Object> storeInfo = new HashMap<>();
        storeInfo.put("tenCuaHang", "BEEMATE STORE");
        storeInfo.put("diaChi", "13 phố Phan Tây Nhạc, phường Xuân Phương, TP Hà Nội");
        storeInfo.put("soDienThoai", "1900 3636");
        Map<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("maHoaDon", hd.getMa());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        orderInfo.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : sdf.format(new Date()));
        orderInfo.put("thuNgan", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Hệ thống");
        if (hd.getKhachHang() != null) {
            orderInfo.put("tenKhachHang", hd.getKhachHang().getHoTen());
            orderInfo.put("sdtKhachHang", hd.getKhachHang().getSoDienThoai());
            orderInfo.put("inThongTinTaiKhoan", true);
        } else {
            orderInfo.put("tenKhachHang", hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "Khách vãng lai");
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
        summary.put("phuongThuc", hd.getPhuongThucThanhToan());
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("store", storeInfo);
        responseData.put("order", orderInfo);
        responseData.put("items", items);
        responseData.put("summary", summary);
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/momo-payment/{invoiceId}")
    @Transactional
    public ResponseEntity<?> getMomoUrlOnline(@PathVariable Integer invoiceId, @RequestBody Map<String, Object> payload) {
        try {
            HoaDon hd = hdRepo.findById(invoiceId).orElseThrow(() -> new RuntimeException("Không tìm thấy Hóa đơn"));

            BigDecimal totalAmount = hd.getGiaTong();

            hd.setPhuongThucThanhToan("MOMO");
            hdRepo.save(hd);

            String rId = String.valueOf(System.currentTimeMillis());
            String oId = hd.getMa() + "_" + rId;
            String amountStr = totalAmount.setScale(0, RoundingMode.HALF_UP).toString();

            String onlineReturnUrl = "http://localhost:8080/api/hoa-don/momo-callback";

            String rawHash = "accessKey=" + accessKey.trim() + "&amount=" + amountStr + "&extraData=&ipnUrl=" + notifyUrl.trim() + "&orderId=" + oId + "&orderInfo=ThanhToan_Online_" + hd.getMa() + "&partnerCode=" + partnerCode.trim() + "&redirectUrl=" + onlineReturnUrl + "&requestId=" + rId + "&requestType=captureWallet";
            String signature = MomoSecurity.signHmacSHA256(rawHash, secretKey.trim());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("partnerCode", partnerCode.trim());
            body.put("accessKey", accessKey.trim());
            body.put("requestId", rId);
            body.put("amount", Long.valueOf(amountStr));
            body.put("orderId", oId);
            body.put("orderInfo", "ThanhToan_Online_" + hd.getMa());
            body.put("redirectUrl", onlineReturnUrl);
            body.put("ipnUrl", notifyUrl.trim());
            body.put("extraData", "");
            body.put("requestType", "captureWallet");
            body.put("signature", signature);
            body.put("lang", "vi");

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.postForObject(endpoint.trim(), body, Map.class);

            return ResponseEntity.ok(Map.of("payUrl", response.get("payUrl")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    @GetMapping("/momo-callback")
    @Transactional
    public ResponseEntity<Void> momoCallbackOnline(HttpServletRequest request) {
        String resultCode = request.getParameter("resultCode");
        String orderId = request.getParameter("orderId");

        String redirectUrl = "http://localhost:8080/customer#home";

        try {
            if (orderId != null && orderId.contains("_")) {
                String maHD = orderId.split("_")[0];
                HoaDon hd = hdRepo.findByMa(maHD);

                if (hd != null && "CHO_THANH_TOAN".equals(hd.getTrangThaiHoaDon().getMa())) {

                    if ("0".equals(resultCode)) {
                        TrangThaiHoaDon ttChoXacNhan = ttRepo.findByMa("CHO_XAC_NHAN");
                        hd.setTrangThaiHoaDon(ttChoXacNhan);
                        hd.setNgayThanhToan(new Date());
                        hdRepo.save(hd);

                        lsRepo.save(LichSuHoaDon.builder()
                                .hoaDon(hd).trangThaiHoaDon(ttChoXacNhan)
                                .ghiChu("Thanh toán MoMo thành công").build());

                        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                            ThongBao tb = new ThongBao();
                            tb.setTaiKhoanId(hd.getKhachHang().getTaiKhoan().getId());
                            tb.setTieuDe("Thanh toán thành công");
                            tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã được thanh toán qua MoMo.");
                            tb.setLoaiThongBao("ORDER");
                            tb.setDaDoc(false);
                            thongBaoRepository.save(tb);
                        }

                        redirectUrl = "http://localhost:8080/customer#account";

                    } else {
                        TrangThaiHoaDon ttHuy = ttRepo.findByMa("DA_HUY");
                        hd.setTrangThaiHoaDon(ttHuy);
                        hdRepo.save(hd);

                        List<HoaDonChiTiet> hdctList = hdctRepo.findByHoaDonId(hd.getId());
                        for (HoaDonChiTiet ct : hdctList) {
                            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
                            spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong());
                            spctRepo.save(spct);
                        }

                        if (hd.getMaGiamGia() != null) {
                            MaGiamGia voucher = hd.getMaGiamGia();
                            int luotMoi = voucher.getLuotSuDung() - 1;
                            if (luotMoi >= 0) {
                                voucher.setLuotSuDung(luotMoi);
                                if (!voucher.getTrangThai() && voucher.getNgayKetThuc().isAfter(java.time.LocalDateTime.now())) {
                                    voucher.setTrangThai(true);
                                }
                                maGiamGiaRepo.save(voucher);
                            }
                        }

                        lsRepo.save(LichSuHoaDon.builder()
                                .hoaDon(hd).trangThaiHoaDon(ttHuy)
                                .ghiChu("Hủy đơn do khách hàng không hoàn tất thanh toán MoMo").build());

                        redirectUrl = "http://localhost:8080/customer#checkout";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
    }

    @PostMapping("/vnpay-payment/{invoiceId}")
    @Transactional
    public ResponseEntity<?> getVnPayUrlOnline(@PathVariable Integer invoiceId, @RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            HoaDon hd = hdRepo.findById(invoiceId).orElseThrow(() -> new RuntimeException("Không tìm thấy Hóa đơn"));
            BigDecimal totalAmount = hd.getGiaTong();

            hd.setPhuongThucThanhToan("VNPAY");
            hdRepo.save(hd);

            String vnp_TxnRef = hd.getMa() + "_" + System.currentTimeMillis();
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode.trim());
            vnp_Params.put("vnp_Amount", String.valueOf(totalAmount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "ThanhToan_Online_" + hd.getMa());
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");

            String onlineReturnUrl = "http://localhost:8080/api/hoa-don/vnpay-callback";
            vnp_Params.put("vnp_ReturnUrl", onlineReturnUrl);
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext(); ) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    String encodedValue = java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.UTF_8.toString()).replace("+", "%20");
                    String encodedName = java.net.URLEncoder.encode(fieldName, java.nio.charset.StandardCharsets.UTF_8.toString());
                    hashData.append(fieldName).append('=').append(encodedValue);
                    query.append(encodedName).append('=').append(encodedValue);
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            String vnp_SecureHash = com.example.bee.utils.VnPayUtil.hmacSHA512(vnp_HashSecret.trim(), hashData.toString());
            String paymentUrl = vnp_PayUrl.trim() + "?" + query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;

            return ResponseEntity.ok(Map.of("payUrl", paymentUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/vnpay-callback")
    @Transactional
    public ResponseEntity<Void> vnpayCallbackOnline(HttpServletRequest request) {
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");

        String redirectUrl = "http://localhost:8080/customer#home";

        try {
            if (vnp_TxnRef != null && vnp_TxnRef.contains("_")) {
                String maHD = vnp_TxnRef.split("_")[0];
                HoaDon hd = hdRepo.findByMa(maHD);

                if (hd != null && "CHO_THANH_TOAN".equals(hd.getTrangThaiHoaDon().getMa())) {

                    if ("00".equals(vnp_ResponseCode)) {
                        TrangThaiHoaDon ttChoXacNhan = ttRepo.findByMa("CHO_XAC_NHAN");
                        hd.setTrangThaiHoaDon(ttChoXacNhan);
                        hd.setNgayThanhToan(new Date());
                        hdRepo.save(hd);

                        lsRepo.save(LichSuHoaDon.builder()
                                .hoaDon(hd).trangThaiHoaDon(ttChoXacNhan)
                                .ghiChu("Thanh toán VNPay thành công").build());

                        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                            ThongBao tb = new ThongBao();
                            tb.setTaiKhoanId(hd.getKhachHang().getTaiKhoan().getId());
                            tb.setTieuDe("Thanh toán thành công");
                            tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã được thanh toán qua VNPay.");
                            tb.setLoaiThongBao("ORDER");
                            tb.setDaDoc(false);
                            thongBaoRepository.save(tb);
                        }

                        redirectUrl = "http://localhost:8080/customer#account";

                    } else {
                        TrangThaiHoaDon ttHuy = ttRepo.findByMa("DA_HUY");
                        hd.setTrangThaiHoaDon(ttHuy);
                        hdRepo.save(hd);

                        List<HoaDonChiTiet> hdctList = hdctRepo.findByHoaDonId(hd.getId());
                        for (HoaDonChiTiet ct : hdctList) {
                            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
                            spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong());
                            spctRepo.save(spct);
                        }

                        if (hd.getMaGiamGia() != null) {
                            MaGiamGia voucher = hd.getMaGiamGia();
                            int luotMoi = voucher.getLuotSuDung() - 1;
                            if (luotMoi >= 0) {
                                voucher.setLuotSuDung(luotMoi);
                                if (!voucher.getTrangThai() && voucher.getNgayKetThuc().isAfter(java.time.LocalDateTime.now())) {
                                    voucher.setTrangThai(true);
                                }
                                maGiamGiaRepo.save(voucher);
                            }
                        }

                        lsRepo.save(LichSuHoaDon.builder()
                                .hoaDon(hd).trangThaiHoaDon(ttHuy)
                                .ghiChu("Hủy đơn do khách hàng không hoàn tất thanh toán VNPay").build());

                        redirectUrl = "http://localhost:8080/customer#checkout";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND).location(java.net.URI.create(redirectUrl)).build();
    }

    @GetMapping("/thong-bao-moi")
    public ResponseEntity<?> getNewOnlineOrders() {
        List<String> pendingStatuses = Arrays.asList("CHO_XAC_NHAN", "CHO_GIAO", "CHO_THANH_TOAN");

        List<HoaDon> list = hdRepo.findTop5ByLoaiHoaDonAndTrangThaiHoaDon_MaInOrderByNgayTaoDesc(1, pendingStatuses);
        List<Map<String, Object>> result = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (HoaDon hd : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", hd.getId());
            map.put("ma", hd.getMa());
            map.put("khachHang", hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "Khách hàng");
            map.put("tongTien", hd.getGiaTong());
            map.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : "");
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    public static class CheckoutRequest {
        public String tenNguoiNhan;
        public String soDienThoai;
        public String email;
        public String diaChiGiaoHang;
        public String ghiChu;
        public String phuongThucThanhToan;
        public BigDecimal tienHang;
        public BigDecimal phiShip;
        public BigDecimal tienGiam;
        public BigDecimal tongTien;
        public Integer voucherId;
        public List<CheckoutItemRequest> chiTietDonHangs;
    }

    public static class CheckoutItemRequest {
        public Integer chiTietSanPhamId;
        public Integer soLuong;
        public BigDecimal donGia;
    }

    @PostMapping("/{id}/confirm-transfer")
    @Transactional
    public ResponseEntity<?> confirmTransferPayment(@PathVariable Integer id) {
        try {
            HoaDon hd = hdRepo.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));

            if (!"CHO_THANH_TOAN".equals(hd.getTrangThaiHoaDon().getMa())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Đơn hàng không ở trạng thái chờ thanh toán"));
            }

            TrangThaiHoaDon ttChoXacNhan = ttRepo.findByMa("CHO_XAC_NHAN");
            if (ttChoXacNhan == null) throw new RuntimeException("Chưa cấu hình trạng thái CHO_XAC_NHAN trong DB");

            hd.setTrangThaiHoaDon(ttChoXacNhan);
            hd.setNgayThanhToan(new Date());
            hdRepo.save(hd);

            lsRepo.save(LichSuHoaDon.builder()
                    .hoaDon(hd)
                    .trangThaiHoaDon(ttChoXacNhan)
                    .ghiChu("Khách hàng xác nhận đã chuyển khoản qua QR code")
                    .ngayTao(new Date())
                    .build());

            return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái chờ xác nhận"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 🌟 API Lấy danh sách ID mã giảm giá khách hàng đã dùng
    @GetMapping("/my-used-vouchers")
    public ResponseEntity<?> getMyUsedVouchers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        Optional<KhachHang> khOpt = khRepo.findByTaiKhoan_TenDangNhap(auth.getName());
        if (khOpt.isEmpty()) return ResponseEntity.ok(Collections.emptyList());

        List<Integer> usedIds = hdRepo.findByKhachHangIdOrderByNgayTaoDesc(khOpt.get().getId()).stream()
                .filter(hd -> !"DA_HUY".equals(hd.getTrangThaiHoaDon().getMa()) && hd.getMaGiamGia() != null)
                .map(hd -> hd.getMaGiamGia().getId())
                .distinct()
                .collect(Collectors.toList());

        return ResponseEntity.ok(usedIds);
    }
}