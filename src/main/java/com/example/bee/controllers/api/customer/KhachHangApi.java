package com.example.bee.controllers.api.customer;

import com.example.bee.dto.DanhGiaRequest;
import com.example.bee.dto.DiaChiRequest;
import com.example.bee.dto.KhachHangRequest;
import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.account.VaiTro;
import com.example.bee.entities.cart.GioHang;
import com.example.bee.entities.customer.DiaChiKhachHang;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.order.HoaDon;
import com.example.bee.entities.order.HoaDonChiTiet;
import com.example.bee.entities.product.SanPham;
import com.example.bee.entities.product.SanPhamYeuThich;
import com.example.bee.entities.reviews.DanhGia;
import com.example.bee.entities.staff.NhanVien;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.account.VaiTroRepository;
import com.example.bee.repositories.customer.DiaChiKhachHangRepository;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.order.HoaDonChiTietRepository;
import com.example.bee.repositories.order.HoaDonRepository;
import com.example.bee.repositories.reviews.DanhGiaRepository;
import com.example.bee.repositories.staff.NhanVienRepository;
import com.example.bee.repositories.products.SanPhamYeuThichRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangApi {

    private static final Map<String, String> otpStorageKhach = new HashMap<>();
    private static final SecureRandom RAND = new SecureRandom();
    private final org.springframework.mail.javamail.JavaMailSender mailSender;
    private final KhachHangRepository khRepo;
    private final NhanVienRepository nvRepo;
    private final DiaChiKhachHangRepository dcRepo;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;
    private final SanPhamYeuThichRepository wishlistRepo;
    private final DanhGiaRepository danhGiaRepo;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final HoaDonRepository hoaDonRepository;
    private final VaiTroRepository vaiTroRepo;
    private final com.example.bee.repositories.cart.GioHangRepository gioHangRepository;
    @Value("${spring.mail.username}")
    private String senderEmail;

    private String generateMa() {
        return "KH" + System.currentTimeMillis();
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<KhachHang> pageData = khRepo.search(q, trangThai, pageable);
        Page<java.util.Map<String, Object>> safePage = pageData.map(kh -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", kh.getId());
            map.put("ma", kh.getMa());
            map.put("hoTen", kh.getHoTen());
            map.put("soDienThoai", kh.getSoDienThoai());
            map.put("email", kh.getEmail());
            map.put("gioiTinh", kh.getGioiTinh());
            map.put("ngaySinh", kh.getNgaySinh());
            map.put("trangThai", kh.getTrangThai());
            return map;
        });
        return ResponseEntity.ok(safePage);
    }

    @GetMapping("/{id}")
    public KhachHang getDetail(@PathVariable Integer id) {
        return khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody KhachHangRequest req) {
        String ten = req.getHoTen() != null ? req.getHoTen().trim() : "";
        if (ten.isEmpty() || ten.length() > 100) {
            return ResponseEntity.badRequest().body(Map.of("message", "Họ tên không được để trống và tối đa 100 ký tự"));
        }

        String sdt = req.getSoDienThoai() != null ? req.getSoDienThoai().trim() : "";
        if (sdt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại không được để trống"));
        }
        if (!sdt.matches("^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại không đúng định dạng"));
        }
        if (khRepo.existsBySoDienThoai(sdt)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại đã tồn tại trong hệ thống"));
        }
        if (taiKhoanRepository.existsByTenDangNhap(sdt)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại này đã được đăng ký tài khoản khác"));
        }

        String email = req.getEmail() != null ? req.getEmail().trim() : "";
        if (!email.isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email không đúng định dạng"));
            }
            if (khRepo.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email đã tồn tại trong hệ thống"));
            }
        }

        VaiTro roleCustomer = vaiTroRepo.findByMa("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình quyền ROLE_CUSTOMER"));

        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap(sdt);
        tk.setMatKhau(passwordEncoder.encode("123456"));
        tk.setVaiTro(roleCustomer);
        tk.setTrangThai(true);
        TaiKhoan savedTk = taiKhoanRepository.save(tk);

        GioHang gioHang = new GioHang();
        gioHang.setTaiKhoan(savedTk);
        gioHangRepository.save(gioHang);

        KhachHang kh = new KhachHang();
        kh.setMa(generateMa());
        kh.setHoTen(ten);
        kh.setGioiTinh(req.getGioiTinh() != null && req.getGioiTinh() ? "Nam" : "Nữ");
        kh.setNgaySinh(req.getNgaySinh());
        kh.setSoDienThoai(sdt);
        kh.setEmail(email.isEmpty() ? null : email);
        kh.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : true);
        if (req.getHinhAnh() != null) kh.setHinhAnh(req.getHinhAnh());

        kh.setTaiKhoan(savedTk);

        String tinh = req.getTinhThanhPho() != null ? req.getTinhThanhPho().trim() : "";
        String huyen = req.getQuanHuyen() != null ? req.getQuanHuyen().trim() : "";
        String xa = req.getPhuongXa() != null ? req.getPhuongXa().trim() : "";
        String chiTiet = req.getDiaChiChiTiet() != null ? req.getDiaChiChiTiet().trim() : "";
        boolean hasRealAddress = !tinh.isEmpty() && !tinh.equals("Chưa cập nhật") && !tinh.equals("null");
        if (hasRealAddress) {
            String fullAddress = String.format("%s, %s, %s, %s", chiTiet, xa, huyen, tinh);
            kh.setDiaChi(fullAddress);
        } else {
            kh.setDiaChi("Khách lẻ / Mua tại cửa hàng");
        }
        KhachHang savedKh = khRepo.save(kh);

        if (hasRealAddress) {
            DiaChiKhachHang dc = new DiaChiKhachHang();
            dc.setKhachHang(savedKh);
            dc.setHoTenNhan(ten);
            dc.setSdtNhan(sdt);
            dc.setDiaChiChiTiet(chiTiet);
            dc.setPhuongXa(xa);
            dc.setQuanHuyen(huyen);
            dc.setTinhThanhPho(tinh);
            dc.setMaTinh(req.getMaTinh());
            dc.setMaHuyen(req.getMaHuyen());
            dc.setMaXa(req.getMaXa());
            dc.setLoaiDiaChi("Nhà riêng");
            dc.setLaMacDinh(true);
            dc.setTrangThai(true);
            dcRepo.save(dc);
        }
        return ResponseEntity.ok(savedKh);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody KhachHangRequest req) {
        KhachHang kh = khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        if (req.getHoTen() != null) {
            String ten = req.getHoTen().trim();
            if (ten.isEmpty() || ten.length() > 100) {
                return ResponseEntity.badRequest().body(Map.of("message", "Họ tên không được để trống và tối đa 100 ký tự"));
            }
            kh.setHoTen(ten);
        }

        if (req.getGioiTinh() != null) kh.setGioiTinh(req.getGioiTinh() ? "Nam" : "Nữ");
        if (req.getNgaySinh() != null) kh.setNgaySinh(req.getNgaySinh());

        if (req.getSoDienThoai() != null) {
            String sdtNew = req.getSoDienThoai().trim();
            if (sdtNew.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại không được để trống"));
            }
            if (!sdtNew.matches("^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại không đúng định dạng"));
            }
            if (khRepo.existsBySoDienThoaiAndIdNot(sdtNew, id)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại đã được sử dụng bởi khách hàng khác"));
            }

            kh.setSoDienThoai(sdtNew);
        }

        if (req.getEmail() != null) {
            String emailNew = req.getEmail().trim();
            if (!emailNew.isEmpty()) {
                if (!emailNew.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Email không đúng định dạng"));
                }
                if (khRepo.existsByEmailAndIdNot(emailNew, id)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Email đã được sử dụng bởi khách hàng khác"));
                }
                kh.setEmail(emailNew);
            } else {
                kh.setEmail(null);
            }
        }

        if (req.getTrangThai() != null) {
            kh.setTrangThai(req.getTrangThai());
            if (kh.getTaiKhoan() != null) {
                kh.getTaiKhoan().setTrangThai(req.getTrangThai());
            }
        }
        if (req.getHinhAnh() != null) kh.setHinhAnh(req.getHinhAnh());

        return ResponseEntity.ok(khRepo.save(kh));
    }

    @PatchMapping("/{id}/trang-thai")
    @Transactional
    public ResponseEntity<?> quickToggleStatus(@PathVariable Integer id) {
        KhachHang kh = khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khách hàng không tồn tại"));

        kh.setTrangThai(!kh.getTrangThai());

        if (kh.getTaiKhoan() != null) {
            TaiKhoan tk = kh.getTaiKhoan();
            tk.setTrangThai(kh.getTrangThai());
            taiKhoanRepository.save(tk);
        }

        return ResponseEntity.ok(khRepo.save(kh));
    }

    @GetMapping("/{id}/hoa-don")
    public ResponseEntity<?> getCustomerOrders(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "") String statusId
    ) {
        List<HoaDon> allOrders = hoaDonRepository.findByKhachHangIdOrderByNgayTaoDesc(id);

        List<HoaDon> filteredList = allOrders.stream()
                .filter(hd -> q.isEmpty() || hd.getMa().toLowerCase().contains(q.toLowerCase()))
                .filter(hd -> statusId.isEmpty() || (hd.getTrangThaiHoaDon() != null && hd.getTrangThaiHoaDon().getMa().equals(statusId)))
                .collect(Collectors.toList());

        int start = page * size;
        int end = Math.min((start + size), filteredList.size());
        List<HoaDon> pageContent = new ArrayList<>();
        if (start <= filteredList.size()) {
            pageContent = filteredList.subList(start, end);
        }

        int totalPages = (int) Math.ceil((double) filteredList.size() / size);

        return ResponseEntity.ok(Map.of(
                "content", pageContent,
                "totalPages", totalPages,
                "totalElements", filteredList.size(),
                "numberOfElements", pageContent.size()
        ));
    }

    @GetMapping("/{id}/san-pham-da-mua")
    public ResponseEntity<?> getProductsBought(@PathVariable Integer id,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "5") int size) {
        List<HoaDon> orders = hoaDonRepository.findByKhachHangIdOrderByNgayTaoDesc(id);

        List<HoaDon> completedOrders = orders.stream()
                .filter(hd -> hd.getTrangThaiHoaDon() != null && "HOAN_THANH".equals(hd.getTrangThaiHoaDon().getMa()))
                .collect(Collectors.toList());

        Map<Integer, Map<String, Object>> productStats = new java.util.HashMap<>();

        for (HoaDon hd : completedOrders) {
            List<HoaDonChiTiet> details = hoaDonChiTietRepository.findByHoaDonId(hd.getId());
            for (HoaDonChiTiet ct : details) {
                Integer spctId = ct.getSanPhamChiTiet().getId();

                productStats.putIfAbsent(spctId, new java.util.HashMap<>(Map.of(
                        "spct", ct.getSanPhamChiTiet(),
                        "soLuongMua", 0,
                        "tongTienChi", BigDecimal.ZERO
                )));

                Map<String, Object> stat = productStats.get(spctId);
                stat.put("soLuongMua", (int) stat.get("soLuongMua") + ct.getSoLuong());

                BigDecimal currentTotal = (BigDecimal) stat.get("tongTienChi");
                BigDecimal itemTotal = ct.getGiaTien().multiply(BigDecimal.valueOf(ct.getSoLuong()));
                stat.put("tongTienChi", currentTotal.add(itemTotal));
            }
        }

        List<Map<String, Object>> sortedList = new java.util.ArrayList<>(productStats.values());
        sortedList.sort((a, b) -> Integer.compare((int) b.get("soLuongMua"), (int) a.get("soLuongMua")));

        int start = Math.min(page * size, sortedList.size());
        int end = Math.min(start + size, sortedList.size());
        List<Map<String, Object>> pageContent = sortedList.subList(start, end);
        int totalPages = (int) Math.ceil((double) sortedList.size() / size);

        return ResponseEntity.ok(Map.of(
                "content", pageContent,
                "totalPages", totalPages,
                "totalElements", sortedList.size(),
                "numberOfElements", pageContent.size()
        ));
    }

    @GetMapping("/{id}/voucher-da-dung")
    public ResponseEntity<?> getVouchersUsed(@PathVariable Integer id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "5") int size) {
        List<com.example.bee.entities.order.HoaDon> orders = hoaDonRepository.findByKhachHangIdOrderByNgayTaoDesc(id);

        List<com.example.bee.entities.order.HoaDon> ordersWithVoucher = orders.stream()
                .filter(hd -> hd.getMaGiamGia() != null
                        && hd.getTrangThaiHoaDon() != null
                        && "HOAN_THANH".equals(hd.getTrangThaiHoaDon().getMa()))
                .collect(java.util.stream.Collectors.toList());

        List<Map<String, Object>> voucherList = new java.util.ArrayList<>();

        // 🌟 ĐÃ FIX: Dùng SimpleDateFormat để xử lý chuẩn xác kiểu java.util.Date của sếp
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        for (com.example.bee.entities.order.HoaDon hd : ordersWithVoucher) {
            // Lấy ra đúng kiểu java.util.Date
            java.util.Date ngay = hd.getNgayThanhToan() != null ? hd.getNgayThanhToan() : hd.getNgayTao();
            String formattedDate = ngay != null ? formatter.format(ngay) : "---";

            voucherList.add(Map.of(
                    "maVoucher", hd.getMaGiamGia().getMaCode(),
                    "tenVoucher", hd.getMaGiamGia().getTen(),
                    "giamGia", hd.getGiaTriKhuyenMai(),
                    "maDonHang", hd.getMa(),
                    "ngaySuDung", formattedDate // Trả về chuỗi đẹp luôn
            ));
        }

        int start = Math.min(page * size, voucherList.size());
        int end = Math.min(start + size, voucherList.size());
        List<Map<String, Object>> pageContent = voucherList.subList(start, end);
        int totalPages = (int) Math.ceil((double) voucherList.size() / size);

        return ResponseEntity.ok(Map.of(
                "content", pageContent,
                "totalPages", totalPages,
                "totalElements", voucherList.size(),
                "numberOfElements", pageContent.size()
        ));
    }

    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        String username = authentication.getName();

        Optional<KhachHang> khOpt = khRepo.findByTaiKhoan_TenDangNhap(username);
        if (khOpt.isPresent()) {
            KhachHang kh = khOpt.get();
            return ResponseEntity.ok(Map.of(
                    "hoTen", kh.getHoTen() != null ? kh.getHoTen() : "Khách hàng",
                    "soDienThoai", kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "",
                    "email", kh.getEmail() != null ? kh.getEmail() : "",
                    "gioiTinh", kh.getGioiTinh() != null ? kh.getGioiTinh() : "",
                    "ngaySinh", kh.getNgaySinh() != null ? kh.getNgaySinh().toString() : "",
                    "hinhAnh", kh.getHinhAnh() != null ? kh.getHinhAnh() : "",
                    "taiKhoanId", kh.getTaiKhoan() != null ? kh.getTaiKhoan().getId() : 0,
                    "tenDangNhap", kh.getTaiKhoan() != null ? kh.getTaiKhoan().getTenDangNhap() : "",
                    "daDoiTenDangNhap", kh.getTaiKhoan() != null && Boolean.TRUE.equals(kh.getTaiKhoan().getDaDoiTenDangNhap())
            ));
        }

        Optional<NhanVien> nvOpt = nvRepo.findByTaiKhoan_TenDangNhap(username);
        if (nvOpt.isPresent()) {
            NhanVien nv = nvOpt.get();
            return ResponseEntity.ok(Map.of(
                    "hoTen", nv.getHoTen() != null ? nv.getHoTen() : "Nhân viên",
                    "soDienThoai", nv.getSoDienThoai() != null ? nv.getSoDienThoai() : "",
                    "email", nv.getEmail() != null ? nv.getEmail() : "",
                    "gioiTinh", nv.getGioiTinh() != null ? nv.getGioiTinh() : "",
                    "ngaySinh", nv.getNgaySinh() != null ? nv.getNgaySinh().toString() : "",
                    "hinhAnh", nv.getHinhAnh() != null ? nv.getHinhAnh() : "",
                    "taiKhoanId", nv.getTaiKhoan() != null ? nv.getTaiKhoan().getId() : 0,
                    "tenDangNhap", nv.getTaiKhoan() != null ? nv.getTaiKhoan().getTenDangNhap() : "",
                    "daDoiTenDangNhap", nv.getTaiKhoan() != null && Boolean.TRUE.equals(nv.getTaiKhoan().getDaDoiTenDangNhap())
            ));
        }

        return ResponseEntity.ok(Map.of(
                "hoTen", "Người dùng mới",
                "soDienThoai", username,
                "email", "Chưa cập nhật",
                "gioiTinh", "",
                "ngaySinh", "",
                "hinhAnh", "",
                "taiKhoanId", 0
        ));
    }

    @PutMapping("/my-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        String username = authentication.getName();

        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(username)
                .orElse(null);

        if (taiKhoan == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Không tìm thấy tài khoản hợp lệ!"));
        }

        Optional<NhanVien> nvOpt = nvRepo.findByTaiKhoan_TenDangNhap(username);
        if (nvOpt.isPresent()) {
            NhanVien nv = nvOpt.get();
            nv.setHoTen(payload.get("hoTen"));
            nv.setSoDienThoai(payload.get("soDienThoai"));
            nv.setEmail(payload.get("email"));
            nv.setGioiTinh(payload.get("gioiTinh"));
            nv.setDiaChi(payload.get("diaChi"));

            if (payload.get("ngaySinh") != null && !payload.get("ngaySinh").isEmpty()) {
                nv.setNgaySinh(java.sql.Date.valueOf(payload.get("ngaySinh")));
            }

            if (payload.get("hinhAnh") != null && !payload.get("hinhAnh").trim().isEmpty()) {
                nv.setHinhAnh(payload.get("hinhAnh"));
            }

            nvRepo.save(nv);
            return ResponseEntity.ok(Map.of("message", "Cập nhật hồ sơ nhân viên thành công!"));
        }

        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(username).orElse(null);

        if (kh == null) {
            kh = new KhachHang();
            kh.setTaiKhoan(taiKhoan);
            kh.setTrangThai(true);
            kh.setMa("KH" + System.currentTimeMillis());
        }

        kh.setHoTen(payload.get("hoTen"));
        kh.setSoDienThoai(payload.get("soDienThoai"));
        kh.setEmail(payload.get("email"));
        kh.setGioiTinh(payload.get("gioiTinh"));
        kh.setDiaChi(payload.get("diaChi"));

        if (payload.get("ngaySinh") != null && !payload.get("ngaySinh").toString().isEmpty()) {
            kh.setNgaySinh(java.time.LocalDate.parse(payload.get("ngaySinh").toString()));
        }

        if (payload.get("hinhAnh") != null && !payload.get("hinhAnh").trim().isEmpty()) {
            kh.setHinhAnh(payload.get("hinhAnh"));
        }

        khRepo.save(kh);

        boolean isUsernameChanged = false;
        String newUsername = payload.get("tenDangNhap");

        if (newUsername != null && !newUsername.trim().isEmpty() && !newUsername.equals(taiKhoan.getTenDangNhap())) {

            if (Boolean.TRUE.equals(taiKhoan.getDaDoiTenDangNhap())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Bạn chỉ được đổi tên đăng nhập 1 lần duy nhất!"));
            }

            if (taiKhoanRepository.existsByTenDangNhap(newUsername)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Tên đăng nhập này đã có người sử dụng!"));
            }

            taiKhoan.setTenDangNhap(newUsername);
            taiKhoan.setDaDoiTenDangNhap(true);
            taiKhoanRepository.save(taiKhoan);

            isUsernameChanged = true;
        }

        return ResponseEntity.ok(Map.of(
                "message", "Cập nhật thành công!",
                "usernameChanged", isUsernameChanged
        ));

    }

    @GetMapping("/{id}/dia-chi")
    public ResponseEntity<?> getAddressList(@PathVariable Integer id) {
        if (!khRepo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok(dcRepo.findByKhachHangId(id));
    }

    @PostMapping("/{id}/dia-chi")
    @Transactional
    public ResponseEntity<?> addAddress(@PathVariable Integer id, @RequestBody DiaChiRequest req) {
        KhachHang kh = khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        DiaChiKhachHang dc = new DiaChiKhachHang();
        dc.setKhachHang(kh);
        dc.setDiaChiChiTiet(req.getDiaChiChiTiet());
        dc.setPhuongXa(req.getPhuongXa());
        dc.setQuanHuyen(req.getQuanHuyen());
        dc.setTinhThanhPho(req.getTinhThanhPho());
        dc.setMaTinh(req.getMaTinh());
        dc.setMaHuyen(req.getMaHuyen());
        dc.setMaXa(req.getMaXa());
        dc.setHoTenNhan(req.getHoTenNhan() != null ? req.getHoTenNhan() : kh.getHoTen());
        dc.setSdtNhan(req.getSdtNhan() != null ? req.getSdtNhan() : kh.getSoDienThoai());
        dc.setLoaiDiaChi("Khác");
        dc.setLaMacDinh(false);
        dc.setTrangThai(true);
        return ResponseEntity.ok(dcRepo.save(dc));
    }

    @DeleteMapping("/dia-chi/{idDiaChi}")
    @Transactional
    public ResponseEntity<?> deleteAddress(@PathVariable Integer idDiaChi) {
        DiaChiKhachHang dc = dcRepo.findById(idDiaChi)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (dc.getLaMacDinh()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa địa chỉ mặc định");
        }
        dcRepo.delete(dc);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/dia-chi/{idDiaChi}/mac-dinh")
    @Transactional
    public ResponseEntity<?> setDefaultAddress(@PathVariable Integer idDiaChi) {
        DiaChiKhachHang newDefault = dcRepo.findById(idDiaChi)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Integer idKhach = newDefault.getKhachHang().getId();
        dcRepo.findByKhachHangIdAndLaMacDinhTrue(idKhach).ifPresent(old -> {
            old.setLaMacDinh(false);
            dcRepo.save(old);
        });
        newDefault.setLaMacDinh(true);
        return ResponseEntity.ok(dcRepo.save(newDefault));
    }

    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập"));
            }
            String currentUsername = auth.getName();
            KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(currentUsername)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));
            TaiKhoan taiKhoan = kh.getTaiKhoan();
            if (taiKhoan == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Tài khoản không tồn tại"));
            }
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            if (!passwordEncoder.matches(oldPassword, taiKhoan.getMatKhau())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Sai mật khẩu hiện tại"));
            }
            taiKhoan.setMatKhau(passwordEncoder.encode(newPassword));
            taiKhoanRepository.save(taiKhoan);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/addresses")
    public ResponseEntity<?> getMyAddresses(Authentication authentication) {
        String username = authentication.getName();
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        return ResponseEntity.ok(dcRepo.findByKhachHangId(kh.getId()));
    }

    @GetMapping("/addresses/default")
    public ResponseEntity<?> getDefaultAddress(Authentication authentication) {
        String username = authentication.getName();
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return ResponseEntity.ok(dcRepo.findByKhachHangIdAndLaMacDinhTrue(kh.getId()).orElse(null));
    }

    @PostMapping("/addresses")
    @Transactional
    public ResponseEntity<?> addMyAddress(@RequestBody DiaChiRequest req, Authentication authentication) {
        String username = authentication.getName();
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(username).orElseThrow();

        if (req.getLaMacDinh() != null && req.getLaMacDinh()) {
            dcRepo.findByKhachHangIdAndLaMacDinhTrue(kh.getId()).ifPresent(old -> {
                old.setLaMacDinh(false);
                dcRepo.save(old);
            });
        }

        DiaChiKhachHang dc = new DiaChiKhachHang();
        dc.setKhachHang(kh);
        mapRequestToEntity(req, dc);
        return ResponseEntity.ok(dcRepo.save(dc));
    }

    @PutMapping("/addresses/{id}")
    @Transactional
    public ResponseEntity<?> updateMyAddress(@PathVariable Integer id, @RequestBody DiaChiRequest req, Authentication authentication) {
        DiaChiKhachHang dc = dcRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!dc.getKhachHang().getTaiKhoan().getTenDangNhap().equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (req.getLaMacDinh() != null && req.getLaMacDinh()) {
            dcRepo.findByKhachHangIdAndLaMacDinhTrue(dc.getKhachHang().getId()).ifPresent(old -> {
                if (!old.getId().equals(id)) {
                    old.setLaMacDinh(false);
                    dcRepo.save(old);
                }
            });
        }

        mapRequestToEntity(req, dc);
        return ResponseEntity.ok(dcRepo.save(dc));
    }

    @PutMapping("/addresses/{id}/default")
    @Transactional
    public ResponseEntity<?> setMyDefaultAddress(@PathVariable Integer id, Authentication authentication) {
        DiaChiKhachHang newDef = dcRepo.findById(id).orElseThrow();
        if (!newDef.getKhachHang().getTaiKhoan().getTenDangNhap().equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        dcRepo.findByKhachHangIdAndLaMacDinhTrue(newDef.getKhachHang().getId()).ifPresent(old -> {
            old.setLaMacDinh(false);
            dcRepo.save(old);
        });

        newDef.setLaMacDinh(true);
        return ResponseEntity.ok(dcRepo.save(newDef));
    }

    @DeleteMapping("/addresses/{idDiaChi}")
    public ResponseEntity<?> deleteMyAddress(@PathVariable Integer idDiaChi, Authentication authentication) {
        DiaChiKhachHang dc = dcRepo.findById(idDiaChi)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!dc.getKhachHang().getTaiKhoan().getTenDangNhap().equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (dc.getLaMacDinh()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa địa chỉ mặc định");
        }
        dcRepo.delete(dc);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/wishlist/check/{sanPhamId}")
    public ResponseEntity<?> checkWishlist(@PathVariable Integer sanPhamId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.ok(Map.of("isSaved", false));
        }
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(authentication.getName()).orElse(null);
        if (kh == null || kh.getTaiKhoan() == null) return ResponseEntity.ok(Map.of("isSaved", false));

        boolean isSaved = wishlistRepo.findFirstByTaiKhoanIdAndSanPhamId(kh.getTaiKhoan().getId(), sanPhamId).isPresent();
        return ResponseEntity.ok(Map.of("isSaved", isSaved));
    }

    @PostMapping("/wishlist/toggle/{sanPhamId}")
    @Transactional
    public ResponseEntity<?> toggleWishlist(@PathVariable Integer sanPhamId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập!"));
        }
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(authentication.getName()).orElse(null);
        if (kh == null || kh.getTaiKhoan() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập!"));
        }

        Integer userId = kh.getTaiKhoan().getId();
        var exist = wishlistRepo.findFirstByTaiKhoanIdAndSanPhamId(userId, sanPhamId);

        if (exist.isPresent()) {
            wishlistRepo.delete(exist.get());
            return ResponseEntity.ok(Map.of("message", "Đã bỏ lưu sản phẩm khỏi danh sách yêu thích", "status", "removed"));
        } else {
            SanPhamYeuThich wl = new SanPhamYeuThich();
            wl.setTaiKhoanId(userId);
            wl.setSanPhamId(sanPhamId);
            wishlistRepo.save(wl);
            return ResponseEntity.ok(Map.of("message", "Đã lưu vào danh sách yêu thích!", "status", "added"));
        }
    }

    @GetMapping("/reviews/{sanPhamId}")
    public ResponseEntity<?> getReviews(@PathVariable Integer sanPhamId) {
        List<DanhGia> list = danhGiaRepo.findBySanPhamIdOrderByNgayTaoDesc(sanPhamId);
        List<KhachHang> allKhachHang = khRepo.findAll();

        for (DanhGia dg : list) {
            String tenNguoiDanhGia = "Khách hàng ẩn danh";

            for (KhachHang kh : allKhachHang) {
                if (kh.getTaiKhoan() != null && dg.getTaiKhoan() != null && kh.getTaiKhoan().getId().equals(dg.getTaiKhoan().getId())) {
                    if (kh.getHoTen() != null && !kh.getHoTen().trim().isEmpty()) {
                        tenNguoiDanhGia = kh.getHoTen();
                    }
                    break;
                }
            }
            dg.setTenKhachHang(tenNguoiDanhGia);
        }

        return ResponseEntity.ok(list);
    }

    @PostMapping("/reviews/detail/{orderDetailId}")
    @Transactional
    public ResponseEntity<?> addReview(
            @PathVariable Integer orderDetailId,
            @RequestBody DanhGiaRequest req,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(auth.getName()).orElse(null);
        if (tk == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        HoaDonChiTiet hdct = hoaDonChiTietRepository.findById(orderDetailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chi tiết hóa đơn"));

        if (hdct.getHoaDon() == null || hdct.getHoaDon().getKhachHang() == null ||
                hdct.getHoaDon().getKhachHang().getTaiKhoan() == null ||
                !hdct.getHoaDon().getKhachHang().getTaiKhoan().getId().equals(tk.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Bạn không có quyền đánh giá sản phẩm của đơn hàng này!"));
        }

        if (hdct.getHoaDon().getTrangThaiHoaDon() == null || !"HOAN_THANH".equals(hdct.getHoaDon().getTrangThaiHoaDon().getMa())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chỉ đơn hàng đã giao thành công mới được phép đánh giá!"));
        }

        SanPham sp = hdct.getSanPhamChiTiet().getSanPham();

        Optional<DanhGia> existingOpt = danhGiaRepo.findByHoaDonChiTiet_Id(orderDetailId);

        DanhGia danhGia;
        if (existingOpt.isPresent()) {
            danhGia = existingOpt.get();

            if (Boolean.TRUE.equals(danhGia.getDaSua())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Bạn chỉ được phép sửa đánh giá 1 lần duy nhất!"));
            }

            danhGia.setDaSua(true);
        } else {
            danhGia = new DanhGia();
            danhGia.setDaSua(false);
            danhGia.setNgayTao(java.time.LocalDateTime.now());
        }

        danhGia.setTaiKhoan(tk);
        danhGia.setSanPham(sp);
        danhGia.setHoaDonChiTiet(hdct);

        String noiDung = req.getNoiDung() != null ? req.getNoiDung().trim() : "";
        if (noiDung.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập nội dung đánh giá!"));
        }

        danhGia.setSoSao(req.getSoSao() != null ? req.getSoSao() : 5);
        danhGia.setNoiDung(noiDung);
        danhGia.setPhanLoai(req.getPhanLoai());
        danhGia.setDanhSachHinhAnh(req.getDanhSachHinhAnh());

        danhGiaRepo.save(danhGia);
        return ResponseEntity.ok(Map.of("message", "Đánh giá thành công!"));
    }

    @GetMapping("/wishlist")
    public ResponseEntity<?> getMyWishlist(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(authentication.getName()).orElse(null);
        if (kh == null || kh.getTaiKhoan() == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        List<SanPhamYeuThich> list = wishlistRepo.findAll().stream()
                .filter(w -> w.getTaiKhoanId().equals(kh.getTaiKhoan().getId()))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(list);
    }

    @GetMapping("/my-reviews")
    public ResponseEntity<?> getMyAllReviews(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return ResponseEntity.ok(java.util.Collections.emptyList());
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(authentication.getName()).orElse(null);
        if (kh == null || kh.getTaiKhoan() == null) return ResponseEntity.ok(java.util.Collections.emptyList());

        List<DanhGia> myReviews = danhGiaRepo.findAll().stream()
                .filter(r -> r.getTaiKhoan() != null && r.getTaiKhoan().getId().equals(kh.getTaiKhoan().getId()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(myReviews);
    }

    @PatchMapping("/{id}/doi-mat-khau")
    @Transactional
    public ResponseEntity<?> adminChangePassword(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String matKhauMoi = body.get("matKhauMoi");

        if (matKhauMoi == null || matKhauMoi.trim().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu phải có ít nhất 6 ký tự"));
        }

        KhachHang kh = khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        TaiKhoan tk = kh.getTaiKhoan();
        if (tk == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Khách hàng này chưa có tài khoản đăng nhập (Chỉ mua tại quầy)"));
        }

        tk.setMatKhau(passwordEncoder.encode(matKhauMoi));
        taiKhoanRepository.save(tk);

        return ResponseEntity.ok(Map.of("message", "Cập nhật mật khẩu thành công"));
    }

    @GetMapping("/reviews/check-eligibility/{sanPhamId}")
    public ResponseEntity<?> checkReviewEligibility(@PathVariable Integer sanPhamId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.ok(Map.of("eligible", false, "message", "Vui lòng đăng nhập để viết đánh giá!"));
        }
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(authentication.getName()).orElse(null);
        if (kh == null || kh.getTaiKhoan() == null) {
            return ResponseEntity.ok(Map.of("eligible", false, "message", "Tài khoản không hợp lệ!"));
        }

        Integer userId = kh.getTaiKhoan().getId();

        List<HoaDon> myOrders = hoaDonRepository.findByKhachHangIdOrderByNgayTaoDesc(kh.getId());
        List<HoaDonChiTiet> purchasedDetails = new java.util.ArrayList<>();

        for (HoaDon hd : myOrders) {
            if ("HOAN_THANH".equals(hd.getTrangThaiHoaDon().getMa())) {
                List<HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findByHoaDonId(hd.getId());
                for (HoaDonChiTiet ct : chiTiets) {
                    if (ct.getSanPhamChiTiet().getSanPham().getId().equals(sanPhamId)) {
                        purchasedDetails.add(ct);
                    }
                }
            }
        }

        if (purchasedDetails.isEmpty()) {
            return ResponseEntity.ok(Map.of("eligible", false, "message", "Bạn cần mua và nhận sản phẩm này thành công để có thể đánh giá!"));
        }

        List<Integer> reviewedDetailIds = danhGiaRepo.findBySanPhamIdOrderByNgayTaoDesc(sanPhamId).stream()
                .filter(r -> r.getTaiKhoan() != null && r.getTaiKhoan().getId().equals(userId)
                        && r.getHoaDonChiTiet() != null && r.getHoaDonChiTiet().getId() != null)
                .map(r -> r.getHoaDonChiTiet().getId())
                .collect(java.util.stream.Collectors.toList());

        HoaDonChiTiet unreviewedDetail = null;
        for (HoaDonChiTiet ct : purchasedDetails) {
            if (!reviewedDetailIds.contains(ct.getId())) {
                unreviewedDetail = ct;
                break;
            }
        }

        if (unreviewedDetail == null) {
            return ResponseEntity.ok(Map.of("eligible", false, "message", "Bạn đã viết đánh giá cho tất cả các lượt mua của sản phẩm này rồi! Cảm ơn bạn rất nhiều."));
        }

        String phanLoai = unreviewedDetail.getSanPhamChiTiet().getMauSac().getTen() + " - " + unreviewedDetail.getSanPhamChiTiet().getKichThuoc().getTen();
        return ResponseEntity.ok(Map.of(
                "eligible", true,
                "orderDetailId", unreviewedDetail.getId(),
                "phanLoai", phanLoai
        ));
    }

    @PostMapping("/send-otp")
    @ResponseBody
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        try {
            String otp = String.format("%06d", new Random().nextInt(999999));
            otpStorageKhach.put(email, otp);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(email);
            message.setSubject("[BeeMate] MÃ XÁC THỰC OTP THAY ĐỔI EMAIL");
            message.setText("Chào bạn,\n\nBạn đang thực hiện thao tác thay đổi Email trên hệ thống BeeMate.\n"
                    + "Mã xác thực OTP của bạn là: " + otp + "\n\n"
                    + "Vui lòng nhập mã này vào hệ thống để hoàn tất.\n"
                    + "Trân trọng,\nBan Quản Trị Hệ Thống BeeMate.");
            mailSender.send(message);
            return ResponseEntity.ok("Đã gửi mã OTP thành công đến email: " + email);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi gửi Email: Mất mạng hoặc cấu hình sai!");
        }
    }

    @PostMapping("/verify-otp")
    @ResponseBody
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        String savedOtp = otpStorageKhach.get(email);
        if (savedOtp != null && savedOtp.equals(otp)) {
            otpStorageKhach.remove(email);
            return ResponseEntity.ok("Xác thực OTP thành công!");
        }
        return ResponseEntity.badRequest().body("Mã OTP không chính xác, vui lòng nhập lại!");
    }

    @GetMapping("/check-email")
    @ResponseBody
    public ResponseEntity<?> checkEmail(@RequestParam String email, @RequestParam(required = false) Integer id) {
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"))
            return ResponseEntity.badRequest().body("Email không đúng định dạng!");
        if (id == null) {
            if (khRepo.existsByEmail(email))
                return ResponseEntity.badRequest().body("Email này đã tồn tại trong hệ thống!");
        } else {
            if (khRepo.existsByEmailAndIdNot(email, id))
                return ResponseEntity.badRequest().body("Email này đã thuộc về tài khoản khác!");
        }
        return ResponseEntity.ok("Email hợp lệ");
    }


    private void mapRequestToEntity(DiaChiRequest req, DiaChiKhachHang dc) {
        dc.setHoTenNhan(req.getHoTenNhan());
        dc.setSdtNhan(req.getSdtNhan());
        dc.setDiaChiChiTiet(req.getDiaChiChiTiet());
        dc.setTinhThanhPho(req.getTinhThanhPho());
        dc.setMaTinh(req.getMaTinh());
        dc.setQuanHuyen(req.getQuanHuyen());
        dc.setMaHuyen(req.getMaHuyen());
        dc.setPhuongXa(req.getPhuongXa());
        dc.setMaXa(req.getMaXa());
        dc.setLaMacDinh(req.getLaMacDinh() != null ? req.getLaMacDinh() : false);
        dc.setTrangThai(true);
        dc.setLoaiDiaChi("Nhà riêng");
    }
}