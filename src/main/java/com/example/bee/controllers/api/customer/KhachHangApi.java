package com.example.bee.controllers.api.customer;

import com.example.bee.dto.DanhGiaRequest;
import com.example.bee.dto.DiaChiRequest;
import com.example.bee.dto.KhachHangRequest;
import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.customer.DiaChiKhachHang;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.order.HoaDonChiTiet;
import com.example.bee.entities.product.SanPhamYeuThich;
import com.example.bee.entities.reviews.DanhGia;
import com.example.bee.entities.user.NhanVien;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.customer.DiaChiKhachHangRepository;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.order.HoaDonChiTietRepository;
import com.example.bee.repositories.order.HoaDonRepository;
import com.example.bee.repositories.reviews.DanhGiaRepository;
import com.example.bee.repositories.role.NhanVienRepository;
import com.example.bee.repositories.wishlist.SanPhamYeuThichRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangApi {

    private static final SecureRandom RAND = new SecureRandom();
    private final KhachHangRepository khRepo;
    private final NhanVienRepository nvRepo;
    private final DiaChiKhachHangRepository dcRepo;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;
    private final SanPhamYeuThichRepository wishlistRepo;
    private final DanhGiaRepository danhGiaRepo;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final HoaDonRepository hoaDonRepository;

    private String generateMa() {
        long count = khRepo.count();
        String ma;
        do {
            count++;
            ma = String.format("KH%08d", count);

        } while (khRepo.existsByMaIgnoreCase(ma));
        return ma;
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
    public ResponseEntity<KhachHang> create(@RequestBody KhachHangRequest req) {
        String ten = req.getHoTen() != null ? req.getHoTen().trim() : "";
        if (ten.isEmpty() || ten.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ tên không được để trống và tối đa 100 ký tự");
        }
        String sdt = req.getSoDienThoai() != null ? req.getSoDienThoai().trim() : "";
        if (sdt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không được để trống");
        }
        if (!sdt.matches("^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không đúng định dạng");
        }
        if (khRepo.existsBySoDienThoai(sdt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại đã tồn tại trong hệ thống");
        }
        String email = req.getEmail() != null ? req.getEmail().trim() : "";
        if (!email.isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không đúng định dạng");
            }
            if (khRepo.existsByEmail(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại trong hệ thống");
            }
        }
        KhachHang kh = new KhachHang();
        kh.setMa(generateMa());
        kh.setHoTen(ten);
        kh.setGioiTinh(req.getGioiTinh() != null && req.getGioiTinh() ? "Nam" : "Nữ");
        kh.setNgaySinh(req.getNgaySinh());
        kh.setSoDienThoai(sdt);
        kh.setEmail(email.isEmpty() ? null : email);
        kh.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : true);
        if (req.getHinhAnh() != null) kh.setHinhAnh(req.getHinhAnh());
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
            dc.setLoaiDiaChi("Nhà riêng");
            dc.setLaMacDinh(true);
            dc.setTrangThai(true);
            dcRepo.save(dc);
        }
        return ResponseEntity.ok(savedKh);
    }

    @PutMapping("/{id}")
    public ResponseEntity<KhachHang> update(@PathVariable Integer id, @RequestBody KhachHangRequest req) {
        KhachHang kh = khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        if (req.getHoTen() != null) {
            String ten = req.getHoTen().trim();
            if (ten.isEmpty() || ten.length() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ tên không được để trống và tối đa 100 ký tự");
            }
            kh.setHoTen(ten);
        }
        if (req.getGioiTinh() != null) kh.setGioiTinh(req.getGioiTinh() ? "Nam" : "Nữ");
        if (req.getNgaySinh() != null) kh.setNgaySinh(req.getNgaySinh());
        if (req.getSoDienThoai() != null) {
            String sdtNew = req.getSoDienThoai().trim();
            if (sdtNew.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không được để trống");
            }
            if (!sdtNew.matches("^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không đúng định dạng");
            }
            if (khRepo.existsBySoDienThoaiAndIdNot(sdtNew, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng bởi khách hàng khác");
            }
            kh.setSoDienThoai(sdtNew);
        }
        if (req.getEmail() != null) {
            String emailNew = req.getEmail().trim();
            if (!emailNew.isEmpty()) {
                if (!emailNew.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không đúng định dạng");
                }
                if (khRepo.existsByEmailAndIdNot(emailNew, id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng bởi khách hàng khác");
                }
                kh.setEmail(emailNew);
            } else {
                kh.setEmail(null);
            }
        }
        if (req.getTrangThai() != null) kh.setTrangThai(req.getTrangThai());
        if (req.getHinhAnh() != null) kh.setHinhAnh(req.getHinhAnh());
        return ResponseEntity.ok(khRepo.save(kh));
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> quickToggleStatus(@PathVariable Integer id) {
        KhachHang kh = khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khách hàng không tồn tại"));
        kh.setTrangThai(!kh.getTrangThai());
        return ResponseEntity.ok(khRepo.save(kh));
    }

    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        // 1. Kiểm tra xem đã đăng nhập chưa
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        String username = authentication.getName();

        // 2. Tìm trong bảng Khách Hàng
        Optional<KhachHang> khOpt = khRepo.findByTaiKhoan_TenDangNhap(username);
        if (khOpt.isPresent()) {
            KhachHang kh = khOpt.get();
            return ResponseEntity.ok(Map.of(
                    "hoTen", kh.getHoTen() != null ? kh.getHoTen() : "Khách hàng",
                    "soDienThoai", kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "",
                    "email", kh.getEmail() != null ? kh.getEmail() : "",
                    "gioiTinh", kh.getGioiTinh() != null ? kh.getGioiTinh() : "",
                    "ngaySinh", kh.getNgaySinh() != null ? kh.getNgaySinh().toString() : "",
                    "hinhAnh", kh.getHinhAnh() != null ? kh.getHinhAnh() : ""
            ));
        }

        // 3. Tìm trong bảng Nhân Viên (nếu nhân viên đi mua hàng)
        Optional<NhanVien> nvOpt = nvRepo.findByTaiKhoan_TenDangNhap(username);
        if (nvOpt.isPresent()) {
            NhanVien nv = nvOpt.get();
            return ResponseEntity.ok(Map.of(
                    "hoTen", nv.getHoTen() != null ? nv.getHoTen() : "Nhân viên",
                    "soDienThoai", nv.getSoDienThoai() != null ? nv.getSoDienThoai() : "",
                    "email", nv.getEmail() != null ? nv.getEmail() : "",
                    "gioiTinh", nv.getGioiTinh() != null ? nv.getGioiTinh() : "",
                    "ngaySinh", nv.getNgaySinh() != null ? nv.getNgaySinh().toString() : "",
                    "hinhAnh", nv.getHinhAnh() != null ? nv.getHinhAnh() : ""
            ));
        }

        // 🌟 4. CHỮA CHÁY LỖI 404 TẠI ĐÂY:
        // Nếu tài khoản chưa có hồ sơ (Bị lỗi đăng ký), trả về thông tin mặc định thay vì báo lỗi!
        return ResponseEntity.ok(Map.of(
                "hoTen", "Người dùng mới",
                "soDienThoai", username,
                "email", "Chưa cập nhật",
                "gioiTinh", "",
                "ngaySinh", "",
                "hinhAnh", ""
        ));
    }

    @PutMapping("/my-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        String username = authentication.getName();

        // 1. Tìm Tài khoản gốc trước để lát nữa móc nối
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(username)
                .orElse(null);

        if (taiKhoan == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Không tìm thấy tài khoản hợp lệ!"));
        }

        // ==========================================
        // ƯU TIÊN 1: NẾU LÀ NHÂN VIÊN ĐANG ĐĂNG NHẬP
        // ==========================================
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

            // 👉 THÊM ĐOẠN NÀY ĐỂ LƯU LINK ẢNH CLOUDINARY
            if (payload.get("hinhAnh") != null && !payload.get("hinhAnh").trim().isEmpty()) {
                nv.setHinhAnh(payload.get("hinhAnh"));
            }

            nvRepo.save(nv);
            return ResponseEntity.ok(Map.of("message", "Cập nhật hồ sơ nhân viên thành công!"));
        }

        // ==========================================
        // ƯU TIÊN 2: NẾU LÀ KHÁCH HÀNG (Tìm thấy hoặc Tạo mới)
        // ==========================================
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

        // 👉 THÊM ĐOẠN NÀY ĐỂ LƯU LINK ẢNH CLOUDINARY
        if (payload.get("hinhAnh") != null && !payload.get("hinhAnh").trim().isEmpty()) {
            kh.setHinhAnh(payload.get("hinhAnh"));
        }

        khRepo.save(kh);

        // ==========================================
        // TÍNH NĂNG SMART LOCK: CHO PHÉP ĐỔI TÊN ĐĂNG NHẬP 1 LẦN
        // ==========================================
        String newUsername = payload.get("tenDangNhap");

        if (newUsername != null && !newUsername.trim().isEmpty() && !newUsername.equals(taiKhoan.getTenDangNhap())) {
            if (taiKhoanRepository.existsByTenDangNhap(newUsername)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Tên đăng nhập này đã có người sử dụng!"));
            }
            taiKhoan.setTenDangNhap(newUsername);
            taiKhoanRepository.save(taiKhoan);
        }

        return ResponseEntity.ok(Map.of("message", "Cập nhật thành công!"));
    }

    @GetMapping("/{id}/dia-chi")
    public ResponseEntity<?> getAddressList(@PathVariable Integer id) {
        if (!khRepo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok(dcRepo.findByKhachHangId(id));
    }

    @PostMapping("/{id}/dia-chi")
    public ResponseEntity<?> addAddress(@PathVariable Integer id, @RequestBody DiaChiRequest req) {
        KhachHang kh = khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        DiaChiKhachHang dc = new DiaChiKhachHang();
        dc.setKhachHang(kh);
        dc.setDiaChiChiTiet(req.getDiaChiChiTiet());
        dc.setPhuongXa(req.getPhuongXa());
        dc.setQuanHuyen(req.getQuanHuyen());
        dc.setTinhThanhPho(req.getTinhThanhPho());
        dc.setHoTenNhan(req.getHoTenNhan() != null ? req.getHoTenNhan() : kh.getHoTen());
        dc.setSdtNhan(req.getSdtNhan() != null ? req.getSdtNhan() : kh.getSoDienThoai());
        dc.setLoaiDiaChi("Khác");
        dc.setLaMacDinh(false);
        dc.setTrangThai(true);
        return ResponseEntity.ok(dcRepo.save(dc));
    }

    @DeleteMapping("/dia-chi/{idDiaChi}")
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

    // 1. Lấy danh sách địa chỉ của chính khách hàng đang đăng nhập
    @GetMapping("/addresses")
    public ResponseEntity<?> getMyAddresses(Authentication authentication) {
        String username = authentication.getName();
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        return ResponseEntity.ok(dcRepo.findByKhachHangId(kh.getId()));
    }

    // 2. Lấy địa chỉ mặc định của khách hàng đang đăng nhập
    @GetMapping("/addresses/default")
    public ResponseEntity<?> getDefaultAddress(Authentication authentication) {
        String username = authentication.getName();
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return ResponseEntity.ok(dcRepo.findByKhachHangIdAndLaMacDinhTrue(kh.getId()).orElse(null));
    }

    // 3. Thêm mới địa chỉ (Có hỗ trợ mã GHN)
    @PostMapping("/addresses")
    @Transactional
    public ResponseEntity<?> addMyAddress(@RequestBody DiaChiRequest req, Authentication authentication) {
        String username = authentication.getName();
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(username).orElseThrow();

        // Nếu đặt là mặc định, bỏ mặc định cũ
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

    // 4. Cập nhật địa chỉ (Có hỗ trợ mã GHN)
    @PutMapping("/addresses/{id}")
    @Transactional
    public ResponseEntity<?> updateMyAddress(@PathVariable Integer id, @RequestBody DiaChiRequest req, Authentication authentication) {
        DiaChiKhachHang dc = dcRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Kiểm tra bảo mật: địa chỉ này phải thuộc về user đang login
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

    // 5. API thiết lập mặc định (Dùng cho nút bấm ngoài danh sách)
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

        // Kiểm tra sở hữu
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

        boolean isSaved = wishlistRepo.findByTaiKhoanIdAndSanPhamId(kh.getTaiKhoan().getId(), sanPhamId).isPresent();
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
        var exist = wishlistRepo.findByTaiKhoanIdAndSanPhamId(userId, sanPhamId);

        if (exist.isPresent()) {
            wishlistRepo.delete(exist.get());
            return ResponseEntity.ok(Map.of("message", "Đã bỏ lưu sản phẩm khỏi danh sách yêu thích", "status", "removed"));
        } else {
            SanPhamYeuThich wl = new SanPhamYeuThich();
            wl.setTaiKhoanId(userId);
            wl.setSanPhamId(sanPhamId);
            wishlistRepo.save(wl);
            return ResponseEntity.ok(Map.of("message", "Đã lưu vào danh sách yêu thích! ❤️", "status", "added"));
        }
    }

    @GetMapping("/reviews/{sanPhamId}")
    public ResponseEntity<?> getReviews(@PathVariable Integer sanPhamId) {
        // Lấy danh sách đánh giá của sản phẩm
        List<DanhGia> list = danhGiaRepo.findBySanPhamIdOrderByNgayTaoDesc(sanPhamId);

        // Lấy danh sách khách hàng để map (dịch) tên
        List<KhachHang> allKhachHang = khRepo.findAll();

        for (DanhGia dg : list) {
            String tenNguoiDanhGia = "Khách hàng ẩn danh"; // Tên mặc định nếu không tìm thấy

            // Quét tìm Tên của khách hàng khớp với ID người đánh giá
            for (KhachHang kh : allKhachHang) {
                if (kh.getTaiKhoan() != null && kh.getTaiKhoan().getId().equals(dg.getTaiKhoanId())) {
                    if (kh.getHoTen() != null && !kh.getHoTen().trim().isEmpty()) {
                        tenNguoiDanhGia = kh.getHoTen();

                        /* (TÙY CHỌN BẢO MẬT):
                           Nếu bạn muốn che bớt tên giống Shopee (VD: Bùi Cao Minh -> B*** Minh)
                           thì hãy xóa dấu // ở 4 dòng code bên dưới: */

                        // String[] parts = tenNguoiDanhGia.split(" ");
                        // if (parts.length > 1) {
                        //     tenNguoiDanhGia = parts[0].charAt(0) + "*** " + parts[parts.length - 1];
                        // }
                    }
                    break;
                }
            }
            dg.setTenKhachHang(tenNguoiDanhGia); // Gắn tên thật vào để gửi về Frontend
        }

        return ResponseEntity.ok(list);
    }

    // 2. GHI ĐÈ: CẬP NHẬT LẠI API THÊM ĐÁNH GIÁ ĐỂ HỖ TRỢ "SỬA 1 LẦN"
    // 🌟 API ĐƯỢC SỬA LẠI: Lấy orderDetailId thay vì productId
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

        Integer sanPhamId = hdct.getSanPhamChiTiet().getSanPham().getId();

        Optional<DanhGia> existingOpt = danhGiaRepo.findAll().stream()
                .filter(d -> d.getHoaDonChiTietId() != null && d.getHoaDonChiTietId().equals(orderDetailId))
                .findFirst();

        DanhGia danhGia;
        if (existingOpt.isPresent()) {
            danhGia = existingOpt.get();

            // 🌟 CHỐT CHẶN: Nếu đã sửa 1 lần rồi thì báo lỗi ngay lập tức
            if (Boolean.TRUE.equals(danhGia.getDaSua())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Bạn chỉ được phép sửa đánh giá 1 lần duy nhất!"));
            }

            // Đánh dấu là đã dùng quyền sửa
            danhGia.setDaSua(true);
        } else {
            danhGia = new DanhGia();
            danhGia.setDaSua(false); // Tạo mới thì chưa bị tính là sửa
            danhGia.setNgayTao(java.time.LocalDateTime.now());
        }

        danhGia.setHoaDonChiTietId(orderDetailId);
        danhGia.setSanPhamId(sanPhamId);
        danhGia.setTaiKhoanId(tk.getId());
        danhGia.setSoSao(req.getSoSao());
        danhGia.setNoiDung(req.getNoiDung());
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

        // Lấy danh sách Sản phẩm yêu thích từ Database
        List<SanPhamYeuThich> list = wishlistRepo.findAll().stream()
                .filter(w -> w.getTaiKhoanId().equals(kh.getTaiKhoan().getId()))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(list);
    }

    // 1. THÊM MỚI: API LẤY TẤT CẢ ĐÁNH GIÁ CỦA TÀI KHOẢN ĐANG ĐĂNG NHẬP
    @GetMapping("/my-reviews")
    public ResponseEntity<?> getMyAllReviews(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.ok(java.util.Collections.emptyList());
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(authentication.getName()).orElse(null);
        if (kh == null || kh.getTaiKhoan() == null) return ResponseEntity.ok(java.util.Collections.emptyList());

        // Lọc ra các đánh giá của riêng user này
        List<DanhGia> myReviews = danhGiaRepo.findAll().stream()
                .filter(r -> r.getTaiKhoanId().equals(kh.getTaiKhoan().getId()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(myReviews);
    }

    // =================================================================
    // API DÀNH RIÊNG CHO TRANG CHI TIẾT SẢN PHẨM (detail.html)
    // =================================================================
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

        // 1. TÌM CÁC LƯỢT MUA THÀNH CÔNG SẢN PHẨM NÀY CỦA KHÁCH HÀNG
        List<com.example.bee.entities.order.HoaDon> myOrders = hoaDonRepository.findByKhachHangIdOrderByNgayTaoDesc(kh.getId());
        List<com.example.bee.entities.order.HoaDonChiTiet> purchasedDetails = new java.util.ArrayList<>();

        for (com.example.bee.entities.order.HoaDon hd : myOrders) {
            if ("HOAN_THANH".equals(hd.getTrangThaiHoaDon().getMa())) {
                List<com.example.bee.entities.order.HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findByHoaDonId(hd.getId());
                for (com.example.bee.entities.order.HoaDonChiTiet ct : chiTiets) {
                    if (ct.getSanPhamChiTiet().getSanPham().getId().equals(sanPhamId)) {
                        purchasedDetails.add(ct);
                    }
                }
            }
        }

        if (purchasedDetails.isEmpty()) {
            return ResponseEntity.ok(Map.of("eligible", false, "message", "Bạn cần mua và nhận sản phẩm này thành công để có thể đánh giá!"));
        }

        // 2. TÌM CÁC LƯỢT MUA ĐÃ BỊ ĐÁNH GIÁ (Để loại trừ)
        List<Integer> reviewedDetailIds = danhGiaRepo.findBySanPhamIdOrderByNgayTaoDesc(sanPhamId).stream()
                .filter(r -> r.getTaiKhoanId().equals(userId) && r.getHoaDonChiTietId() != null)
                .map(DanhGia::getHoaDonChiTietId)
                .collect(java.util.stream.Collectors.toList());

        // 3. TÌM 1 LƯỢT MUA CHƯA ĐƯỢC ĐÁNH GIÁ ĐỂ CẤP QUYỀN
        com.example.bee.entities.order.HoaDonChiTiet unreviewedDetail = null;
        for (com.example.bee.entities.order.HoaDonChiTiet ct : purchasedDetails) {
            if (!reviewedDetailIds.contains(ct.getId())) {
                unreviewedDetail = ct;
                break;
            }
        }

        if (unreviewedDetail == null) {
            return ResponseEntity.ok(Map.of("eligible", false, "message", "Bạn đã viết đánh giá cho tất cả các lượt mua của sản phẩm này rồi! Cảm ơn bạn rất nhiều."));
        }

        // 4. TRẢ VỀ ID CHI TIẾT ĐỂ FRONTEND MỞ MODAL
        String phanLoai = unreviewedDetail.getSanPhamChiTiet().getMauSac().getTen() + " - " + unreviewedDetail.getSanPhamChiTiet().getKichThuoc().getTen();
        return ResponseEntity.ok(Map.of(
                "eligible", true,
                "orderDetailId", unreviewedDetail.getId(),
                "phanLoai", phanLoai
        ));
    }



    // Hàm phụ để map dữ liệu
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