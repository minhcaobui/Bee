package com.example.bee.controllers.api.staff;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.staff.CV;
import com.example.bee.entities.staff.NV;
import com.example.bee.entities.staff.TK;
import com.example.bee.entities.staff.VT;
import com.example.bee.entities.user.NhanVien;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.role.NhanVienRepository;
import com.example.bee.repositories.staff.NVRepository;
import com.example.bee.repositories.staff.TKRepository;
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

import java.util.*;

@Controller
@RequestMapping("/api/admin/nhan-vien")
@RequiredArgsConstructor
public class NhanVienApi {

    private static final Map<String, String> otpStorage = new HashMap<>();
    private final NVRepository nvRepo;
    private final TKRepository tkRepo;
    private final TaiKhoanRepository taiKhoanRepo;
    private final NhanVienRepository nhanVienRepository;
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
            mailSender.send(message); // Bắn mail đi
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
            if (nvRepo.existsByEmail(email))
                return ResponseEntity.badRequest().body("Email này đã tồn tại trong hệ thống!");
        } else {
            if (nvRepo.existsByEmailAndIdNot(email, id))
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
            if (nvRepo.existsBySoDienThoai(phone))
                return ResponseEntity.badRequest().body("Số điện thoại này đã tồn tại trong hệ thống!");
        } else {
            if (nvRepo.existsBySoDienThoaiAndIdNot(phone, id))
                return ResponseEntity.badRequest().body("Số điện thoại này đã thuộc về nhân viên khác!");
        }
        return ResponseEntity.ok("SĐT hợp lệ");
    }


    @GetMapping("/detail/{id}")
    @ResponseBody
    public ResponseEntity<NV> getOne(@PathVariable Integer id) {
        NV nv = nvRepo.findByIdWithTaiKhoan(id).orElse(null);
        if (nv == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(nv);
    }

    @GetMapping("/list-json")
    @ResponseBody
    public List<NV> getListJson(
            @RequestParam(value = "q", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "status", required = false) Boolean status) {
        return nvRepo.searchNhanVien(keyword, status);
    }

    @PatchMapping("/toggle-status/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id) {
        NV nv = nvRepo.findById(id).orElse(null);
        if (nv == null) return ResponseEntity.notFound().build();
        nv.setTrangThai(!nv.getTrangThai());
        nvRepo.save(nv);
        return ResponseEntity.ok(nv.getTrangThai() ? "Hồ sơ đã được kích hoạt!" : "Nhân viên đã tạm nghỉ việc!");
    }

    @Transactional
    @PostMapping("/save")
    public ResponseEntity<?> save(@ModelAttribute NV nv,
                                  @RequestParam(value = "fileAnh", required = false) MultipartFile file,
                                  @RequestParam(value = "password", required = false) String pass,
                                  @RequestParam(value = "confirmPassword", required = false) String confirm) {
        try {
            if (nv.getHoTen() == null || nv.getHoTen().trim().isEmpty())
                return ResponseEntity.badRequest().body("Họ tên không được để trống!");
            if (nv.getSoDienThoai() == null || nv.getSoDienThoai().isBlank())
                return ResponseEntity.badRequest().body("Số điện thoại không được để trống!");

            if (!nv.getSoDienThoai().matches("^0[0-9]{9}$"))
                return ResponseEntity.badRequest().body("SĐT phải đủ 10 số và bắt đầu bằng 0!");
            if (nv.getEmail() == null || nv.getEmail().isBlank())
                return ResponseEntity.badRequest().body("Email không được để trống!");
            if (!nv.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"))
                return ResponseEntity.badRequest().body("Email không đúng định dạng!");
            if (nv.getNgaySinh() == null) return ResponseEntity.badRequest().body("Ngày sinh không được để trống!");
            java.time.LocalDate ngaySinh = nv.getNgaySinh();
            java.time.LocalDate hienTai = java.time.LocalDate.now();
            int tuoi = java.time.Period.between(ngaySinh, hienTai).getYears();
            if (tuoi < 15) return ResponseEntity.badRequest().body("Nhân viên phải từ đủ 15 tuổi trở lên!");
            NV target;
            boolean isNew = (nv.getId() == null);
            if (!isNew) {
                target = nvRepo.findByIdWithTaiKhoan(nv.getId()).orElseThrow();
                if (target.getChucVu() != null && target.getChucVu().getId() == 1)
                    return ResponseEntity.badRequest().body("Thông tin Admin là bảo mật, không thể xem hoặc sửa!");
                if (nvRepo.existsBySoDienThoaiAndIdNot(nv.getSoDienThoai(), nv.getId()))
                    return ResponseEntity.badRequest().body("Số điện thoại này đã thuộc về nhân viên khác!");
                if (nvRepo.existsByEmailAndIdNot(nv.getEmail(), nv.getId()))
                    return ResponseEntity.badRequest().body("Email/Tài khoản này đã tồn tại!");
            } else {
                if (nvRepo.existsBySoDienThoai(nv.getSoDienThoai()))
                    return ResponseEntity.badRequest().body("Số điện thoại đã tồn tại!");
                if (nvRepo.existsByEmail(nv.getEmail())) return ResponseEntity.badRequest().body("Email đã tồn tại!");
                target = new NV();
                target.setMa("TEMP");
                target.setTrangThai(true);
                CV cv = new CV();
                cv.setId(2);
                target.setChucVu(cv);
            }
            TK tk = (target.getTaiKhoan() == null) ? new TK() : target.getTaiKhoan();
            tk.setTenDangNhap(nv.getEmail());
            if (isNew) {
                tk.setMatKhau(passwordEncoder.encode("12345678"));
                tk.setTrangThai(true);
                VT vt = new VT();
                vt.setId(2);
                tk.setVaiTro(vt);
            } else {
                if (pass != null && !pass.trim().isEmpty()) {
                    if (!pass.equals(confirm)) return ResponseEntity.badRequest().body("Mật khẩu xác nhận không khớp!");
                    tk.setMatKhau(passwordEncoder.encode(pass));
                }
            }
            TK savedTk = tkRepo.save(tk);
            target.setTaiKhoan(savedTk);
            target.setHoTen(nv.getHoTen());
            target.setSoDienThoai(nv.getSoDienThoai());
            target.setEmail(nv.getEmail());
            target.setNgaySinh(nv.getNgaySinh());
            target.setGioiTinh(nv.getGioiTinh());
            if (nv.getDiaChi() != null) target.setDiaChi(nv.getDiaChi());
            target.setTrangThai(nv.getTrangThai());
            if (isNew) {
                target.setTrangThai(true);
            }
            if (nv.getHinhAnh() != null && !nv.getHinhAnh().trim().isEmpty()) {
                target.setHinhAnh(nv.getHinhAnh());
            }
            NV saved = nvRepo.save(target);
            if (isNew) {
                saved.setMa(String.format("NV%03d", saved.getId()));
                nvRepo.save(saved);
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
        List<NV> ketQua = nvRepo.searchNhanVien(keyword, status);
        model.addAttribute("list", ketQua);
        return "admin/staff/staff :: table_body";
    }

    @GetMapping("/test-login")
    @ResponseBody
    public String testLogin() {
        TK admin = tkRepo.findById(1).orElse(null);
        if (admin == null) return "Không tìm thấy user admin01 trong DB!";
        String hashTrongDb = admin.getMatKhau();
        boolean isMatch = passwordEncoder.matches("123456", hashTrongDb);
        return "Mã Hash trong DB: " + hashTrongDb + "<br>Kết quả so sánh với '123456': " + (isMatch ? "KHỚP 100% (Mật khẩu đúng)" : "SAI BÉT (Mật khẩu sai)");
    }

    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        String username = authentication.getName();
        Optional<NhanVien> nvOpt = nhanVienRepository.findByTaiKhoan_TenDangNhap(username);

        if (nvOpt.isEmpty()) {
            // Nếu là Admin tối cao (Không nằm trong bảng Nhân viên)
            TaiKhoan tk = taiKhoanRepo.findByTenDangNhap(username).orElse(null);
            if (tk != null) {
                Map<String, Object> adminData = new HashMap<>();
                adminData.put("hoTen", "Quản trị viên hệ thống");
                adminData.put("tenDangNhap", tk.getTenDangNhap());
                adminData.put("vaiTro", tk.getVaiTro() != null ? tk.getVaiTro().getTen() : "ADMIN");
                adminData.put("chucVu", "System Admin");
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
        data.put("vaiTro", nv.getTaiKhoan() != null && nv.getTaiKhoan().getVaiTro() != null ? nv.getTaiKhoan().getVaiTro().getMa() : "");
        data.put("chucVu", nv.getChucVu() != null ? nv.getChucVu().getTen() : "Nhân viên");
        data.put("trangThai", nv.getTrangThai());

        return ResponseEntity.ok(data);
    }

    // ========================================================
    // 2. API CẬP NHẬT THÔNG TIN CÁ NHÂN (VÀ ẢNH ĐẠI DIỆN)
    // ========================================================
    @PutMapping("/my-profile")
    @Transactional
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        String username = authentication.getName();
        Optional<NhanVien> nvOpt = nhanVienRepository.findByTaiKhoan_TenDangNhap(username);

        if (nvOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy hồ sơ nhân viên để cập nhật"));
        }

        NhanVien nv = nvOpt.get();

        // Cập nhật các trường được gửi lên
        if (payload.containsKey("hoTen")) nv.setHoTen(payload.get("hoTen"));
        if (payload.containsKey("soDienThoai")) nv.setSoDienThoai(payload.get("soDienThoai"));
        if (payload.containsKey("email")) nv.setEmail(payload.get("email"));
        if (payload.containsKey("gioiTinh")) nv.setGioiTinh(payload.get("gioiTinh"));
        if (payload.containsKey("diaChi")) nv.setDiaChi(payload.get("diaChi"));
        if (payload.containsKey("hinhAnh")) nv.setHinhAnh(payload.get("hinhAnh")); // Lưu URL ảnh Cloudinary

        // Xử lý ngày sinh
        if (payload.containsKey("ngaySinh") && payload.get("ngaySinh") != null && !payload.get("ngaySinh").isEmpty()) {
            try {
                nv.setNgaySinh(java.sql.Date.valueOf(payload.get("ngaySinh"))); // Format: yyyy-MM-dd
            } catch (Exception e) {
                // Bỏ qua nếu lỗi format ngày
            }
        }

        nhanVienRepository.save(nv);
        return ResponseEntity.ok(Map.of("message", "Cập nhật hồ sơ thành công"));
    }

    // ========================================================
    // 3. API ĐỔI MẬT KHẨU
    // ========================================================
    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> payload, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        String username = authentication.getName();
        TaiKhoan tk = taiKhoanRepo.findByTenDangNhap(username).orElse(null);

        if (tk == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản không tồn tại"));
        }

        String oldPw = payload.get("oldPassword");
        String newPw = payload.get("newPassword");

        if (oldPw == null || newPw == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ"));
        }

        // Kiểm tra mật khẩu cũ có khớp trong Database không
        if (!passwordEncoder.matches(oldPw, tk.getMatKhau())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Mật khẩu hiện tại không đúng"));
        }

        // Mã hóa mật khẩu mới và lưu
        tk.setMatKhau(passwordEncoder.encode(newPw));
        taiKhoanRepo.save(tk);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }
}