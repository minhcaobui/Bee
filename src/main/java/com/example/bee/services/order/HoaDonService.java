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
public class HoaDonService {

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

    public Page<HoaDon> timKiemDonHangChoXuLy(String tuKhoa, Integer idTrangThai, Integer loaiHoaDon, String phuongThucThanhToan, Date tuNgay, Date denNgay, Pageable pageable) {
        return hoaDonRepository.searchDonHangChoXuLy(tuKhoa, idTrangThai, loaiHoaDon, phuongThucThanhToan, tuNgay, denNgay, pageable);
    }

    public Page<HoaDon> timKiemLichSuHoaDon(String tuKhoa, Integer idTrangThai, Integer idNhanVien, Integer loaiHoaDon, String phuongThucThanhToan, Date tuNgay, Date denNgay, Pageable pageable) {
        return hoaDonRepository.searchLichSuHoaDon(tuKhoa, idTrangThai, idNhanVien, loaiHoaDon, phuongThucThanhToan, tuNgay, denNgay, pageable);
    }

    public Map<String, Object> layChiTietHoaDon(Integer idHoaDon) {
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        List<HoaDonChiTiet> danhSachChiTiet = hoaDonChiTietRepository.findByHoaDonId(idHoaDon);

        Map<Integer, Integer> sanPhamTraLai = new HashMap<>();
        Date ngayCapNhatCuoi = hoaDon.getNgayThanhToan();
        BigDecimal tongTienHoan = BigDecimal.ZERO;

        List<YeuCauDoiTra> danhSachDoiTra = yeuCauDoiTraRepository.findAll().stream().filter(y -> y.getHoaDon().getId().equals(idHoaDon) && "HOAN_THANH".equals(y.getTrangThai())).collect(Collectors.toList());
        for (YeuCauDoiTra yeuCau : danhSachDoiTra) {
            if (ngayCapNhatCuoi == null || (yeuCau.getNgayXuLy() != null && yeuCau.getNgayXuLy().after(ngayCapNhatCuoi))) {
                ngayCapNhatCuoi = yeuCau.getNgayXuLy();
            }
            if (yeuCau.getSoTienHoan() != null) {
                tongTienHoan = tongTienHoan.add(yeuCau.getSoTienHoan());
            }
            List<ChiTietDoiTra> chiTietDoiTras = chiTietDoiTraRepository.findByYeuCauDoiTraId(yeuCau.getId());
            for (ChiTietDoiTra chiTietDoiTra : chiTietDoiTras) {
                Integer idHdct = chiTietDoiTra.getHoaDonChiTiet().getId();
                sanPhamTraLai.put(idHdct, sanPhamTraLai.getOrDefault(idHdct, 0) + chiTietDoiTra.getSoLuong());
            }
        }

        BigDecimal tongTienHangThucTe = BigDecimal.ZERO;
        List<Map<String, Object>> phanHoiChiTiet = new ArrayList<>();

        for (HoaDonChiTiet chiTiet : danhSachChiTiet) {
            BigDecimal giaBan = chiTiet.getGiaTien();
            Integer soLuong = chiTiet.getSoLuong();
            Integer soLuongTra = sanPhamTraLai.getOrDefault(chiTiet.getId(), 0);
            tongTienHangThucTe = tongTienHangThucTe.add(giaBan.multiply(BigDecimal.valueOf(soLuong)));

            Map<String, Object> thongTinChiTiet = new HashMap<>();
            thongTinChiTiet.put("id", chiTiet.getId());
            thongTinChiTiet.put("idSanPhamChiTiet", chiTiet.getSanPhamChiTiet().getId());
            thongTinChiTiet.put("idSanPham", chiTiet.getSanPhamChiTiet().getSanPham().getId());
            thongTinChiTiet.put("tenSanPham", chiTiet.getSanPhamChiTiet().getSanPham().getTen());
            thongTinChiTiet.put("sku", chiTiet.getSanPhamChiTiet().getSku());
            thongTinChiTiet.put("thuocTinh", chiTiet.getSanPhamChiTiet().getKichThuoc().getTen() + " - " + chiTiet.getSanPhamChiTiet().getMauSac().getTen());
            thongTinChiTiet.put("hinhAnh", chiTiet.getSanPhamChiTiet().getHinhAnh());
            thongTinChiTiet.put("soLuong", soLuong);
            thongTinChiTiet.put("donGia", chiTiet.getSanPhamChiTiet().getGiaBan());
            thongTinChiTiet.put("giaBan", giaBan);
            thongTinChiTiet.put("soLuongTra", soLuongTra);
            phanHoiChiTiet.add(thongTinChiTiet);
        }

        BigDecimal phiVanChuyen = hoaDon.getPhiVanChuyen() != null ? hoaDon.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal tongPhaiTra = hoaDon.getGiaTong() != null ? hoaDon.getGiaTong() : BigDecimal.ZERO;
        BigDecimal soTienGiamGia = tongTienHangThucTe.add(phiVanChuyen).subtract(tongPhaiTra);
        if (soTienGiamGia.compareTo(BigDecimal.ZERO) < 0) soTienGiamGia = BigDecimal.ZERO;

        SimpleDateFormat dinhDangNgay = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String tenNhanVienHienTai = "Hệ thống";
        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        if (xacThuc != null && xacThuc.isAuthenticated() && !xacThuc.getPrincipal().equals("anonymousUser")) {
            String tenDangNhap = xacThuc.getName();
            var nhanVienDangNhap = nhanVienRepository.findByTaiKhoan_TenDangNhap(tenDangNhap).orElse(null);
            if (nhanVienDangNhap != null) {
                tenNhanVienHienTai = nhanVienDangNhap.getHoTen();
            }
        }

        Map<String, Object> phanHoi = new HashMap<>();
        phanHoi.put("id", hoaDon.getId());
        phanHoi.put("ma", hoaDon.getMa());

        String tenNguoiNhan = (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getTenNguoiNhan() != null)
                ? hoaDon.getThongTinGiaoHang().getTenNguoiNhan()
                : (hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getHoTen() : "Khách vãng lai");

        String sdtNguoiNhan = (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getSdtNhan() != null)
                ? hoaDon.getThongTinGiaoHang().getSdtNhan()
                : (hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getSoDienThoai() : "");

        String diaChiNhan = (hoaDon.getThongTinGiaoHang() != null) ? hoaDon.getThongTinGiaoHang().getDiaChiChiTiet() : "";

        phanHoi.put("tenKhachHang", tenNguoiNhan);
        phanHoi.put("sdtKhachHang", sdtNguoiNhan);
        phanHoi.put("email", hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getEmail() : "");
        phanHoi.put("diaChiGiaoHang", diaChiNhan);

        String ghiChuGoc = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : "";
        if (ghiChuGoc.contains("[BUY_NOW]")) {
            ghiChuGoc = ghiChuGoc.replace("[BUY_NOW]", "").trim();
        }
        phanHoi.put("ghiChu", ghiChuGoc);
        phanHoi.put("ngayThanhToan", hoaDon.getNgayThanhToan() != null ? dinhDangNgay.format(hoaDon.getNgayThanhToan()) : null);
        phanHoi.put("ngayCapNhat", ngayCapNhatCuoi != null ? dinhDangNgay.format(ngayCapNhatCuoi) : null);
        phanHoi.put("tenNhanVien", hoaDon.getNhanVien() != null ? hoaDon.getNhanVien().getHoTen() : tenNhanVienHienTai);

        List<ThanhToan> danhSachThanhToan = thanhToanRepository.findByHoaDon_Id(hoaDon.getId());
        String phuongThucThanhToan = "TIEN_MAT";
        if (danhSachThanhToan != null && !danhSachThanhToan.isEmpty()) {
            phuongThucThanhToan = danhSachThanhToan.get(0).getPhuongThuc();
        }
        phanHoi.put("phuongThucThanhToan", phuongThucThanhToan);
        phanHoi.put("trangThaiMa", hoaDon.getTrangThaiHoaDon().getMa());
        phanHoi.put("trangThaiTen", hoaDon.getTrangThaiHoaDon().getTen());
        phanHoi.put("loaiHoaDon", hoaDon.getLoaiHoaDon());
        phanHoi.put("ngayTao", hoaDon.getNgayTao() != null ? dinhDangNgay.format(hoaDon.getNgayTao()) : null);
        phanHoi.put("tienHang", tongTienHangThucTe);
        phanHoi.put("phiVanChuyen", phiVanChuyen);
        phanHoi.put("tienGiamVoucher", soTienGiamGia);
        phanHoi.put("tongTien", tongPhaiTra);
        phanHoi.put("tienHoan", tongTienHoan);
        phanHoi.put("chiTiets", phanHoiChiTiet);

        return phanHoi;
    }

    @Transactional
    public Map<String, Object> chuyenTrangThaiTiepTheo(Integer idHoaDon, Map<String, String> yeuCau) {
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        String maTrangThaiHienTai = hoaDon.getTrangThaiHoaDon().getMa();
        String maTrangThaiTiepTheo;
        String ghiChu = yeuCau.getOrDefault("ghiChu", "Cập nhật trạng thái tự động");
        switch (maTrangThaiHienTai) {
            case "CHO_XAC_NHAN" -> maTrangThaiTiepTheo = "CHO_GIAO";
            case "CHO_GIAO" -> maTrangThaiTiepTheo = "DANG_GIAO";
            case "DANG_GIAO" -> {
                maTrangThaiTiepTheo = "HOAN_THANH";
                hoaDon.setNgayThanhToan(new Date());
            }
            default -> throw new IllegalArgumentException("Trạng thái hiện tại không thể chuyển tiếp");
        }
        TrangThaiHoaDon trangThaiTiepTheo = trangThaiHoaDonRepository.findByMa(maTrangThaiTiepTheo);
        hoaDon.setTrangThaiHoaDon(trangThaiTiepTheo);

        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (xacThuc != null && xacThuc.isAuthenticated() && !xacThuc.getPrincipal().equals("anonymousUser"))
                ? nhanVienRepository.findByTaiKhoan_TenDangNhap(xacThuc.getName()).orElse(null) : null;

        if (hoaDon.getNhanVien() == null && nhanVienThaoTac != null) {
            hoaDon.setNhanVien(nhanVienThaoTac);
        }
        hoaDonRepository.save(hoaDon);

        if (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getTaiKhoan() != null) {
            ThongBao thongBao = new ThongBao();
            thongBao.setTaiKhoanId(hoaDon.getKhachHang().getTaiKhoan().getId());
            thongBao.setLoaiThongBao("ORDER");
            thongBao.setDaDoc(false);
            if ("CHO_GIAO".equals(maTrangThaiTiepTheo)) {
                thongBao.setTieuDe("Đơn hàng đã được xác nhận");
                thongBao.setNoiDung("Đơn hàng #" + hoaDon.getMa() + " đã đóng gói xong và đang chờ giao cho đơn vị vận chuyển.");
            } else if ("DANG_GIAO".equals(maTrangThaiTiepTheo)) {
                thongBao.setTieuDe("Đơn hàng đang trên đường giao");
                thongBao.setNoiDung("Đơn hàng #" + hoaDon.getMa() + " đang được vận chuyển đến bạn. Vui lòng chú ý điện thoại nhé!");
            } else if ("HOAN_THANH".equals(maTrangThaiTiepTheo)) {
                thongBao.setTieuDe("Giao hàng thành công");
                thongBao.setNoiDung("Tuyệt vời! Đơn hàng #" + hoaDon.getMa() + " đã được giao thành công. Cảm ơn bạn đã mua sắm tại Beemate.");
            }
            thongBaoRepository.save(thongBao);
        }
        lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hoaDon).trangThaiHoaDon(trangThaiTiepTheo).nhanVien(nhanVienThaoTac).ghiChu(ghiChu).build());

        return Map.of("message", "Cập nhật thành công", "nextStatus", trangThaiTiepTheo.getTen(), "nextStatusMa", trangThaiTiepTheo.getMa());
    }

    @Transactional
    public Map<String, Object> yeuCauThanhToanLai(Integer idHoaDon) {
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        List<ThanhToan> danhSachThanhToan = thanhToanRepository.findByHoaDon_Id(hoaDon.getId());
        String phuongThuc = "TIEN_MAT";
        if (danhSachThanhToan != null && !danhSachThanhToan.isEmpty()) {
            phuongThuc = danhSachThanhToan.get(0).getPhuongThuc();
        }
        if (!"CHUYEN_KHOAN".equalsIgnoreCase(phuongThuc)) {
            throw new IllegalArgumentException("Chỉ áp dụng cho đơn Chuyển khoản");
        }
        TrangThaiHoaDon trangThaiChoThanhToan = trangThaiHoaDonRepository.findByMa("CHO_THANH_TOAN");
        hoaDon.setTrangThaiHoaDon(trangThaiChoThanhToan);
        hoaDonRepository.save(hoaDon);

        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (xacThuc != null && xacThuc.isAuthenticated() && !xacThuc.getPrincipal().equals("anonymousUser"))
                ? nhanVienRepository.findByTaiKhoan_TenDangNhap(xacThuc.getName()).orElse(null) : null;

        lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hoaDon).trangThaiHoaDon(trangThaiChoThanhToan).nhanVien(nhanVienThaoTac).ghiChu("Nhân viên xác nhận chưa nhận được tiền, chuyển về Chờ thanh toán").build());
        return Map.of("message", "Đã chuyển về Chờ thanh toán");
    }

    @Transactional
    public Map<String, Object> huyDonHang(Integer idHoaDon, Map<String, String> yeuCau) {
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        String trangThaiHienTai = hoaDon.getTrangThaiHoaDon().getMa();

        List<ThanhToan> danhSachThanhToan = thanhToanRepository.findByHoaDon_Id(hoaDon.getId());
        boolean daThanhToanTrucTuyen = danhSachThanhToan.stream().anyMatch(tt ->
                ("VNPAY".equals(tt.getPhuongThuc()) || "MOMO".equals(tt.getPhuongThuc())) && "THANH_CONG".equals(tt.getTrangThai())
        );

        if (daThanhToanTrucTuyen) throw new IllegalArgumentException("Đơn hàng đã được thanh toán trực tuyến. Vui lòng liên hệ Hotline CSKH để yêu cầu hủy và hoàn tiền!");
        if ("HOAN_THANH".equals(trangThaiHienTai) || "DANG_GIAO".equals(trangThaiHienTai) || "DA_TRA".equals(trangThaiHienTai) || "DA_DOI".equals(trangThaiHienTai)) {
            throw new IllegalArgumentException("Không thể hủy đơn hàng đã giao hoặc đang giao!");
        }
        if (!"CHO_XAC_NHAN".equals(trangThaiHienTai) && !"CHO_THANH_TOAN".equals(trangThaiHienTai)) {
            throw new IllegalArgumentException("Không thể hủy đơn hàng đang xử lý hoặc đã hoàn thành!");
        }
        if ("DA_HUY".equals(hoaDon.getTrangThaiHoaDon().getMa())) {
            throw new IllegalArgumentException("Đơn hàng này đã bị hủy từ trước!");
        }

        TrangThaiHoaDon trangThaiHuy = trangThaiHoaDonRepository.findByMa("DA_HUY");
        hoaDon.setTrangThaiHoaDon(trangThaiHuy);

        List<HoaDonChiTiet> danhSachHdct = hoaDonChiTietRepository.findByHoaDonId(hoaDon.getId());
        for (HoaDonChiTiet chiTiet : danhSachHdct) {
            SanPhamChiTiet spct = chiTiet.getSanPhamChiTiet();
            if (spct != null) {
                spct.setSoLuong(spct.getSoLuong() + chiTiet.getSoLuong());
                sanPhamChiTietRepository.save(spct);
            }
        }

        if (hoaDon.getMaGiamGia() != null) {
            MaGiamGia maGiamGia = hoaDon.getMaGiamGia();
            int luotSuDungMoi = maGiamGia.getLuotSuDung() - 1;
            if (luotSuDungMoi >= 0) {
                maGiamGia.setLuotSuDung(luotSuDungMoi);
                if (!maGiamGia.getTrangThai() && maGiamGia.getNgayKetThuc().isAfter(java.time.LocalDateTime.now())) {
                    maGiamGia.setTrangThai(true);
                }
                maGiamGiaRepository.save(maGiamGia);
            }
        }

        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (xacThuc != null && xacThuc.isAuthenticated() && !xacThuc.getPrincipal().equals("anonymousUser"))
                ? nhanVienRepository.findByTaiKhoan_TenDangNhap(xacThuc.getName()).orElse(null) : null;
        if (hoaDon.getNhanVien() == null && nhanVienThaoTac != null) hoaDon.setNhanVien(nhanVienThaoTac);

        hoaDonRepository.save(hoaDon);

        if (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getTaiKhoan() != null) {
            ThongBao thongBao = new ThongBao();
            thongBao.setTaiKhoanId(hoaDon.getKhachHang().getTaiKhoan().getId());
            thongBao.setLoaiThongBao("ORDER");
            thongBao.setDaDoc(false);
            thongBao.setTieuDe("Đơn hàng đã bị hủy");
            thongBao.setNoiDung("Đơn hàng #" + hoaDon.getMa() + " đã bị hủy. Lý do: " + yeuCau.getOrDefault("ghiChu", "Khách hàng / Admin yêu cầu hủy đơn."));
            thongBaoRepository.save(thongBao);
        }

        lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hoaDon).trangThaiHoaDon(trangThaiHuy).nhanVien(nhanVienThaoTac).ghiChu(yeuCau.getOrDefault("ghiChu", "Khách hàng/Admin yêu cầu hủy đơn")).build());
        return Map.of("message", "Đơn hàng đã được hủy và hoàn sản phẩm vào kho!");
    }

    @Transactional
    public Map<String, Object> thanhToanDonHang(CheckoutRequest yeuCauThanhToan) {
        if (yeuCauThanhToan.chiTietDonHangs == null || yeuCauThanhToan.chiTietDonHangs.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống, không thể tạo đơn hàng!");
        }
        HoaDon hoaDonMoi = new HoaDon();
        hoaDonMoi.setMa("HD" + System.currentTimeMillis());
        hoaDonMoi.setLoaiHoaDon(1);
        hoaDonMoi.setNgayTao(new Date());

        ThongTinGiaoHang thongTinNhanHang = ThongTinGiaoHang.builder()
                .tenNguoiNhan(yeuCauThanhToan.tenNguoiNhan)
                .sdtNhan(yeuCauThanhToan.soDienThoai)
                .diaChiChiTiet(yeuCauThanhToan.diaChiGiaoHang)
                .maTinh(yeuCauThanhToan.maTinh)
                .maHuyen(yeuCauThanhToan.maHuyen)
                .maXa(yeuCauThanhToan.maXa)
                .build();
        hoaDonMoi.setThongTinGiaoHang(thongTinNhanHang);
        hoaDonMoi.setHinhThucGiaoHang(yeuCauThanhToan.hinhThucGiaoHang != null ? yeuCauThanhToan.hinhThucGiaoHang : "GIAO_TAN_NOI");

        if ("GIAO_TAN_NOI".equals(hoaDonMoi.getHinhThucGiaoHang()) && yeuCauThanhToan.maHuyen != null && yeuCauThanhToan.maXa != null) {
            Integer idDichVu = 53320;
            Date ngayDuKien = ghnService.tinhNgayNhanHangDuKien(yeuCauThanhToan.maHuyen, yeuCauThanhToan.maXa, idDichVu);
            if (ngayDuKien != null) hoaDonMoi.setNgayNhanHangDuKien(ngayDuKien);
        } else if ("NHAN_TAI_CUA_HANG".equals(hoaDonMoi.getHinhThucGiaoHang()) && yeuCauThanhToan.ngayHenLayHang != null) {
            hoaDonMoi.setNgayHenLayHang(yeuCauThanhToan.ngayHenLayHang);
        }

        BigDecimal chietKhauNhanVien = BigDecimal.ZERO;
        String ghiChuDonHang = yeuCauThanhToan.ghiChu != null ? yeuCauThanhToan.ghiChu : "";
        if (yeuCauThanhToan.isBuyNow != null && yeuCauThanhToan.isBuyNow) ghiChuDonHang += " [BUY_NOW]";

        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        boolean laNhanVienHoacAdmin = false;
        String tenDangNhapHienTai = "";

        if (xacThuc != null && xacThuc.isAuthenticated() && !xacThuc.getPrincipal().equals("anonymousUser")) {
            String quyenHan = xacThuc.getAuthorities().iterator().next().getAuthority();
            tenDangNhapHienTai = xacThuc.getName();
            if ("ROLE_STAFF".equals(quyenHan) || "ROLE_ADMIN".equals(quyenHan)) {
                laNhanVienHoacAdmin = true;
            } else {
                KhachHang khachHang = khachHangRepository.findByTaiKhoan_TenDangNhap(xacThuc.getName()).orElse(null);
                hoaDonMoi.setKhachHang(khachHang);
            }
        }

        if (laNhanVienHoacAdmin) {
            chietKhauNhanVien = yeuCauThanhToan.tienHang.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
            NhanVien nhanVienDangMua = nhanVienRepository.findByTaiKhoan_TenDangNhap(tenDangNhapHienTai).orElse(null);
            String tenNhanVien = nhanVienDangMua != null ? nhanVienDangMua.getHoTen() : tenDangNhapHienTai;
            if (!ghiChuDonHang.isEmpty()) ghiChuDonHang += " - ";
            ghiChuDonHang += "[Đơn mua nội bộ bởi: " + tenNhanVien + "]";
            hoaDonMoi.setNhanVien(nhanVienDangMua);
        }

        hoaDonMoi.setGhiChu(ghiChuDonHang.trim());

        if (yeuCauThanhToan.voucherId != null) {
            MaGiamGia maGiamGia = maGiamGiaRepository.findById(yeuCauThanhToan.voucherId).orElse(null);
            if (maGiamGia != null) {
                if (!maGiamGia.getTrangThai() || maGiamGia.getLuotSuDung() >= maGiamGia.getSoLuong() || (maGiamGia.getNgayKetThuc() != null && maGiamGia.getNgayKetThuc().isBefore(java.time.LocalDateTime.now()))) {
                    throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng hoặc hết hạn!");
                }
                if (hoaDonMoi.getKhachHang() != null) {
                    boolean daDungMaNay = hoaDonRepository.existsByKhachHangIdAndMaGiamGiaIdAndTrangThaiHoaDon_MaNot(hoaDonMoi.getKhachHang().getId(), maGiamGia.getId(), "DA_HUY");
                    if (daDungMaNay) throw new IllegalArgumentException("Bạn đã sử dụng mã giảm giá này cho một đơn hàng khác rồi!");
                }
                hoaDonMoi.setMaGiamGia(maGiamGia);
                int luotSuDungMoi = maGiamGia.getLuotSuDung() + 1;
                maGiamGia.setLuotSuDung(luotSuDungMoi);
                if (luotSuDungMoi >= maGiamGia.getSoLuong()) maGiamGia.setTrangThai(false);
                maGiamGiaRepository.save(maGiamGia);
            }
        }

        BigDecimal tongTienCuoi = yeuCauThanhToan.tienHang.subtract(yeuCauThanhToan.tienGiam != null ? yeuCauThanhToan.tienGiam : BigDecimal.ZERO).subtract(chietKhauNhanVien).add(yeuCauThanhToan.phiShip != null ? yeuCauThanhToan.phiShip : BigDecimal.ZERO);
        if (tongTienCuoi.compareTo(BigDecimal.ZERO) < 0) tongTienCuoi = BigDecimal.ZERO;

        hoaDonMoi.setGiaTamThoi(yeuCauThanhToan.tienHang);
        hoaDonMoi.setGiaTriKhuyenMai((yeuCauThanhToan.tienGiam != null ? yeuCauThanhToan.tienGiam : BigDecimal.ZERO).add(chietKhauNhanVien));
        hoaDonMoi.setPhiVanChuyen(yeuCauThanhToan.phiShip);
        hoaDonMoi.setGiaTong(tongTienCuoi);

        TrangThaiHoaDon trangThaiKhoiTao;
        if ("MOMO".equalsIgnoreCase(yeuCauThanhToan.phuongThucThanhToan) || "VNPAY".equalsIgnoreCase(yeuCauThanhToan.phuongThucThanhToan) || "CHUYEN_KHOAN".equalsIgnoreCase(yeuCauThanhToan.phuongThucThanhToan)) {
            trangThaiKhoiTao = trangThaiHoaDonRepository.findByMa("CHO_THANH_TOAN");
        } else {
            trangThaiKhoiTao = trangThaiHoaDonRepository.findByMa("CHO_XAC_NHAN");
        }

        hoaDonMoi.setTrangThaiHoaDon(trangThaiKhoiTao);
        HoaDon hoaDonDaLuu = hoaDonRepository.save(hoaDonMoi);
        BigDecimal tongTienHangThucTeHienTai = BigDecimal.ZERO;

        for (CheckoutItemRequest chiTietYeuCau : yeuCauThanhToan.chiTietDonHangs) {
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(chiTietYeuCau.chiTietSanPhamId).orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
            if (spct.getSoLuong() < chiTietYeuCau.soLuong) throw new IllegalArgumentException("Sản phẩm " + spct.getSanPham().getTen() + " không đủ số lượng!");

            BigDecimal giaThucTeKhiMua = (spct.getGiaSauKhuyenMai() != null && spct.getGiaSauKhuyenMai().compareTo(BigDecimal.ZERO) > 0) ? spct.getGiaSauKhuyenMai() : spct.getGiaBan();
            tongTienHangThucTeHienTai = tongTienHangThucTeHienTai.add(giaThucTeKhiMua.multiply(BigDecimal.valueOf(chiTietYeuCau.soLuong)));

            spct.setSoLuong(spct.getSoLuong() - chiTietYeuCau.soLuong);
            sanPhamChiTietRepository.save(spct);

            HoaDonChiTiet hdct = new HoaDonChiTiet();
            hdct.setHoaDon(hoaDonDaLuu);
            hdct.setSanPhamChiTiet(spct);
            hdct.setSoLuong(chiTietYeuCau.soLuong);
            hdct.setGiaTien(giaThucTeKhiMua);
            hoaDonChiTietRepository.save(hdct);
        }

        BigDecimal tongPhaiTraSauCung = tongTienHangThucTeHienTai.subtract(yeuCauThanhToan.tienGiam != null ? yeuCauThanhToan.tienGiam : BigDecimal.ZERO).subtract(chietKhauNhanVien).add(yeuCauThanhToan.phiShip != null ? yeuCauThanhToan.phiShip : BigDecimal.ZERO);
        if (tongPhaiTraSauCung.compareTo(BigDecimal.ZERO) < 0) tongPhaiTraSauCung = BigDecimal.ZERO;

        hoaDonDaLuu.setGiaTamThoi(tongTienHangThucTeHienTai);
        hoaDonDaLuu.setGiaTong(tongPhaiTraSauCung);
        hoaDonRepository.save(hoaDonDaLuu);

        ThanhToan banGhiThanhToan = new ThanhToan();
        banGhiThanhToan.setHoaDon(hoaDonDaLuu);
        banGhiThanhToan.setSoTien(tongPhaiTraSauCung);
        banGhiThanhToan.setPhuongThuc(yeuCauThanhToan.phuongThucThanhToan != null ? yeuCauThanhToan.phuongThucThanhToan : "TIEN_MAT");
        banGhiThanhToan.setLoaiThanhToan("THANH_TOAN");
        banGhiThanhToan.setTrangThai(trangThaiKhoiTao.getMa().equals("CHO_THANH_TOAN") ? "CHO_THANH_TOAN" : "THANH_CONG");
        banGhiThanhToan.setNgayThanhToan(new Date());
        thanhToanRepository.save(banGhiThanhToan);

        LichSuHoaDon lichSuMoi = new LichSuHoaDon();
        lichSuMoi.setHoaDon(hoaDonDaLuu);
        lichSuMoi.setTrangThaiHoaDon(trangThaiKhoiTao);
        lichSuMoi.setGhiChu(trangThaiKhoiTao.getMa().equals("CHO_THANH_TOAN") ? "Đang chờ thanh toán online" : "Khách hàng đặt đơn Online");
        lichSuMoi.setNgayTao(new Date());
        lichSuHoaDonRepository.save(lichSuMoi);

        if ((yeuCauThanhToan.isBuyNow == null || !yeuCauThanhToan.isBuyNow) && hoaDonDaLuu.getKhachHang() != null && hoaDonDaLuu.getKhachHang().getTaiKhoan() != null) {
            com.example.bee.entities.cart.GioHang gioHangCuaKhach = gioHangRepository.findByTaiKhoan_Id(hoaDonDaLuu.getKhachHang().getTaiKhoan().getId()).orElse(null);
            if (gioHangCuaKhach != null) {
                List<com.example.bee.entities.cart.GioHangChiTiet> danhSachSanPhamTrongGio = gioHangChiTietRepository.findByGioHang_Id(gioHangCuaKhach.getId());
                gioHangChiTietRepository.deleteAll(danhSachSanPhamTrongGio);
            }
        }

        try {
            if (hoaDonDaLuu.getKhachHang() != null && hoaDonDaLuu.getKhachHang().getTaiKhoan() != null) {
                ThongBao tbDatHang = new ThongBao();
                tbDatHang.setTaiKhoanId(hoaDonDaLuu.getKhachHang().getTaiKhoan().getId());
                tbDatHang.setTieuDe("Đặt hàng thành công");
                tbDatHang.setNoiDung("Đơn hàng #" + hoaDonDaLuu.getMa() + " đã được ghi nhận hệ thống.");
                tbDatHang.setLoaiThongBao("ORDER");
                tbDatHang.setDaDoc(false);
                thongBaoRepository.save(tbDatHang);
            }
            if (yeuCauThanhToan.email != null && !yeuCauThanhToan.email.isBlank()) {
                emailService.guiEmailXacNhanDonHang(hoaDonDaLuu, yeuCauThanhToan.email);
            }
        } catch (Exception e) {
            System.out.println("Lỗi gửi Email: " + e.getMessage());
        }

        return Map.of("message", "Đặt hàng thành công", "maHoaDon", hoaDonDaLuu.getMa(), "id", hoaDonDaLuu.getId(), "tongTienThucTe", tongPhaiTraSauCung);
    }

    public List<Map<String, Object>> layDanhSachDonHangCuaToi() {
        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        if (xacThuc == null || !xacThuc.isAuthenticated() || xacThuc.getPrincipal().equals("anonymousUser")) throw new IllegalArgumentException("Vui lòng đăng nhập");
        KhachHang khachHang = khachHangRepository.findByTaiKhoan_TenDangNhap(xacThuc.getName()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

        List<HoaDon> danhSachHoaDon = hoaDonRepository.findByKhachHangIdOrderByNgayTaoDesc(khachHang.getId());
        SimpleDateFormat dinhDangNgay = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        List<Map<String, Object>> danhSachPhanHoi = new ArrayList<>();
        for (HoaDon hd : danhSachHoaDon) {
            Map<String, Object> thongTinRutGon = new HashMap<>();
            thongTinRutGon.put("id", hd.getId());
            thongTinRutGon.put("ma", hd.getMa());
            thongTinRutGon.put("ngayTao", hd.getNgayTao() != null ? dinhDangNgay.format(hd.getNgayTao()) : "");
            thongTinRutGon.put("tongTien", hd.getGiaTong());
            thongTinRutGon.put("trangThai", hd.getTrangThaiHoaDon().getTen());
            thongTinRutGon.put("trangThaiMa", hd.getTrangThaiHoaDon().getMa());
            danhSachPhanHoi.add(thongTinRutGon);
        }
        return danhSachPhanHoi;
    }

    public Map<String, Object> traCuuNhanhHoaDon(String maHoaDon) {
        HoaDon hoaDon = hoaDonRepository.findByMa(maHoaDon);
        if (hoaDon == null) throw new IllegalArgumentException("Không tìm thấy Hóa đơn");

        Map<Integer, Integer> thongKeTraHang = new HashMap<>();
        BigDecimal tongTienHoanTra = BigDecimal.ZERO;
        List<YeuCauDoiTra> danhSachDoiTra = yeuCauDoiTraRepository.findAll().stream().filter(y -> y.getHoaDon().getId().equals(hoaDon.getId()) && "HOAN_THANH".equals(y.getTrangThai())).collect(Collectors.toList());
        for (YeuCauDoiTra yc : danhSachDoiTra) {
            if (yc.getSoTienHoan() != null) tongTienHoanTra = tongTienHoanTra.add(yc.getSoTienHoan());
            List<ChiTietDoiTra> cacChiTietTra = chiTietDoiTraRepository.findByYeuCauDoiTraId(yc.getId());
            for (ChiTietDoiTra ct : cacChiTietTra) thongKeTraHang.put(ct.getHoaDonChiTiet().getId(), thongKeTraHang.getOrDefault(ct.getHoaDonChiTiet().getId(), 0) + ct.getSoLuong());
        }

        SimpleDateFormat dinhDangNgayFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> phanHoiTraCuu = new HashMap<>();
        phanHoiTraCuu.put("ma", hoaDon.getMa());
        phanHoiTraCuu.put("ngayTao", hoaDon.getNgayTao() != null ? dinhDangNgayFull.format(hoaDon.getNgayTao()) : null);
        phanHoiTraCuu.put("ngayThanhToan", hoaDon.getNgayThanhToan() != null ? dinhDangNgayFull.format(hoaDon.getNgayThanhToan()) : null);

        String tenNhan = (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getTenNguoiNhan() != null) ? hoaDon.getThongTinGiaoHang().getTenNguoiNhan() : (hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getHoTen() : "Khách hàng");
        String sdtNhan = (hoaDon.getThongTinGiaoHang() != null && hoaDon.getThongTinGiaoHang().getSdtNhan() != null) ? hoaDon.getThongTinGiaoHang().getSdtNhan() : (hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getSoDienThoai() : "");
        String diaChiNhan = (hoaDon.getThongTinGiaoHang() != null) ? hoaDon.getThongTinGiaoHang().getDiaChiChiTiet() : "";

        phanHoiTraCuu.put("tenNguoiNhan", tenNhan);
        phanHoiTraCuu.put("sdtKhachHang", sdtNhan);
        phanHoiTraCuu.put("email", hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getEmail() : "");
        phanHoiTraCuu.put("diaChiGiaoHang", diaChiNhan);

        List<ThanhToan> lichSuThanhToan = thanhToanRepository.findByHoaDon_Id(hoaDon.getId());
        phanHoiTraCuu.put("phuongThucThanhToan", (lichSuThanhToan != null && !lichSuThanhToan.isEmpty()) ? lichSuThanhToan.get(0).getPhuongThuc() : "TIEN_MAT");

        String ghiChuSach = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : "";
        if (ghiChuSach.contains("[BUY_NOW]")) ghiChuSach = ghiChuSach.replace("[BUY_NOW]", "").trim();
        phanHoiTraCuu.put("ghiChu", ghiChuSach);
        phanHoiTraCuu.put("giaTong", hoaDon.getGiaTong());
        phanHoiTraCuu.put("phiVanChuyen", hoaDon.getPhiVanChuyen());
        phanHoiTraCuu.put("giaTriKhuyenMai", hoaDon.getGiaTriKhuyenMai());
        phanHoiTraCuu.put("trangThaiHoaDon", hoaDon.getTrangThaiHoaDon());
        phanHoiTraCuu.put("tienHoan", tongTienHoanTra);

        List<HoaDonChiTiet> danhSachChiTietHoaDon = hoaDonChiTietRepository.findByHoaDon(hoaDon);
        List<Map<String, Object>> ketQuaChiTietSanPham = new ArrayList<>();
        if (danhSachChiTietHoaDon != null && !danhSachChiTietHoaDon.isEmpty()) {
            for (HoaDonChiTiet hdct : danhSachChiTietHoaDon) {
                Map<String, Object> chiTietSP = new HashMap<>();
                chiTietSP.put("soLuong", hdct.getSoLuong());
                chiTietSP.put("giaBan", hdct.getGiaTien());
                chiTietSP.put("soLuongTra", thongKeTraHang.getOrDefault(hdct.getId(), 0));
                if (hdct.getSanPhamChiTiet() != null) {
                    chiTietSP.put("hinhAnh", hdct.getSanPhamChiTiet().getHinhAnh());
                    if (hdct.getSanPhamChiTiet().getSanPham() != null) chiTietSP.put("tenSanPham", hdct.getSanPhamChiTiet().getSanPham().getTen());
                    if (hdct.getSanPhamChiTiet().getMauSac() != null && hdct.getSanPhamChiTiet().getKichThuoc() != null) {
                        chiTietSP.put("thuocTinh", hdct.getSanPhamChiTiet().getMauSac().getTen() + " - " + hdct.getSanPhamChiTiet().getKichThuoc().getTen());
                    }
                }
                ketQuaChiTietSanPham.add(chiTietSP);
            }
        }
        phanHoiTraCuu.put("chiTiets", ketQuaChiTietSanPham);
        return phanHoiTraCuu;
    }

    public Map<String, Object> layDuLieuInHoaDon(Integer idHoaDon) {
        HoaDon hd = hoaDonRepository.findById(idHoaDon).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        List<HoaDonChiTiet> danhSachChiTiet = hoaDonChiTietRepository.findByHoaDonId(idHoaDon);

        Map<String, Object> thongTinCuaHang = new HashMap<>();
        thongTinCuaHang.put("tenCuaHang", "BEEMATE STORE");
        thongTinCuaHang.put("diaChi", "13 phố Phan Tây Nhạc, phường Xuân Phương, TP Hà Nội");
        thongTinCuaHang.put("soDienThoai", "1900 3636");

        Map<String, Object> thongTinHoaDon = new HashMap<>();
        thongTinHoaDon.put("maHoaDon", hd.getMa());
        SimpleDateFormat dinhDangNgayIn = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        thongTinHoaDon.put("ngayTao", hd.getNgayTao() != null ? dinhDangNgayIn.format(hd.getNgayTao()) : dinhDangNgayIn.format(new Date()));
        thongTinHoaDon.put("thuNgan", hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Hệ thống");

        String tenNguoiNhan = (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getTenNguoiNhan() != null) ? hd.getThongTinGiaoHang().getTenNguoiNhan() : "Khách vãng lai";
        String sdtNguoiNhan = (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getSdtNhan() != null) ? hd.getThongTinGiaoHang().getSdtNhan() : "";

        if (hd.getKhachHang() != null) {
            thongTinHoaDon.put("tenKhachHang", !tenNguoiNhan.equals("Khách vãng lai") ? tenNguoiNhan : hd.getKhachHang().getHoTen());
            thongTinHoaDon.put("sdtKhachHang", !sdtNguoiNhan.isEmpty() ? sdtNguoiNhan : hd.getKhachHang().getSoDienThoai());
            thongTinHoaDon.put("inThongTinTaiKhoan", true);
        } else {
            thongTinHoaDon.put("tenKhachHang", tenNguoiNhan);
            thongTinHoaDon.put("sdtKhachHang", sdtNguoiNhan);
            thongTinHoaDon.put("inThongTinTaiKhoan", false);
        }

        List<Map<String, Object>> danhSachSanPhamIn = new ArrayList<>();
        BigDecimal tongTienHang = BigDecimal.ZERO;
        for (HoaDonChiTiet ct : danhSachChiTiet) {
            Map<String, Object> spIn = new HashMap<>();
            String tenSP = ct.getSanPhamChiTiet().getSanPham().getTen();
            String thuocTinh = ct.getSanPhamChiTiet().getMauSac().getTen() + " - " + ct.getSanPhamChiTiet().getKichThuoc().getTen();
            spIn.put("ten", tenSP + " (" + thuocTinh + ")");
            spIn.put("soLuong", ct.getSoLuong());
            spIn.put("donGia", ct.getGiaTien());
            BigDecimal thanhTien = ct.getGiaTien().multiply(BigDecimal.valueOf(ct.getSoLuong()));
            spIn.put("thanhTien", thanhTien);
            tongTienHang = tongTienHang.add(thanhTien);
            danhSachSanPhamIn.add(spIn);
        }

        Map<String, Object> thongKeHoaDon = new HashMap<>();
        thongKeHoaDon.put("tongTienHang", tongTienHang);
        BigDecimal phiShip = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        thongKeHoaDon.put("phiVanChuyen", phiShip);
        BigDecimal tongPhaiTra = hd.getGiaTong() != null ? hd.getGiaTong() : BigDecimal.ZERO;
        BigDecimal soTienGiam = tongTienHang.add(phiShip).subtract(tongPhaiTra);
        if (soTienGiam.compareTo(BigDecimal.ZERO) < 0) soTienGiam = BigDecimal.ZERO;
        thongKeHoaDon.put("giamGia", soTienGiam);
        thongKeHoaDon.put("tongThanhToan", tongPhaiTra);

        List<ThanhToan> lichSuThanhToanIn = thanhToanRepository.findByHoaDon_Id(hd.getId());
        thongKeHoaDon.put("phuongThuc", (lichSuThanhToanIn != null && !lichSuThanhToanIn.isEmpty()) ? lichSuThanhToanIn.get(0).getPhuongThuc() : "TIEN_MAT");

        return Map.of("store", thongTinCuaHang, "order", thongTinHoaDon, "items", danhSachSanPhamIn, "summary", thongKeHoaDon);
    }

    public List<Map<String, Object>> layDonHangOnlineMoi() {
        List<String> trangThaiCho = Arrays.asList("CHO_XAC_NHAN", "CHO_GIAO", "CHO_THANH_TOAN");
        List<HoaDon> danhSachHoaDon = hoaDonRepository.findTop5ByLoaiHoaDonAndTrangThaiHoaDon_MaInOrderByNgayTaoDesc(1, trangThaiCho);
        List<Map<String, Object>> danhSachHienThi = new ArrayList<>();
        SimpleDateFormat dinhDangNgayFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (HoaDon hd : danhSachHoaDon) {
            Map<String, Object> donHangNgan = new HashMap<>();
            donHangNgan.put("id", hd.getId());
            donHangNgan.put("ma", hd.getMa());
            String tenNguoiNhan = (hd.getThongTinGiaoHang() != null && hd.getThongTinGiaoHang().getTenNguoiNhan() != null) ? hd.getThongTinGiaoHang().getTenNguoiNhan() : "Khách hàng";
            donHangNgan.put("khachHang", tenNguoiNhan);
            donHangNgan.put("tongTien", hd.getGiaTong());
            donHangNgan.put("ngayTao", hd.getNgayTao() != null ? dinhDangNgayFull.format(hd.getNgayTao()) : "");
            danhSachHienThi.add(donHangNgan);
        }
        return danhSachHienThi;
    }

    @Transactional
    public Map<String, Object> xacNhanDaNhanChuyenKhoan(Integer idHoaDon) {
        HoaDon hoaDon = hoaDonRepository.findById(idHoaDon).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
        TrangThaiHoaDon trangThaiChoXacNhan = trangThaiHoaDonRepository.findByMa("CHO_XAC_NHAN");
        hoaDon.setTrangThaiHoaDon(trangThaiChoXacNhan);
        hoaDon.setNgayThanhToan(new Date());
        hoaDonRepository.save(hoaDon);

        boolean muaNgay = hoaDon.getGhiChu() != null && hoaDon.getGhiChu().contains("[BUY_NOW]");
        if (!muaNgay && hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getTaiKhoan() != null) {
            com.example.bee.entities.cart.GioHang gioHang = gioHangRepository.findByTaiKhoan_Id(hoaDon.getKhachHang().getTaiKhoan().getId()).orElse(null);
            if (gioHang != null) gioHangChiTietRepository.deleteAll(gioHangChiTietRepository.findByGioHang_Id(gioHang.getId()));
        }

        lichSuHoaDonRepository.save(LichSuHoaDon.builder().hoaDon(hoaDon).trangThaiHoaDon(trangThaiChoXacNhan).ghiChu("Khách hàng xác nhận đã chuyển khoản qua QR code").ngayTao(new Date()).build());
        return Map.of("message", "Đã cập nhật trạng thái chờ xác nhận");
    }

    public List<Integer> layDanhSachVoucherKhachDaDung() {
        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        if (xacThuc == null || !xacThuc.isAuthenticated() || xacThuc.getPrincipal().equals("anonymousUser")) return Collections.emptyList();
        Optional<KhachHang> khachHangOpt = khachHangRepository.findByTaiKhoan_TenDangNhap(xacThuc.getName());
        if (khachHangOpt.isEmpty()) return Collections.emptyList();

        return hoaDonRepository.findByKhachHangIdOrderByNgayTaoDesc(khachHangOpt.get().getId()).stream()
                .filter(hd -> !"DA_HUY".equals(hd.getTrangThaiHoaDon().getMa()) && hd.getMaGiamGia() != null)
                .map(hd -> hd.getMaGiamGia().getId())
                .distinct().collect(Collectors.toList());
    }
}