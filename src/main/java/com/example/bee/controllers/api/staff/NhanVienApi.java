package com.example.bee.controllers.api.staff;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.account.VaiTro;
import com.example.bee.entities.role.ChucVu;
import com.example.bee.entities.user.NhanVien;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.role.NhanVienRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Controller
@RequestMapping("/api/nhan-vien")
@RequiredArgsConstructor
public class NhanVienApi {

    private static final Map<String, String> otpStorage = new HashMap<>();
    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @PostMapping("/send-otp")
    @ResponseBody
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        try {
            String otp = String.format("%06d", new Random().nextInt(999999));
            otpStorage.put(email, otp);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(email);
            message.setSubject("[BeeMate] MÃ XÁC THỰC OTP HỆ THỐNG");
            message.setText("Chào bạn,\n\nBạn đang thực hiện thao tác cập nhật hồ sơ trên hệ thống BeeMate.\n"
                    + "Mã xác thực OTP của bạn là: " + otp + "\n\n"
                    + "Vui lòng nhập mã này vào hệ thống để hoàn tất.\n"
                    + "Trân trọng,\nBan Quản Trị Hệ Thống BeeMate.");
            mailSender.send(message);
            return ResponseEntity.ok("Đã gửi mã OTP thành công đến email: " + email);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi gửi Email: Sai tài khoản cấu hình hoặc mất mạng!");
        }
    }

    @PostMapping("/verify-otp")
    @ResponseBody
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        String savedOtp = otpStorage.get(email);
        if (savedOtp != null && savedOtp.equals(otp)) {
            otpStorage.remove(email);
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
            if (nhanVienRepository.existsByEmail(email))
                return ResponseEntity.badRequest().body("Email này đã tồn tại trong hệ thống!");
        } else {
            if (nhanVienRepository.existsByEmailAndIdNot(email, id))
                return ResponseEntity.badRequest().body("Email này đã thuộc về tài khoản khác!");
        }
        return ResponseEntity.ok("Email hợp lệ");
    }

    @GetMapping("/check-phone")
    @ResponseBody
    public ResponseEntity<?> checkPhone(@RequestParam String phone, @RequestParam(required = false) Integer id) {
        if (!phone.matches("^0[0-9]{9}$"))
            return ResponseEntity.badRequest().body("SĐT phải đủ 10 số và bắt đầu bằng 0!");
        if (id == null) {
            if (nhanVienRepository.existsBySoDienThoai(phone))
                return ResponseEntity.badRequest().body("Số điện thoại này đã tồn tại trong hệ thống!");
        } else {
            if (nhanVienRepository.existsBySoDienThoaiAndIdNot(phone, id))
                return ResponseEntity.badRequest().body("Số điện thoại này đã thuộc về nhân viên khác!");
        }
        return ResponseEntity.ok("SĐT hợp lệ");
    }

    @GetMapping("/detail/{id}")
    @ResponseBody
    public ResponseEntity<NhanVien> getOne(@PathVariable Integer id) {
        NhanVien nv = nhanVienRepository.findByIdWithTaiKhoan(id).orElse(null);
        if (nv == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(nv);
    }

    @GetMapping("/list-json")
    @ResponseBody
    public List<NhanVien> getListJson(
            @RequestParam(value = "q", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "status", required = false) Boolean status) {
        return nhanVienRepository.searchNhanVien(keyword, status);
    }

    @PatchMapping("/toggle-status/{id}")
    @ResponseBody
    @Transactional // 🌟 Bắt buộc phải có Transactional
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id) {
        NhanVien nv = nhanVienRepository.findById(id).orElse(null);
        if (nv == null) return ResponseEntity.notFound().build();

        nv.setTrangThai(!nv.getTrangThai());

        // 🌟 ĐỒNG BỘ KHÓA LUÔN TÀI KHOẢN ĐĂNG NHẬP
        if (nv.getTaiKhoan() != null) {
            TaiKhoan tk = nv.getTaiKhoan();
            tk.setTrangThai(nv.getTrangThai());
            taiKhoanRepository.save(tk);
        }

        nhanVienRepository.save(nv);
        return ResponseEntity.ok(nv.getTrangThai() ? "Hồ sơ đã được kích hoạt!" : "Nhân viên đã bị khóa!");
    }

    @Transactional
    @PostMapping("/save")
    public ResponseEntity<?> save(
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "hoTen", required = true) String hoTen,
            @RequestParam(value = "soDienThoai", required = true) String soDienThoai,
            @RequestParam(value = "email", required = true) String email,
            @RequestParam(value = "ngaySinh", required = true) String ngaySinhStr,
            @RequestParam(value = "gioiTinh", required = false) String gioiTinh,
            @RequestParam(value = "diaChi", required = false) String diaChi,
            @RequestParam(value = "trangThai", required = false, defaultValue = "true") Boolean trangThai,
            @RequestParam(value = "hinhAnh", required = false) String hinhAnh,
            @RequestParam(value = "password", required = false) String pass,
            @RequestParam(value = "confirmPassword", required = false) String confirm) {
        try {
            if (hoTen == null || hoTen.trim().isEmpty()) return ResponseEntity.badRequest().body("Họ tên không được để trống!");
            if (soDienThoai == null || soDienThoai.isBlank()) return ResponseEntity.badRequest().body("Số điện thoại không được để trống!");
            if (!soDienThoai.matches("^0[0-9]{9}$")) return ResponseEntity.badRequest().body("SĐT phải đủ 10 số và bắt đầu bằng 0!");
            if (email == null || email.isBlank()) return ResponseEntity.badRequest().body("Email không được để trống!");
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) return ResponseEntity.badRequest().body("Email không đúng định dạng!");
            if (ngaySinhStr == null || ngaySinhStr.isBlank()) return ResponseEntity.badRequest().body("Ngày sinh không được để trống!");

            java.sql.Date ngaySinh;
            try {
                ngaySinh = java.sql.Date.valueOf(ngaySinhStr); // Chuyển chuỗi YYYY-MM-DD thành SQL Date
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Ngày sinh không hợp lệ!");
            }

            LocalDate hienTai = LocalDate.now();
            int tuoi = Period.between(ngaySinh.toLocalDate(), hienTai).getYears();
            if (tuoi < 15) return ResponseEntity.badRequest().body("Nhân viên phải từ đủ 15 tuổi trở lên!");

            NhanVien target;
            boolean isNew = (id == null);

            if (!isNew) {
                target = nhanVienRepository.findByIdWithTaiKhoan(id).orElseThrow(() -> new RuntimeException("Không tìm thấy NV"));
                if (target.getChucVu() != null && target.getChucVu().getId() == 1)
                    return ResponseEntity.badRequest().body("Thông tin Admin là bảo mật, không thể xem hoặc sửa!");
                if (nhanVienRepository.existsBySoDienThoaiAndIdNot(soDienThoai, id))
                    return ResponseEntity.badRequest().body("Số điện thoại này đã thuộc về nhân viên khác!");
                if (nhanVienRepository.existsByEmailAndIdNot(email, id))
                    return ResponseEntity.badRequest().body("Email/Tài khoản này đã tồn tại!");
            } else {
                if (nhanVienRepository.existsBySoDienThoai(soDienThoai)) return ResponseEntity.badRequest().body("Số điện thoại đã tồn tại!");
                if (nhanVienRepository.existsByEmail(email)) return ResponseEntity.badRequest().body("Email đã tồn tại!");

                target = new NhanVien();
                target.setMa("TEMP");
                ChucVu cv = new ChucVu();
                cv.setId(2);
                target.setChucVu(cv);
            }

            // Gắn tài khoản
            TaiKhoan tk = (target.getTaiKhoan() == null) ? new TaiKhoan() : target.getTaiKhoan();
            tk.setTenDangNhap(email);

            if (isNew) {
                tk.setMatKhau(passwordEncoder.encode("12345678"));
                tk.setTrangThai(true);
                VaiTro vt = new VaiTro();
                vt.setId(2); // ROLE_STAFF
                tk.setVaiTro(vt);
            } else {
                if (pass != null && !pass.trim().isEmpty()) {
                    if (!pass.equals(confirm)) return ResponseEntity.badRequest().body("Mật khẩu xác nhận không khớp!");
                    // Kiểm tra độ mạnh của mật khẩu
                    String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*])(?=.{8,}).*$";
                    if (!pass.matches(passwordRegex)) return ResponseEntity.badRequest().body("Mật khẩu phải từ 8 ký tự, gồm chữ hoa, thường, số và ký tự đặc biệt!");
                    tk.setMatKhau(passwordEncoder.encode(pass));
                }
            }

            TaiKhoan savedTk = taiKhoanRepository.save(tk);
            target.setTaiKhoan(savedTk);

            // Cập nhật thông tin NV
            target.setHoTen(hoTen);
            target.setSoDienThoai(soDienThoai);
            target.setEmail(email);
            target.setNgaySinh(ngaySinh);
            target.setGioiTinh(gioiTinh);
            if (diaChi != null) target.setDiaChi(diaChi);
            target.setTrangThai(trangThai);
            if (hinhAnh != null && !hinhAnh.trim().isEmpty()) target.setHinhAnh(hinhAnh);

            NhanVien saved = nhanVienRepository.save(target);
            if (isNew) {
                saved.setMa(String.format("NV%03d", saved.getId()));
                nhanVienRepository.save(saved);
            }
            return ResponseEntity.ok("Lưu hồ sơ thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "status", required = false) Boolean status,
            Model model) {
        List<NhanVien> ketQua = nhanVienRepository.searchNhanVien(keyword, status);
        model.addAttribute("list", ketQua);
        return "admin/staff/staff :: table_body";
    }

    @GetMapping("/test-login")
    @ResponseBody
    public String testLogin() {
        TaiKhoan admin = taiKhoanRepository.findById(1).orElse(null);
        if (admin == null) return "Không tìm thấy user admin01 trong DB!";
        String hashTrongDb = admin.getMatKhau();
        boolean isMatch = passwordEncoder.matches("123456", hashTrongDb);
        return "Mã Hash trong DB: " + hashTrongDb + "<br>Kết quả so sánh với '123456': " + (isMatch ? "KHỚP 100% (Mật khẩu đúng)" : "SAI BÉT (Mật khẩu sai)");
    }

    // ========================================================
    // 1. API LẤY THÔNG TIN HỒ SƠ
    // ========================================================
    @GetMapping("/my-profile")
    @ResponseBody
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        String username = authentication.getName();
        Optional<NhanVien> nvOpt = nhanVienRepository.findByTaiKhoan_TenDangNhap(username);

        if (nvOpt.isEmpty()) {
            TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(username).orElse(null);
            if (tk != null) {
                Map<String, Object> adminData = new HashMap<>();
                adminData.put("hoTen", "Quản trị viên hệ thống");
                adminData.put("tenDangNhap", tk.getTenDangNhap());
                // 🌟 Lấy trạng thái đã đổi tên từ DB lên
                adminData.put("daDoiTenDangNhap", tk.getDaDoiTenDangNhap() != null ? tk.getDaDoiTenDangNhap() : false);
                adminData.put("vaiTro", tk.getVaiTro() != null ? tk.getVaiTro().getTen() : "ADMIN");

                Map<String, Object> chucVuMap = new HashMap<>();
                chucVuMap.put("ten", "System Admin");
                adminData.put("chucVu", chucVuMap);

                return ResponseEntity.ok(adminData);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy hồ sơ"));
        }

        NhanVien nv = nvOpt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("ma", nv.getMa());
        data.put("hoTen", nv.getHoTen());
        data.put("hinhAnh", nv.getHinhAnh());
        data.put("gioiTinh", nv.getGioiTinh());
        data.put("ngaySinh", nv.getNgaySinh() != null ? nv.getNgaySinh().toString() : null);
        data.put("soDienThoai", nv.getSoDienThoai());
        data.put("email", nv.getEmail());
        data.put("diaChi", nv.getDiaChi());

        data.put("tenDangNhap", nv.getTaiKhoan() != null ? nv.getTaiKhoan().getTenDangNhap() : "");
        // 🌟 Lấy trạng thái đã đổi tên từ DB lên
        data.put("daDoiTenDangNhap", nv.getTaiKhoan() != null && Boolean.TRUE.equals(nv.getTaiKhoan().getDaDoiTenDangNhap()));

        data.put("vaiTro", nv.getTaiKhoan() != null && nv.getTaiKhoan().getVaiTro() != null ? nv.getTaiKhoan().getVaiTro().getMa() : "");

        Map<String, Object> chucVuMap = new HashMap<>();
        chucVuMap.put("ten", nv.getChucVu() != null ? nv.getChucVu().getTen() : "Nhân viên");
        data.put("chucVu", chucVuMap);
        data.put("trangThai", nv.getTrangThai());

        return ResponseEntity.ok(data);
    }

    // ========================================================
    // 2. API CẬP NHẬT THÔNG TIN VÀ ĐỔI TÊN ĐĂNG NHẬP
    // ========================================================
    @PutMapping("/my-profile")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
            }

            String username = authentication.getName();
            Optional<NhanVien> nvOpt = nhanVienRepository.findByTaiKhoan_TenDangNhap(username);
            TaiKhoan tk;

            if (nvOpt.isEmpty()) {
                tk = taiKhoanRepository.findByTenDangNhap(username).orElse(null);
                if(tk == null) return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản không hợp lệ"));
            } else {
                tk = nvOpt.get().getTaiKhoan();
            }

            // 🌟 LOGIC KHÓA TÊN ĐĂNG NHẬP DỰA TRÊN CỜ TRONG DB
            if (payload.containsKey("tenDangNhap") && tk != null) {
                String newUsername = payload.get("tenDangNhap").trim();
                String oldUsername = tk.getTenDangNhap();

                if (!newUsername.isEmpty() && !newUsername.equals(oldUsername)) {
                    // Nếu cờ = true -> chặn không cho lưu
                    if (Boolean.TRUE.equals(tk.getDaDoiTenDangNhap())) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Bạn chỉ được phép thay đổi tên đăng nhập 1 lần duy nhất!"));
                    }
                    if (taiKhoanRepository.findByTenDangNhap(newUsername).isPresent()) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Tên đăng nhập này đã có người sử dụng!"));
                    }

                    tk.setTenDangNhap(newUsername);
                    tk.setDaDoiTenDangNhap(true); // 🌟 BẬT CỜ ĐÃ ĐỔI TÊN
                    taiKhoanRepository.save(tk);
                }
            }

            // Chỉ cập nhật hồ sơ nếu là Nhân viên
            if (nvOpt.isPresent()) {
                NhanVien nv = nvOpt.get();
                if (payload.containsKey("hoTen") && !payload.get("hoTen").isEmpty()) nv.setHoTen(payload.get("hoTen"));
                if (payload.containsKey("soDienThoai") && !payload.get("soDienThoai").isEmpty()) nv.setSoDienThoai(payload.get("soDienThoai"));
                if (payload.containsKey("email") && !payload.get("email").isEmpty()) nv.setEmail(payload.get("email"));
                if (payload.containsKey("gioiTinh")) nv.setGioiTinh(payload.get("gioiTinh"));
                if (payload.containsKey("diaChi")) nv.setDiaChi(payload.get("diaChi"));
                if (payload.containsKey("hinhAnh")) nv.setHinhAnh(payload.get("hinhAnh"));

                if (payload.containsKey("ngaySinh") && payload.get("ngaySinh") != null && !payload.get("ngaySinh").isEmpty()) {
                    try { nv.setNgaySinh(java.sql.Date.valueOf(payload.get("ngaySinh"))); } catch (Exception e) { }
                }
                nhanVienRepository.save(nv);
            }

            return ResponseEntity.ok(Map.of("message", "Cập nhật hồ sơ cá nhân thành công"));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Lưu thất bại! Có lỗi xảy ra."));
        }
    }


    // ========================================================
    // 3. API ĐỔI MẬT KHẨU
    // ========================================================
    @PostMapping("/change-password")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> payload, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        String username = authentication.getName();
        TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(username).orElse(null);

        if (tk == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản không tồn tại"));
        }

        String oldPw = payload.get("oldPassword");
        String newPw = payload.get("newPassword");

        if (oldPw == null || newPw == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ"));
        }

        // Kiểm tra độ mạnh của mật khẩu bằng Regex
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*])(?=.{8,}).*$";

        if (!newPw.matches(passwordRegex)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt!"
            ));
        }

        if (!passwordEncoder.matches(oldPw, tk.getMatKhau())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Mật khẩu hiện tại không đúng"));
        }

        tk.setMatKhau(passwordEncoder.encode(newPw));
        taiKhoanRepository.save(tk);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu an toàn thành công"));
    }
}