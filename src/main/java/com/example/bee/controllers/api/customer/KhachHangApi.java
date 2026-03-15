package com.example.bee.controllers.api.customer;

import com.example.bee.dto.DiaChiRequest;
import com.example.bee.dto.KhachHangRequest;
import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.customer.DiaChiKhachHang;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.customer.DiaChiKhachHangRepository;
import com.example.bee.repositories.customer.KhachHangRepository;
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

import java.security.Principal;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangApi {

    private static final SecureRandom RAND = new SecureRandom();
    private final KhachHangRepository khRepo;
    private final DiaChiKhachHangRepository dcRepo;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;

    private String generateMa() {
        String ma;
        do {
            StringBuilder sb = new StringBuilder("KH");
            for (int i = 0; i < 6; i++) sb.append(RAND.nextInt(10));
            ma = sb.toString();
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
    public ResponseEntity<?> getMyProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String username = principal.getName();
        Optional<KhachHang> kh = khRepo.findByTaiKhoan_TenDangNhap(username);
        return kh.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/my-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        KhachHang kh = khRepo.findByTaiKhoan_TenDangNhap(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy"));
        kh.setHoTen(request.get("hoTen"));
        kh.setSoDienThoai(request.get("soDienThoai"));
        kh.setGioiTinh(request.get("gioiTinh"));
        kh.setEmail(request.get("email"));
        try {
            if (request.get("ngaySinh") != null && !request.get("ngaySinh").isEmpty()) {
                kh.setNgaySinh(java.time.LocalDate.parse(request.get("ngaySinh")));
            }
        } catch (Exception e) {
            System.out.println("Lỗi parse ngày sinh: " + e.getMessage());
        }
        String newUsername = request.get("tenDangNhap");
        if (newUsername != null && !newUsername.trim().isEmpty()) {
            if (kh.getTaiKhoan() != null && (kh.getTaiKhoan().getTenDangNhap() == null || kh.getTaiKhoan().getTenDangNhap().isEmpty() || kh.getTaiKhoan().getTenDangNhap().contains("pos_"))) {
                if (taiKhoanRepository.existsByTenDangNhap(newUsername)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Tên đăng nhập này đã có người sử dụng!"));
                }
                kh.getTaiKhoan().setTenDangNhap(newUsername);
                taiKhoanRepository.save(kh.getTaiKhoan());
            }
        }
        khRepo.save(kh);
        return ResponseEntity.ok(Map.of("message", "Cập nhật thành công"));
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
}