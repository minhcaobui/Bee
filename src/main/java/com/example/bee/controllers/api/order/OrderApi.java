package com.example.bee.controllers.api.order;

import com.example.bee.dto.HoaDonChiTietResponse;
import com.example.bee.dto.HoaDonResponse;
import com.example.bee.entities.order.HoaDon;
import com.example.bee.entities.order.HoaDonChiTiet;
import com.example.bee.entities.order.LichSuHoaDon;
import com.example.bee.entities.order.TrangThaiHoaDon;
import com.example.bee.repositories.order.HoaDonChiTietRepository;
import com.example.bee.repositories.order.HoaDonRepository;
import com.example.bee.repositories.order.LichSuHoaDonRepository;
import com.example.bee.repositories.order.TrangThaiHoaDonRepository;
import com.example.bee.repositories.role.NhanVienRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/hoa-don")
@RequiredArgsConstructor
public class OrderApi {

    private final HoaDonRepository hdRepo;
    private final TrangThaiHoaDonRepository ttRepo;
    private final LichSuHoaDonRepository lsRepo;
    private final HoaDonChiTietRepository hdctRepo;
    private final NhanVienRepository nvRepo;

    @GetMapping("/don-hang")
    public List<HoaDon> getDonHangOnline() {
        return hdRepo.findByLoaiHoaDonAndTrangThaiHoaDonMaInOrderByNgayTaoDesc(1,
                Arrays.asList("CHO_XAC_NHAN", "CHO_GIAO", "DANG_GIAO"));
    }

    @GetMapping("/lich-su")
    public List<HoaDon> getLichSu() {
        return hdRepo.findLichSuHoaDon();
    }

    // Đã bỏ tham số Authentication ở đây để tránh lỗi không truyền được từ frontend
    @GetMapping("/{id}")
    public ResponseEntity<HoaDonResponse> getDetail(@PathVariable Integer id) {
        HoaDon hd = hdRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<HoaDonChiTiet> listHdct = hdctRepo.findByHoaDonId(id);

        // 1. Map sản phẩm và tính tổng tiền hàng thực tế từ các item
        BigDecimal tongTienHangThucTe = BigDecimal.ZERO;
        List<HoaDonChiTietResponse> chiTietResponses = new ArrayList<>();

        for (HoaDonChiTiet ct : listHdct) {
            BigDecimal giaBan = ct.getGiaTien();
            Integer soLuong = ct.getSoLuong();
            tongTienHangThucTe = tongTienHangThucTe.add(giaBan.multiply(BigDecimal.valueOf(soLuong)));

            chiTietResponses.add(HoaDonChiTietResponse.builder()
                    .id(ct.getId())
                    .tenSanPham(ct.getSanPhamChiTiet().getSanPham().getTen())
                    .sku(ct.getSanPhamChiTiet().getSku())
                    .thuocTinh("Size " + ct.getSanPhamChiTiet().getKichThuoc().getTen() + " - " + ct.getSanPhamChiTiet().getMauSac().getTen())
                    .hinhAnh(ct.getSanPhamChiTiet().getHinhAnh())
                    .soLuong(soLuong)
                    .donGia(ct.getSanPhamChiTiet().getGiaBan()) // Giá niêm yết để hiện giá gốc gạch ngang
                    .giaBan(giaBan) // Giá thực tế trong đơn
                    .build());
        }

        // 2. Tính toán số tiền Voucher bị thiếu hụt ở Database
        BigDecimal phiShip = hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal tongPhaiTra = hd.getGiaTong() != null ? hd.getGiaTong() : BigDecimal.ZERO;

        // Voucher = (Tiền hàng + Ship) - Tổng thanh toán
        BigDecimal voucherCalculated = tongTienHangThucTe.add(phiShip).subtract(tongPhaiTra);
        if (voucherCalculated.compareTo(BigDecimal.ZERO) < 0) voucherCalculated = BigDecimal.ZERO;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 3. Logic lấy tên nhân viên đang đăng nhập bằng SecurityContextHolder
        String tenNhanVienHienTai = "Hệ thống";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName(); // Lấy email/tên đăng nhập
            System.out.println("====> [DEBUG] Lấy Detail - API được gọi bởi User: " + username);

            // Chọc vào DB để lấy NhanVien
            var nhanVienDangNhap = nvRepo.findByTaiKhoan_TenDangNhap(username).orElse(null);
            if (nhanVienDangNhap != null) {
                tenNhanVienHienTai = nhanVienDangNhap.getHoTen();
            } else {
                System.out.println("====> [DEBUG] Không tìm thấy thông tin nhân viên cho username này trong CSDL.");
            }
        }

        HoaDonResponse response = HoaDonResponse.builder()
                .id(hd.getId())
                .ma(hd.getMa())
                .tenKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getHoTen() : "Khách vãng lai")
                // Gán tên nhân viên tạo đơn (nếu có) hoặc người đang thao tác
                .tenNhanVien(hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : tenNhanVienHienTai)
                .sdtNhan(hd.getSdtNhan())
                .diaChiGiaoHang(hd.getDiaChiGiaoHang())
                .phuongThucThanhToan(hd.getPhuongThucThanhToan())
                .trangThaiMa(hd.getTrangThaiHoaDon().getMa())
                .trangThaiTen(hd.getTrangThaiHoaDon().getTen())
                .loaiHoaDon(hd.getLoaiHoaDon())
                .ngayTao(hd.getNgayTao() != null ? sdf.format(hd.getNgayTao()) : null)
                .tienHang(tongTienHangThucTe)
                .phiVanChuyen(phiShip)
                .tienGiamVoucher(voucherCalculated) // Gán giá trị vừa tính toán động
                .tienGiamSale(BigDecimal.ZERO)
                .tongTien(tongPhaiTra)
                .chiTiets(chiTietResponses)
                .build();

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
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái hiện tại không thể chuyển tiếp");
        }

        TrangThaiHoaDon nextStatus = ttRepo.findByMa(nextMa);
        hd.setTrangThaiHoaDon(nextStatus);

        // --- ĐOẠN FIX: Lấy thằng đang thao tác để gán vào Hóa đơn ---
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                ? nvRepo.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null)
                : null;

        // Nếu hóa đơn chưa có ai phụ trách thì gán thằng này vào luôn
        if (hd.getNhanVien() == null && nhanVienThaoTac != null) {
            hd.setNhanVien(nhanVienThaoTac);
        }

        hdRepo.save(hd);

        // Lưu luôn cả thằng thao tác vào Lịch Sử để sau này lôi đầu ra chửi nếu làm sai
        lsRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(nextStatus)
                .nhanVien(nhanVienThaoTac) // Gắn tên thằng nhân viên vào đây!
                .ghiChu(ghiChu)
                .build());

        return ResponseEntity.ok(Map.of(
                "message", "Cập nhật thành công",
                "nextStatus", nextStatus.getTen(),
                "nextStatusMa", nextStatus.getMa()
        ));
    }

    @PatchMapping("/{id}/huy")
    @Transactional
    public ResponseEntity<?> huyDon(@PathVariable Integer id, @RequestBody Map<String, String> req) {
        HoaDon hd = hdRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn"));

        TrangThaiHoaDon ttHuy = ttRepo.findByMa("DA_HUY");
        hd.setTrangThaiHoaDon(ttHuy);

        // --- FIX TƯƠNG TỰ CHO NÚT HỦY ---
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var nhanVienThaoTac = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser"))
                ? nvRepo.findByTaiKhoan_TenDangNhap(auth.getName()).orElse(null)
                : null;

        if (hd.getNhanVien() == null && nhanVienThaoTac != null) {
            hd.setNhanVien(nhanVienThaoTac);
        }

        hdRepo.save(hd);

        lsRepo.save(LichSuHoaDon.builder()
                .hoaDon(hd)
                .trangThaiHoaDon(ttHuy)
                .nhanVien(nhanVienThaoTac) // Gắn tên vào lịch sử hủy
                .ghiChu(req.getOrDefault("ghiChu", "Khách hàng/Admin yêu cầu hủy đơn"))
                .build());

        return ResponseEntity.ok(Map.of("message", "Đơn hàng đã được chuyển sang trạng thái hủy"));
    }
}