package com.example.bee.controllers.api.order;

import com.example.bee.constants.PhuongThucThanhToan;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.notification.ThongBao;
import com.example.bee.entities.order.*;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.promotion.MaGiamGia;
import com.example.bee.entities.staff.NhanVien;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
import com.example.bee.repositories.order.*;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.promotion.MaGiamGiaRepository;
import com.example.bee.repositories.staff.NhanVienRepository;
import com.example.bee.services.EmailService;
import com.example.bee.services.GhnService;
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
public class HoaDonApi {

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

    private final ThanhToanRepository thanhToanRepo;

    private final com.example.bee.repositories.cart.GioHangRepository gioHangRepository;
    private final com.example.bee.repositories.cart.GioHangChiTietRepository gioHangChiTietRepository;

    private final GhnService ghnService;

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
        try {
            HoaDon hd = hdRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            List<HoaDonChiTiet> listHdct = hdctRepo.findByHoaDonId(id);

            Date ngayCapNhatCuoi = hd.getNgayThanhToan();
            BigDecimal tongTienHangThucTe = BigDecimal.ZERO;
            BigDecimal tongGiaGoc = BigDecimal.ZERO;

            List<Map<String, Object>> chiTietResponses = new ArrayList<>();

            for (HoaDonChiTiet ct : listHdct) {
                BigDecimal giaBan = ct.getGiaTien() != null ? ct.getGiaTien() : BigDecimal.ZERO;
                BigDecimal donGia = giaBan;
                if (ct.getSanPhamChiTiet() != null) {
                    donGia = ct.getSanPhamChiTiet().getGiaBan() != null ? ct.getSanPhamChiTiet().getGiaBan() : giaBan;
                }

                Integer soLuong = ct.getSoLuong() != null ? ct.getSoLuong() : 0;

                tongTienHangThucTe = tongTienHangThucTe.add(giaBan.multiply(BigDecimal.valueOf(soLuong)));
                tongGiaGoc = tongGiaGoc.add(donGia.multiply(BigDecimal.valueOf(soLuong)));

                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", ct.getId());
                if (ct.getSanPhamChiTiet() != null) {
                    itemMap.put("idSanPhamChiTiet", ct.getSanPhamChiTiet().getId());
                    itemMap.put("sku", ct.getSanPhamChiTiet().getSku());
                    itemMap.put("hinhAnh", ct.getSanPhamChiTiet().getHinhAnh());

                    if (ct.getSanPhamChiTiet().getSanPham() != null) {
                        itemMap.put("idSanPham", ct.getSanPhamChiTiet().getSanPham().getId());
                        itemMap.put("tenSanPham", ct.getSanPhamChiTiet().getSanPham().getTen());
                    }
                    if (ct.getSanPhamChiTiet().getKichThuoc() != null && ct.getSanPhamChiTiet().getMauSac() != null) {
                        itemMap.put("thuocTinh", ct.getSanPhamChiTiet().getMauSac().getTen() + " - " + ct.getSanPhamChiTiet().getKichThuoc().getTen());
                    }
                }
                itemMap.put("soLuong", soLuong);
                itemMap.put("donGia", donGia);
                itemMap.put("giaBan", giaBan);

                chiTietResponses.add(itemMap);
            }

            BigDecimal giaTamThoi = hd.getGiaTamThoi() != null ? hd.getGiaTamThoi() : tongTienHangThucTe;
            BigDecimal tienGiamGiaSanPham = tongGiaGoc.subtract(giaTamThoi);
            if (tienGiamGiaSanPham.compareTo(BigDecimal.ZERO) < 0) tienGiamGiaSanPham = BigDecimal.ZERO;

            // BÓC TÁCH CHIẾT KHẤU NHÂN VIÊN VÀ TIỀN GIẢM VOUCHER
            BigDecimal chietKhauNV = BigDecimal.ZERO;
            String rawGhiChu = hd.getGhiChu() != null ? hd.getGhiChu() : "";
            if (rawGhiChu.contains("[Đơn mua nội bộ bởi:")) {
                chietKhauNV = giaTamThoi.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
            }

            BigDecimal totalDiscount = hd.getGiaTriKhuyenMai() != null ? hd.getGiaTriKhuyenMai() : BigDecimal.ZERO;
            BigDecimal tienGiamVoucher = totalDiscount.subtract(chietKhauNV);
            if (tienGiamVoucher.compareTo(BigDecimal.ZERO) < 0) {
                tienGiamVoucher = BigDecimal.ZERO;
            }

            BigDecimal phiShip = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
            BigDecimal tongPhaiTra = hd.getGiaTong() != null ? hd.getGiaTong() : BigDecimal.ZERO;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");

            String tenNhanVienHienTai = "Hệ thống";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
                String username = authentication.getName();
                var nhanVienDangNhap = nvRepo.findByTaiKhoan_TenDangNhap(username).orElse(null);
                if (nhanVienDangNhap != null) tenNhanVienHienTai = nhanVienDangNhap.getHoTen();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", hd.getId());
            response.put("ma", hd.getMa());

            String tenNhan = "Khách hàng";
            if (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getTenNguoiNhan() != null) {
                tenNhan = hd.getThongTinGiaoHang().getTenNguoiNhan();
            } else if (hd.getKhachHang() != null) {
                tenNhan = hd.getKhachHang().getHoTen();
            }
            response.put("tenKhachHang", tenNhan);

            String sdtNhan = "";
            if (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getSdtNhan() != null) {
                sdtNhan = hd.getThongTinGiaoHang().getSdtNhan();
            } else if (hd.getKhachHang() != null) {
                sdtNhan = hd.getKhachHang().getSoDienThoai();
            }
            response.put("sdtKhachHang", sdtNhan);

            String emailNhan = "";
            if (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getEmailNhan() != null && !hd.getThongTinGiaoHang().getEmailNhan().isEmpty()) {
                emailNhan = hd.getThongTinGiaoHang().getEmailNhan();
            } else if (hd.getKhachHang() != null && hd.getKhachHang().getEmail() != null) {
                emailNhan = hd.getKhachHang().getEmail();
            }
            response.put("email", emailNhan);

            if (rawGhiChu.contains("[BUY_NOW]")) rawGhiChu = rawGhiChu.replace("[BUY_NOW]", "").trim();
            response.put("ghiChu", rawGhiChu);

            String diaChiGiaoHang = "";
            Integer maTinh = null;
            Integer maHuyen = null;
            String maXa = null;
            if (hd.getThongTinGiaoHang() != null) {
                if (hd.getThongTinGiaoHang().getDiaChiChiTiet() != null) {
                    diaChiGiaoHang = hd.getThongTinGiaoHang().getDiaChiChiTiet();
                }
                maTinh = hd.getThongTinGiaoHang().getMaTinh();
                maHuyen = hd.getThongTinGiaoHang().getMaHuyen();
                maXa = hd.getThongTinGiaoHang().getMaXa();
            }
            response.put("diaChiGiaoHang", diaChiGiaoHang);
            response.put("maTinh", maTinh);
            response.put("maHuyen", maHuyen);
            response.put("maXa", maXa);

            List<ThanhToan> ttList = thanhToanRepo.findByHoaDon_Id(hd.getId());

            String phuongThuc = (hd.getLoaiHoaDon() != null && hd.getLoaiHoaDon() == 0)
                    ? PhuongThucThanhToan.TIEN_MAT
                    : PhuongThucThanhToan.COD;

            if (ttList != null && !ttList.isEmpty() && ttList.get(0).getPhuongThuc() != null) {
                phuongThuc = ttList.get(0).getPhuongThuc();
            }
            response.put("phuongThucThanhToan", phuongThuc);

            response.put("trangThaiMa", hd.getTrangThaiHoaDon() != null ? hd.getTrangThaiHoaDon().getMa() : "");
            response.put("trangThaiTen", hd.getTrangThaiHoaDon() != null ? hd.getTrangThaiHoaDon().getTen() : "");
            response.put("loaiHoaDon", hd.getLoaiHoaDon());
            response.put("tenNhanVien", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : tenNhanVienHienTai);

            response.put("hinhThucGiaoHang", hd.getHinhThucGiaoHang());
            response.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : null);
            response.put("ngayThanhToan", hd.getNgayThanhToan() != null ? sdf.format(hd.getNgayThanhToan()) : null);
            response.put("ngayCapNhat", ngayCapNhatCuoi != null ? sdf.format(ngayCapNhatCuoi) : null);
            response.put("ngayNhanHangDuKien", hd.getNgayNhanHangDuKien() != null ? sdfDate.format(hd.getNgayNhanHangDuKien()) : null);
            response.put("ngayHenLayHang", hd.getNgayHenLayHang() != null ? sdf.format(hd.getNgayHenLayHang()) : null);
            response.put("ngayHangSanSang", hd.getNgayHangSanSang() != null ? sdf.format(hd.getNgayHangSanSang()) : null);

            String maVoucher = "Không áp dụng";
            try {
                if (hd.getMaGiamGia() != null) {
                    maVoucher = hd.getMaGiamGia().getMaCode() != null ? hd.getMaGiamGia().getMaCode() : hd.getMaGiamGia().getMaCode();
                }
            } catch (Exception ignored) {}
            response.put("maVoucher", maVoucher);

            List<Map<String, Object>> resThanhToan = new ArrayList<>();
            if (ttList != null) {
                for (ThanhToan tt : ttList) {
                    Map<String, Object> mapTT = new HashMap<>();
                    mapTT.put("soTien", tt.getSoTien() != null ? tt.getSoTien() : BigDecimal.ZERO);
                    mapTT.put("phuongThuc", tt.getPhuongThuc() != null ? tt.getPhuongThuc() : "Khác");
                    mapTT.put("loaiThanhToan", tt.getLoaiThanhToan() != null ? tt.getLoaiThanhToan() : "Chưa rõ");
                    mapTT.put("trangThai", tt.getTrangThai() != null ? tt.getTrangThai() : "Chờ xử lý");
                    mapTT.put("maGiaoDich", tt.getMaGiaoDich() != null ? tt.getMaGiaoDich() : "N/A");
                    mapTT.put("ngayThanhToan", tt.getNgayThanhToan() != null ? sdf.format(tt.getNgayThanhToan()) : null);
                    resThanhToan.add(mapTT);
                }
            }
            response.put("lichSuThanhToan", resThanhToan);

            List<LichSuHoaDon> listLichSu = lsRepo.findAll().stream()
                    .filter(ls -> ls.getHoaDon() != null && ls.getHoaDon().getId().equals(id))
                    .sorted((ls1, ls2) -> {
                        if (ls1.getNgayTao() == null && ls2.getNgayTao() == null) return 0;
                        if (ls1.getNgayTao() == null) return 1;
                        if (ls2.getNgayTao() == null) return -1;
                        return ls2.getNgayTao().compareTo(ls1.getNgayTao());
                    })
                    .collect(Collectors.toList());

            List<Map<String, Object>> resLichSu = new ArrayList<>();
            for (LichSuHoaDon ls : listLichSu) {
                Map<String, Object> mapLS = new HashMap<>();
                mapLS.put("trangThai", ls.getTrangThaiHoaDon() != null ? ls.getTrangThaiHoaDon().getTen() : "Cập nhật");
                mapLS.put("ghiChu", ls.getGhiChu() != null ? ls.getGhiChu() : "");
                mapLS.put("nguoiThaoTac", ls.getNhanVien() != null ? ls.getNhanVien().getHoTen() : "Hệ thống / Khách hàng");
                mapLS.put("ngayTao", ls.getNgayTao() != null ? sdf.format(ls.getNgayTao()) : null);
                resLichSu.add(mapLS);
            }
            response.put("lichSuThaoTac", resLichSu);

            response.put("tienHang", tongTienHangThucTe);
            response.put("phiVanChuyen", phiShip);
            response.put("tongTien", tongPhaiTra);
            response.put("chiTiets", chiTietResponses);

            response.put("giaTamThoi", giaTamThoi);
            response.put("tongGiaGoc", tongGiaGoc);
            response.put("tienGiamGiaSanPham", tienGiamGiaSanPham);
            response.put("giaTriKhuyenMai", totalDiscount);
            response.put("tienGiamVoucher", tienGiamVoucher);
            response.put("chietKhauNV", chietKhauNV);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi server: " + errorMsg));
        }
    }

    @PostMapping("/{id}/next-status")
    @Transactional
    public ResponseEntity<?> nextStatus(@PathVariable Integer id, @RequestBody Map<String, String> req) {
        HoaDon hd = hdRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));

        String currentMa = hd.getTrangThaiHoaDon().getMa();
        String hinhThucGiao = hd.getHinhThucGiaoHang() != null ? hd.getHinhThucGiaoHang() : "GIAO_TAN_NOI";
        String nextMa = "";
        String ghiChu = req.getOrDefault("ghiChu", "Cập nhật trạng thái tự động");
        String phuongThucMoi = req.get("phuongThucThanhToan"); // Lấy phương thức thanh toán mới do NV chọn

        switch (currentMa) {
            case "CHO_XAC_NHAN":
                nextMa = "DA_XAC_NHAN";
                break;
            case "DA_XAC_NHAN":
                if ("NHAN_TAI_CUA_HANG".equals(hinhThucGiao)) {
                    nextMa = "CHO_KHACH_LAY";
                } else {
                    nextMa = "CHO_GIAO_VAN_CHUYEN";
                }
                hd.setNgayHangSanSang(new Date());
                break;
            case "CHO_GIAO_VAN_CHUYEN":
                if ("NHAN_TAI_CUA_HANG".equals(hinhThucGiao)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn tại cửa hàng không qua bước này!");
                }
                nextMa = "DANG_GIAO_HANG";
                break;
            case "DANG_GIAO_HANG":
                if ("NHAN_TAI_CUA_HANG".equals(hinhThucGiao)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn tại cửa hàng không qua bước này!");
                }
                nextMa = "HOAN_THANH";
                if(hd.getNgayThanhToan() == null) hd.setNgayThanhToan(new Date());
                break;
            case "CHO_KHACH_LAY":
                if (!"NHAN_TAI_CUA_HANG".equals(hinhThucGiao)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn giao tận nơi không qua bước chờ khách lấy!");
                }
                nextMa = "HOAN_THANH";
                if(hd.getNgayThanhToan() == null) hd.setNgayThanhToan(new Date());
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái hiện tại không thể chuyển tiếp");
        }

        TrangThaiHoaDon nextStatus = ttRepo.findByMa(nextMa);
        if (nextStatus == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi Database: Không tìm thấy mã trạng thái " + nextMa);
        }

        hd.setTrangThaiHoaDon(nextStatus);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        NhanVien nhanVienThaoTac = null;
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            nhanVienThaoTac = nvRepo.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null);
        }

        if (hd.getNhanVien() == null && nhanVienThaoTac != null) {
            hd.setNhanVien(nhanVienThaoTac);
        }
        hdRepo.save(hd);

        // Cập nhật trạng thái thanh toán nếu hóa đơn hoàn thành (Dành cho COD hoặc nhận tại cửa hàng)
        if ("HOAN_THANH".equals(nextMa)) {
            List<ThanhToan> ttList = thanhToanRepo.findByHoaDon_Id(hd.getId());
            if (ttList != null && !ttList.isEmpty()) {
                ThanhToan tt = ttList.get(0);
                if ("CHO_THANH_TOAN".equals(tt.getTrangThai())) {
                    tt.setTrangThai("THANH_CONG");
                    tt.setNgayThanhToan(new Date());

                    // CẬP NHẬT PHƯƠNG THỨC THANH TOÁN MỚI (NẾU CÓ)
                    if (phuongThucMoi != null && !phuongThucMoi.trim().isEmpty()) {
                        tt.setPhuongThuc(phuongThucMoi);
                        ghiChu += " (Khách thanh toán bằng: " + phuongThucMoi + ")";
                    }

                    thanhToanRepo.save(tt);
                }
            }
        }

        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            ThongBao tb = new ThongBao();
            tb.setTaiKhoanId(hd.getKhachHang().getTaiKhoan().getId());
            tb.setLoaiThongBao("ORDER");
            tb.setDaDoc(false);

            switch (nextMa) {
                case "DA_XAC_NHAN":
                    tb.setTieuDe("Đơn hàng đã được xác nhận");
                    tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã được xác nhận và đang được đóng gói.");
                    thongBaoRepository.save(tb);
                    break;
                case "CHO_KHACH_LAY":
                    tb.setTieuDe("Đơn hàng đã sẵn sàng");
                    tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã được gói xong. Bạn có thể ghé cửa hàng lấy bất cứ lúc nào nhé!");
                    thongBaoRepository.save(tb);
                    break;
                case "CHO_GIAO_VAN_CHUYEN":
                    tb.setTieuDe("Đơn hàng đã sẵn sàng");
                    tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã đóng gói xong và đang chờ giao cho đơn vị vận chuyển.");
                    thongBaoRepository.save(tb);
                    break;
                case "DANG_GIAO_HANG":
                    tb.setTieuDe("Đơn hàng đang trên đường giao");
                    tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đang được vận chuyển đến bạn. Vui lòng chú ý điện thoại nhé!");
                    thongBaoRepository.save(tb);
                    break;
                case "HOAN_THANH":
                    tb.setTieuDe("Hoàn tất đơn hàng");
                    if ("NHAN_TAI_CUA_HANG".equals(hinhThucGiao)) {
                        tb.setNoiDung("Cảm ơn bạn đã ghé nhận đơn hàng #" + hd.getMa() + " tại cửa hàng. Hẹn gặp lại bạn nhé!");
                    } else {
                        tb.setNoiDung("Tuyệt vời! Đơn hàng #" + hd.getMa() + " đã được giao thành công. Cảm ơn bạn đã mua sắm tại Beemate.");
                    }
                    thongBaoRepository.save(tb);
                    break;
            }
        }

        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setHoaDon(hd);
        ls.setTrangThaiHoaDon(nextStatus);
        ls.setNhanVien(nhanVienThaoTac);
        ls.setGhiChu(ghiChu);
        ls.setNgayTao(new Date());
        lsRepo.save(ls);

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

            List<ThanhToan> tts = thanhToanRepo.findByHoaDon_Id(hd.getId());
            String phuongThuc = "TIEN_MAT";
            if (tts != null && !tts.isEmpty()) {
                phuongThuc = tts.get(0).getPhuongThuc();
            }

            if (!"CHUYEN_KHOAN".equalsIgnoreCase(phuongThuc)) {
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

        List<ThanhToan> ttList = thanhToanRepo.findByHoaDon_Id(hd.getId());
        boolean daThanhToanOnline = ttList.stream().anyMatch(tt ->
                ("VNPAY".equals(tt.getPhuongThuc()) || "MOMO".equals(tt.getPhuongThuc()))
                        && "THANH_CONG".equals(tt.getTrangThai())
        );

        if (daThanhToanOnline) {
            return ResponseEntity.badRequest().body(Map.of("message", "Đơn hàng đã được thanh toán trực tuyến. Vui lòng liên hệ Hotline CSKH để yêu cầu hủy và hoàn tiền!"));
        }

        if ("HOAN_THANH".equals(currentStatus) || "DANG_GIAO".equals(currentStatus) || "DANG_GIAO_HANG".equals(currentStatus) || "CHO_KHACH_LAY".equals(currentStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể hủy đơn hàng đã giao hoặc đang giao!"));
        }
        if (!"CHO_XAC_NHAN".equals(currentStatus) && !"CHO_THANH_TOAN".equals(currentStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể hủy đơn hàng đang xử lý hoặc đã hoàn thành!"));
        }
        if ("DA_HUY".equals(hd.getTrangThaiHoaDon().getMa())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Đơn hàng này đã bị hủy từ trước!"));
        }

        TrangThaiHoaDon ttHuy = ttRepo.findByMa("DA_HUY");
        hd.setTrangThaiHoaDon(ttHuy);

        List<HoaDonChiTiet> hdctList = hdctRepo.findByHoaDonId(hd.getId());
        for (HoaDonChiTiet ct : hdctList) {
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            if (spct != null) {
                spct.setSoLuong(spct.getSoLuong() + ct.getSoLuong());
                spctRepo.save(spct);
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
                maGiamGiaRepo.save(voucher);
            }
        }

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
            tb.setTieuDe("Đơn hàng đã bị hủy");
            tb.setNoiDung("Đơn hàng #" + hd.getMa() + " đã bị hủy. Lý do: " + req.getOrDefault("ghiChu", "Khách hàng / Admin yêu cầu hủy đơn."));
            thongBaoRepository.save(tb);
        }

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
            if (req.chiTietDonHangs == null || req.chiTietDonHangs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Giỏ hàng trống, không thể tạo đơn hàng!"));
            }
            HoaDon hd = new HoaDon();
            hd.setMa("HD" + System.currentTimeMillis());
            hd.setLoaiHoaDon(1);
            hd.setNgayTao(new Date());

            ThongTinGiaoHang thongTin = ThongTinGiaoHang.builder()
                    .tenNguoiNhan(req.tenNguoiNhan)
                    .sdtNhan(req.soDienThoai)
                    .emailNhan(req.email)
                    .diaChiChiTiet(req.diaChiGiaoHang)
                    .maTinh(req.maTinh)
                    .maHuyen(req.maHuyen)
                    .maXa(req.maXa)
                    .build();
            hd.setThongTinGiaoHang(thongTin);
            hd.setHinhThucGiaoHang(req.hinhThucGiaoHang != null ? req.hinhThucGiaoHang : "GIAO_TAN_NOI");

            if ("GIAO_TAN_NOI".equals(hd.getHinhThucGiaoHang()) && req.maHuyen != null && req.maXa != null) {
                Date expectedDate = ghnService.calculateExpectedDeliveryDate(req.maHuyen, req.maXa, null);
                if (expectedDate != null) {
                    hd.setNgayNhanHangDuKien(expectedDate);
                }
            } else if ("NHAN_TAI_CUA_HANG".equals(hd.getHinhThucGiaoHang()) && req.ngayHenLayHang != null) {
                hd.setNgayHenLayHang(req.ngayHenLayHang);
            }

            BigDecimal chietKhauNV = BigDecimal.ZERO;
            String ghiChu = req.ghiChu != null ? req.ghiChu : "";

            if (req.isBuyNow != null && req.isBuyNow) {
                ghiChu += " [BUY_NOW]";
            }

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

            BigDecimal tienHang = req.tienHang != null ? req.tienHang : BigDecimal.ZERO;
            BigDecimal phiShip = req.phiShip != null ? req.phiShip : BigDecimal.ZERO;
            BigDecimal tienGiam = req.tienGiam != null ? req.tienGiam : BigDecimal.ZERO;

            BigDecimal tongTienCuoi = req.tongTien != null ? req.tongTien : tienHang.subtract(tienGiam).add(phiShip);

            if (tongTienCuoi.compareTo(BigDecimal.ZERO) < 0) {
                tongTienCuoi = BigDecimal.ZERO;
            }

            hd.setGiaTamThoi(tienHang);
            hd.setGiaTriKhuyenMai(tienGiam.add(chietKhauNV));
            hd.setPhiVanChuyen(phiShip);
            hd.setGiaTong(tongTienCuoi);

            TrangThaiHoaDon ttBanDau;
            if ("MOMO".equalsIgnoreCase(req.phuongThucThanhToan) || "VNPAY".equalsIgnoreCase(req.phuongThucThanhToan) || "CHUYEN_KHOAN".equalsIgnoreCase(req.phuongThucThanhToan)) {
                ttBanDau = ttRepo.findByMa("CHO_THANH_TOAN");
            } else {
                ttBanDau = ttRepo.findByMa("CHO_XAC_NHAN");
            }

            if (ttBanDau == null) throw new RuntimeException("Lỗi cấu hình trạng thái trong DB");
            hd.setTrangThaiHoaDon(ttBanDau);

            if (req.voucherId != null) {
                MaGiamGia voucher = maGiamGiaRepo.findById(req.voucherId).orElse(null);
                if (voucher != null) {
                    if (!voucher.getTrangThai() ||
                            voucher.getLuotSuDung() >= voucher.getSoLuong() ||
                            (voucher.getNgayKetThuc() != null && voucher.getNgayKetThuc().isBefore(java.time.LocalDateTime.now()))) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Mã giảm giá đã hết lượt sử dụng hoặc hết hạn!"));
                    }

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

            HoaDon savedHd = hdRepo.save(hd);

            for (CheckoutItemRequest itemReq : req.chiTietDonHangs) {
                SanPhamChiTiet spct = spctRepo.findById(itemReq.chiTietSanPhamId)
                        .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

                if (spct.getSoLuong() < itemReq.soLuong) {
                    throw new RuntimeException("Sản phẩm " + spct.getSanPham().getTen() + " không đủ số lượng!");
                }

                BigDecimal giaThucTe = itemReq.donGia != null ? itemReq.donGia : spct.getGiaBan();

                spct.setSoLuong(spct.getSoLuong() - itemReq.soLuong);
                spctRepo.save(spct);

                HoaDonChiTiet hdct = new HoaDonChiTiet();
                hdct.setHoaDon(savedHd);
                hdct.setSanPhamChiTiet(spct);
                hdct.setSoLuong(itemReq.soLuong);
                hdct.setGiaTien(giaThucTe);
                hdctRepo.save(hdct);
            }

            ThanhToan tt = new ThanhToan();
            tt.setHoaDon(savedHd);
            tt.setSoTien(tongTienCuoi);
            String pttt = req.phuongThucThanhToan != null ? req.phuongThucThanhToan : "TIEN_MAT";
            tt.setPhuongThuc(pttt);
            tt.setLoaiThanhToan("THANH_TOAN");

            // Fix trạng thái thanh toán đối với COD
            if ("COD".equalsIgnoreCase(pttt)) {
                tt.setTrangThai("CHO_THANH_TOAN");
                tt.setNgayThanhToan(null);
            } else {
                tt.setTrangThai(ttBanDau.getMa().equals("CHO_THANH_TOAN") ? "CHO_THANH_TOAN" : "THANH_CONG");
                if ("THANH_CONG".equals(tt.getTrangThai())) {
                    tt.setNgayThanhToan(new Date());
                } else {
                    tt.setNgayThanhToan(null);
                }
            }
            thanhToanRepo.save(tt);

            // SỬA ĐOẠN NÀY LẠI ĐỂ LOG ĐÚNG LỊCH SỬ TẠO ĐƠN THEO LOẠI GIAO HÀNG
            String ghiChuTaoDon = "";
            if (ttBanDau.getMa().equals("CHO_THANH_TOAN")) {
                ghiChuTaoDon = "Đang chờ thanh toán online";
            } else {
                if ("NHAN_TAI_CUA_HANG".equals(hd.getHinhThucGiaoHang())) {
                    ghiChuTaoDon = "Khách hàng đặt đơn Online (Nhận và thanh toán tại cửa hàng)";
                } else {
                    ghiChuTaoDon = "Khách hàng đặt đơn Online (Thanh toán khi nhận hàng - COD)";
                }
            }

            LichSuHoaDon ls = new LichSuHoaDon();
            ls.setHoaDon(savedHd);
            ls.setTrangThaiHoaDon(ttBanDau);
            ls.setGhiChu(ghiChuTaoDon);
            ls.setNgayTao(new Date());
            lsRepo.save(ls);

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

            map.put("loaiHoaDon", hd.getLoaiHoaDon());
            map.put("hinhThucGiaoHang", hd.getHinhThucGiaoHang());
            map.put("phiVanChuyen", hd.getPhiVanChuyen());

            map.put("giaTamThoi", hd.getGiaTamThoi());
            map.put("giaTriKhuyenMai", hd.getGiaTriKhuyenMai());
            if (hd.getMaGiamGia() != null) {
                map.put("maVoucher", hd.getMaGiamGia().getMaCode());
            } else {
                map.put("maVoucher", "");
            }

            List<ThanhToan> ttList = thanhToanRepo.findByHoaDon_Id(hd.getId());
            String phuongThuc = "COD";
            if (ttList != null && !ttList.isEmpty() && ttList.get(0).getPhuongThuc() != null) {
                phuongThuc = ttList.get(0).getPhuongThuc();
            }
            map.put("phuongThucThanhToan", phuongThuc);

            List<HoaDonChiTiet> chiTiets = hdctRepo.findByHoaDonId(hd.getId());
            if (!chiTiets.isEmpty()) {
                HoaDonChiTiet firstItem = chiTiets.get(0);
                Map<String, Object> ctMap = new HashMap<>();
                if (firstItem.getSanPhamChiTiet() != null) {
                    ctMap.put("hinhAnh", firstItem.getSanPhamChiTiet().getHinhAnh());
                    if (firstItem.getSanPhamChiTiet().getSanPham() != null) {
                        ctMap.put("tenSanPham", firstItem.getSanPhamChiTiet().getSanPham().getTen());
                    }
                    if (firstItem.getSanPhamChiTiet().getKichThuoc() != null && firstItem.getSanPhamChiTiet().getMauSac() != null) {
                        ctMap.put("thuocTinh", firstItem.getSanPhamChiTiet().getMauSac().getTen() + " - " + firstItem.getSanPhamChiTiet().getKichThuoc().getTen());
                    }
                }
                ctMap.put("giaBan", firstItem.getGiaTien());
                map.put("chiTiets", Collections.singletonList(ctMap));
            }

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

        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd/MM/yyyy");

        Map<String, Object> result = new java.util.HashMap<>();

        result.put("id", hoaDon.getId());
        result.put("ma", hoaDon.getMa());

        result.put("loaiHoaDon", hoaDon.getLoaiHoaDon());
        result.put("hinhThucGiaoHang", hoaDon.getHinhThucGiaoHang());

        result.put("trangThaiMa", hoaDon.getTrangThaiHoaDon() != null ? hoaDon.getTrangThaiHoaDon().getMa() : "");
        result.put("trangThaiTen", hoaDon.getTrangThaiHoaDon() != null ? hoaDon.getTrangThaiHoaDon().getTen() : "");
        result.put("trangThaiHoaDon", hoaDon.getTrangThaiHoaDon());

        result.put("ngayTao", hoaDon.getNgayTao() != null ? sdf.format(hoaDon.getNgayTao()) : null);
        result.put("ngayThanhToan", hoaDon.getNgayThanhToan() != null ? sdf.format(hoaDon.getNgayThanhToan()) : null);
        result.put("ngayNhanHangDuKien", hoaDon.getNgayNhanHangDuKien() != null ? sdfDate.format(hoaDon.getNgayNhanHangDuKien()) : null);
        result.put("ngayHenLayHang", hoaDon.getNgayHenLayHang() != null ? sdf.format(hoaDon.getNgayHenLayHang()) : null);
        result.put("ngayHangSanSang", hoaDon.getNgayHangSanSang() != null ? sdf.format(hoaDon.getNgayHangSanSang()) : null);

        String tenNhan = "Khách hàng";
        if (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getTenNguoiNhan() != null) {
            tenNhan = hoaDon.getThongTinGiaoHang().getTenNguoiNhan();
        } else if (hoaDon.getKhachHang() != null) {
            tenNhan = hoaDon.getKhachHang().getHoTen();
        }
        result.put("tenNguoiNhan", tenNhan);
        result.put("tenKhachHang", tenNhan);

        String sdtNhan = "";
        if (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getSdtNhan() != null) {
            sdtNhan = hoaDon.getThongTinGiaoHang().getSdtNhan();
        } else if (hoaDon.getKhachHang() != null) {
            sdtNhan = hoaDon.getKhachHang().getSoDienThoai();
        }
        result.put("sdtKhachHang", sdtNhan);

        String emailNhan = "";
        if (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getEmailNhan() != null && !hoaDon.getThongTinGiaoHang().getEmailNhan().isEmpty()) {
            emailNhan = hoaDon.getThongTinGiaoHang().getEmailNhan();
        } else if (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getEmail() != null) {
            emailNhan = hoaDon.getKhachHang().getEmail();
        }
        result.put("email", emailNhan);

        String diaChiGiaoHang = "";
        Integer maTinh = null;
        Integer maHuyen = null;
        String maXa = null;
        if (hoaDon.getThongTinGiaoHang() != null) {
            if (hoaDon.getThongTinGiaoHang().getDiaChiChiTiet() != null) {
                diaChiGiaoHang = hoaDon.getThongTinGiaoHang().getDiaChiChiTiet();
            }
            maTinh = hoaDon.getThongTinGiaoHang().getMaTinh();
            maHuyen = hoaDon.getThongTinGiaoHang().getMaHuyen();
            maXa = hoaDon.getThongTinGiaoHang().getMaXa();
        }
        result.put("diaChiGiaoHang", diaChiGiaoHang);
        result.put("maTinh", maTinh);
        result.put("maHuyen", maHuyen);
        result.put("maXa", maXa);

        List<ThanhToan> ttListTC = thanhToanRepo.findByHoaDon_Id(hoaDon.getId());
        String ptttTC = "TIEN_MAT";
        if (ttListTC != null && !ttListTC.isEmpty()) {
            ptttTC = ttListTC.get(0).getPhuongThuc();
        }
        result.put("phuongThucThanhToan", ptttTC);

        List<Map<String, Object>> resThanhToan = new ArrayList<>();
        if (ttListTC != null) {
            for (ThanhToan tt : ttListTC) {
                Map<String, Object> mapTT = new HashMap<>();
                mapTT.put("soTien", tt.getSoTien() != null ? tt.getSoTien() : BigDecimal.ZERO);
                mapTT.put("phuongThuc", tt.getPhuongThuc() != null ? tt.getPhuongThuc() : "Khác");
                mapTT.put("loaiThanhToan", tt.getLoaiThanhToan() != null ? tt.getLoaiThanhToan() : "Chưa rõ");
                mapTT.put("trangThai", tt.getTrangThai() != null ? tt.getTrangThai() : "Chờ xử lý");
                mapTT.put("maGiaoDich", tt.getMaGiaoDich() != null ? tt.getMaGiaoDich() : "N/A");
                mapTT.put("ngayThanhToan", tt.getNgayThanhToan() != null ? sdf.format(tt.getNgayThanhToan()) : null);
                resThanhToan.add(mapTT);
            }
        }
        result.put("lichSuThanhToan", resThanhToan);

        String rawGhiChu = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : "";
        if (rawGhiChu.contains("[BUY_NOW]")) {
            rawGhiChu = rawGhiChu.replace("[BUY_NOW]", "").trim();
        }
        result.put("ghiChu", rawGhiChu);

        result.put("giaTong", hoaDon.getGiaTong());
        result.put("tongTien", hoaDon.getGiaTong());
        result.put("phiVanChuyen", hoaDon.getPhiVanChuyen());

        java.util.List<com.example.bee.entities.order.HoaDonChiTiet> danhSachChiTiet = hdctRepo.findByHoaDonId(hoaDon.getId());
        BigDecimal tongGiaGocTraCuu = BigDecimal.ZERO;
        BigDecimal giaTamThoiTraCuu = hoaDon.getGiaTamThoi() != null ? hoaDon.getGiaTamThoi() : BigDecimal.ZERO;

        java.util.List<java.util.Map<String, Object>> listChiTiet = new java.util.ArrayList<>();
        if (danhSachChiTiet != null && !danhSachChiTiet.isEmpty()) {
            for (com.example.bee.entities.order.HoaDonChiTiet hdct : danhSachChiTiet) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();

                item.put("id", hdct.getId());
                item.put("soLuong", hdct.getSoLuong());

                BigDecimal giaBan = hdct.getGiaTien() != null ? hdct.getGiaTien() : BigDecimal.ZERO;
                BigDecimal donGia = giaBan;
                if (hdct.getSanPhamChiTiet() != null && hdct.getSanPhamChiTiet().getGiaBan() != null) {
                    donGia = hdct.getSanPhamChiTiet().getGiaBan();
                }
                item.put("donGia", donGia);
                item.put("giaBan", giaBan);

                tongGiaGocTraCuu = tongGiaGocTraCuu.add(donGia.multiply(BigDecimal.valueOf(hdct.getSoLuong())));

                if (hdct.getSanPhamChiTiet() != null) {
                    item.put("sku", hdct.getSanPhamChiTiet().getSku());
                    item.put("idSanPhamChiTiet", hdct.getSanPhamChiTiet().getId());

                    item.put("hinhAnh", hdct.getSanPhamChiTiet().getHinhAnh());
                    if (hdct.getSanPhamChiTiet().getSanPham() != null) {
                        item.put("idSanPham", hdct.getSanPhamChiTiet().getSanPham().getId());
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

        BigDecimal tienGiamSanPhamTraCuu = tongGiaGocTraCuu.subtract(giaTamThoiTraCuu);
        if(tienGiamSanPhamTraCuu.compareTo(BigDecimal.ZERO) < 0) tienGiamSanPhamTraCuu = BigDecimal.ZERO;

        BigDecimal chietKhauNV = BigDecimal.ZERO;
        if (rawGhiChu.contains("[Đơn mua nội bộ bởi:")) {
            chietKhauNV = giaTamThoiTraCuu.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
        }

        BigDecimal totalDiscount = hoaDon.getGiaTriKhuyenMai() != null ? hoaDon.getGiaTriKhuyenMai() : BigDecimal.ZERO;
        BigDecimal tienGiamVoucher = totalDiscount.subtract(chietKhauNV);
        if (tienGiamVoucher.compareTo(BigDecimal.ZERO) < 0) {
            tienGiamVoucher = BigDecimal.ZERO;
        }

        result.put("giaTamThoi", giaTamThoiTraCuu);
        result.put("tongGiaGoc", tongGiaGocTraCuu);
        result.put("tienGiamGiaSanPham", tienGiamSanPhamTraCuu);
        result.put("giaTriKhuyenMai", totalDiscount);
        result.put("tienGiamVoucher", tienGiamVoucher);
        result.put("chietKhauNV", chietKhauNV);

        if (hoaDon.getMaGiamGia() != null) {
            result.put("maVoucher", hoaDon.getMaGiamGia().getMaCode());
        } else {
            result.put("maVoucher", "Không áp dụng");
        }

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

        String tenNhan = "Khách vãng lai";
        String sdtNhan = "";
        if (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getTenNguoiNhan() != null) {
            tenNhan = hd.getThongTinGiaoHang().getTenNguoiNhan();
            sdtNhan = hd.getThongTinGiaoHang().getSdtNhan() != null ? hd.getThongTinGiaoHang().getSdtNhan() : "";
        } else if (hd.getKhachHang() != null) {
            tenNhan = hd.getKhachHang().getHoTen();
            sdtNhan = hd.getKhachHang().getSoDienThoai();
        }

        if (hd.getKhachHang() != null) {
            orderInfo.put("tenKhachHang", tenNhan);
            orderInfo.put("sdtKhachHang", sdtNhan);
            orderInfo.put("inThongTinTaiKhoan", true);
        } else {
            orderInfo.put("tenKhachHang", tenNhan);
            orderInfo.put("sdtKhachHang", sdtNhan);
            orderInfo.put("inThongTinTaiKhoan", false);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal tongTienHang = BigDecimal.ZERO;
        BigDecimal tongGiaGoc = BigDecimal.ZERO;

        for (HoaDonChiTiet ct : listHdct) {
            Map<String, Object> item = new HashMap<>();
            String tenSP = ct.getSanPhamChiTiet().getSanPham().getTen();
            String thuocTinh = ct.getSanPhamChiTiet().getMauSac().getTen() + " - " + ct.getSanPhamChiTiet().getKichThuoc().getTen();
            item.put("ten", tenSP + " (" + thuocTinh + ")");
            item.put("soLuong", ct.getSoLuong());

            BigDecimal giaBan = ct.getGiaTien() != null ? ct.getGiaTien() : BigDecimal.ZERO;
            BigDecimal donGia = giaBan;
            if (ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getGiaBan() != null) {
                donGia = ct.getSanPhamChiTiet().getGiaBan();
            }

            item.put("donGia", giaBan);
            item.put("giaBan", giaBan);

            BigDecimal thanhTien = giaBan.multiply(BigDecimal.valueOf(ct.getSoLuong()));
            item.put("thanhTien", thanhTien);

            tongTienHang = tongTienHang.add(thanhTien);
            tongGiaGoc = tongGiaGoc.add(donGia.multiply(BigDecimal.valueOf(ct.getSoLuong())));
            items.add(item);
        }
        Map<String, Object> summary = new HashMap<>();

        BigDecimal tienGiamSanPham = tongGiaGoc.subtract(tongTienHang);
        if(tienGiamSanPham.compareTo(BigDecimal.ZERO) < 0) tienGiamSanPham = BigDecimal.ZERO;

        summary.put("tongTienHang", tongTienHang);
        summary.put("tongGiaGoc", tongGiaGoc);
        summary.put("tienGiamSanPham", tienGiamSanPham);

        BigDecimal phiShip = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        summary.put("phiVanChuyen", phiShip);
        BigDecimal tongPhaiTra = hd.getGiaTong() != null ? hd.getGiaTong() : BigDecimal.ZERO;
        BigDecimal giamGia = tongTienHang.add(phiShip).subtract(tongPhaiTra);
        if (giamGia.compareTo(BigDecimal.ZERO) < 0) giamGia = BigDecimal.ZERO;
        summary.put("giamGia", giamGia);
        summary.put("tongThanhToan", tongPhaiTra);

        BigDecimal chietKhauNV = BigDecimal.ZERO;
        String rawGhiChu = hd.getGhiChu() != null ? hd.getGhiChu() : "";
        if (rawGhiChu.contains("[Đơn mua nội bộ bởi:")) {
            chietKhauNV = tongTienHang.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
        }
        BigDecimal tienGiamVoucher = giamGia.subtract(chietKhauNV);
        if (tienGiamVoucher.compareTo(BigDecimal.ZERO) < 0) {
            tienGiamVoucher = BigDecimal.ZERO;
        }

        summary.put("tienGiamVoucher", tienGiamVoucher);
        summary.put("chietKhauNV", chietKhauNV);

        List<ThanhToan> ttListPrint = thanhToanRepo.findByHoaDon_Id(hd.getId());
        String ptttPrint = "TIEN_MAT";
        if (ttListPrint != null && !ttListPrint.isEmpty()) {
            ptttPrint = ttListPrint.get(0).getPhuongThuc();
        }
        summary.put("phuongThuc", ptttPrint);

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

            List<ThanhToan> ttMomoList = thanhToanRepo.findByHoaDon_Id(hd.getId());
            if (ttMomoList != null && !ttMomoList.isEmpty()) {
                ThanhToan ttMomo = ttMomoList.get(0);
                ttMomo.setPhuongThuc("MOMO");
                thanhToanRepo.save(ttMomo);
            } else {
                ThanhToan ttMomo = new ThanhToan();
                ttMomo.setHoaDon(hd);
                ttMomo.setSoTien(totalAmount);
                ttMomo.setPhuongThuc("MOMO");
                ttMomo.setLoaiThanhToan("THANH_TOAN");
                ttMomo.setTrangThai("CHO_THANH_TOAN");
                ttMomo.setNgayThanhToan(new Date());
                thanhToanRepo.save(ttMomo);
            }

            String rId = String.valueOf(System.currentTimeMillis());
            String oId = hd.getMa() + "_" + rId;
            String amountStr = totalAmount.setScale(0, RoundingMode.HALF_UP).toString();

            String onlineReturnUrl = "https://beemate.store/api/hoa-don/momo-callback";

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

        String redirectUrl = "https://beemate.store/customer#home";

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

                        List<ThanhToan> ttMomoCb = thanhToanRepo.findByHoaDon_Id(hd.getId());
                        if (ttMomoCb != null && !ttMomoCb.isEmpty()) {
                            ThanhToan tt = ttMomoCb.get(0);
                            tt.setTrangThai("THANH_CONG");
                            tt.setMaGiaoDich(orderId);
                            tt.setNgayThanhToan(new Date());
                            thanhToanRepo.save(tt);
                        }

                        boolean isBuyNow = hd.getGhiChu() != null && hd.getGhiChu().contains("[BUY_NOW]");
                        if (!isBuyNow && hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                            com.example.bee.entities.cart.GioHang gh = gioHangRepository.findByTaiKhoan_Id(hd.getKhachHang().getTaiKhoan().getId()).orElse(null);
                            if (gh != null) {
                                List<com.example.bee.entities.cart.GioHangChiTiet> ghcts = gioHangChiTietRepository.findByGioHang_Id(gh.getId());
                                gioHangChiTietRepository.deleteAll(ghcts);
                            }
                        }

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

                        redirectUrl = "https://beemate.store/customer#account";

                    } else {
                        TrangThaiHoaDon ttHuy = ttRepo.findByMa("DA_HUY");
                        hd.setTrangThaiHoaDon(ttHuy);
                        hdRepo.save(hd);

                        List<ThanhToan> ttMomoCbFail = thanhToanRepo.findByHoaDon_Id(hd.getId());
                        if (ttMomoCbFail != null && !ttMomoCbFail.isEmpty()) {
                            ThanhToan tt = ttMomoCbFail.get(0);
                            tt.setTrangThai("THAT_BAI");
                            thanhToanRepo.save(tt);
                        }

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

                        redirectUrl = "https://beemate.store/customer#checkout";
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

            List<ThanhToan> ttVnpayList = thanhToanRepo.findByHoaDon_Id(hd.getId());
            if (ttVnpayList != null && !ttVnpayList.isEmpty()) {
                ThanhToan ttVnpay = ttVnpayList.get(0);
                ttVnpay.setPhuongThuc("VNPAY");
                thanhToanRepo.save(ttVnpay);
            } else {
                ThanhToan ttVnpay = new ThanhToan();
                ttVnpay.setHoaDon(hd);
                ttVnpay.setSoTien(totalAmount);
                ttVnpay.setPhuongThuc("VNPAY");
                ttVnpay.setLoaiThanhToan("THANH_TOAN");
                ttVnpay.setTrangThai("CHO_THANH_TOAN");
                ttVnpay.setNgayThanhToan(new Date());
                thanhToanRepo.save(ttVnpay);
            }

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

            String onlineReturnUrl = "https://beemate.store/api/hoa-don/vnpay-callback";
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

        String redirectUrl = "https://beemate.store/customer#home";

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

                        List<ThanhToan> ttVnCb = thanhToanRepo.findByHoaDon_Id(hd.getId());
                        if (ttVnCb != null && !ttVnCb.isEmpty()) {
                            ThanhToan tt = ttVnCb.get(0);
                            tt.setTrangThai("THANH_CONG");
                            tt.setMaGiaoDich(vnp_TxnRef);
                            tt.setNgayThanhToan(new Date());
                            thanhToanRepo.save(tt);
                        }

                        boolean isBuyNow = hd.getGhiChu() != null && hd.getGhiChu().contains("[BUY_NOW]");
                        if (!isBuyNow && hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                            com.example.bee.entities.cart.GioHang gh = gioHangRepository.findByTaiKhoan_Id(hd.getKhachHang().getTaiKhoan().getId()).orElse(null);
                            if (gh != null) {
                                List<com.example.bee.entities.cart.GioHangChiTiet> ghcts = gioHangChiTietRepository.findByGioHang_Id(gh.getId());
                                gioHangChiTietRepository.deleteAll(ghcts);
                            }
                        }

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

                        redirectUrl = "https://beemate.store/customer#account";

                    } else {
                        TrangThaiHoaDon ttHuy = ttRepo.findByMa("DA_HUY");
                        hd.setTrangThaiHoaDon(ttHuy);
                        hdRepo.save(hd);

                        List<ThanhToan> ttVnCbFail = thanhToanRepo.findByHoaDon_Id(hd.getId());
                        if (ttVnCbFail != null && !ttVnCbFail.isEmpty()) {
                            ThanhToan tt = ttVnCbFail.get(0);
                            tt.setTrangThai("THAT_BAI");
                            thanhToanRepo.save(tt);
                        }

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

                        redirectUrl = "https://beemate.store/customer#checkout";
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

            String tenNhan = "Khách hàng";
            if (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getTenNguoiNhan() != null) {
                tenNhan = hd.getThongTinGiaoHang().getTenNguoiNhan();
            } else if (hd.getKhachHang() != null) {
                tenNhan = hd.getKhachHang().getHoTen();
            }
            map.put("khachHang", tenNhan);

            map.put("tongTien", hd.getGiaTong());
            map.put("ngayTao", hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : "");
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/confirm-transfer")
    @Transactional
    public ResponseEntity<?> confirmTransferPayment(@PathVariable Integer id) {
        try {
            HoaDon hd = hdRepo.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));

            TrangThaiHoaDon ttChoXacNhan = ttRepo.findByMa("CHO_XAC_NHAN");
            if (ttChoXacNhan == null) throw new RuntimeException("Chưa cấu hình trạng thái CHO_XAC_NHAN trong DB");

            hd.setTrangThaiHoaDon(ttChoXacNhan);
            hd.setNgayThanhToan(new Date());
            hdRepo.save(hd);

            boolean isBuyNow = hd.getGhiChu() != null && hd.getGhiChu().contains("[BUY_NOW]");
            if (!isBuyNow && hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                com.example.bee.entities.cart.GioHang gh = gioHangRepository.findByTaiKhoan_Id(hd.getKhachHang().getTaiKhoan().getId()).orElse(null);
                if (gh != null) {
                    List<com.example.bee.entities.cart.GioHangChiTiet> ghcts = gioHangChiTietRepository.findByGioHang_Id(gh.getId());
                    gioHangChiTietRepository.deleteAll(ghcts);
                }
            }

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

    public static class CheckoutRequest {
        public String tenNguoiNhan;
        public String soDienThoai;
        public String email;
        public String diaChiGiaoHang;

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

    public static class CheckoutItemRequest {
        public Integer chiTietSanPhamId;
        public Integer soLuong;
        public BigDecimal donGia;
    }
}