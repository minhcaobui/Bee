package com.example.bee.services;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.account.VaiTro;
import com.example.bee.entities.cart.GioHang;
import com.example.bee.entities.staff.ChucVu;
import com.example.bee.entities.staff.NhanVien;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.cart.GioHangRepository;
import com.example.bee.repositories.staff.ChucVuRepository;
import com.example.bee.repositories.staff.NhanVienRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NhanVienService {

    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;
    private final GioHangRepository gioHangRepository;
    private final ChucVuRepository chucVuRepository;

    public ResponseEntity<?> kiemTraEmail(String email, Integer id) {
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

    public ResponseEntity<?> kiemTraSdt(String phone, Integer id) {
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

    public ResponseEntity<NhanVien> layChiTiet(Integer id) {
        NhanVien nv = nhanVienRepository.findByIdWithTaiKhoan(id).orElse(null);
        if (nv == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(nv);
    }

    public List<NhanVien> layDanhSachJson(String keyword, Boolean status) {
        return nhanVienRepository.searchNhanVien(keyword, status);
    }

    @Transactional
    public ResponseEntity<?> doiTrangThai(Integer id) {
        NhanVien nv = nhanVienRepository.findById(id).orElse(null);
        if (nv == null) return ResponseEntity.notFound().build();

        nv.setTrangThai(!nv.getTrangThai());
        if (nv.getTaiKhoan() != null) {
            TaiKhoan tk = nv.getTaiKhoan();
            tk.setTrangThai(nv.getTrangThai());
            taiKhoanRepository.save(tk);
        }

        nhanVienRepository.save(nv);
        return ResponseEntity.ok(nv.getTrangThai() ? "Hồ sơ đã được kích hoạt!" : "Nhân viên đã bị khóa!");
    }

    @Transactional
    public ResponseEntity<?> luuHoSo(Integer id, String hoTen, String soDienThoai, String email, String ngaySinhStr,
                                     String gioiTinh, String diaChi, Integer chucVuId, Boolean trangThai,
                                     String hinhAnh, String pass, String confirm) {
        try {
            if (hoTen == null || hoTen.trim().isEmpty()) return ResponseEntity.badRequest().body("Họ tên không được để trống!");
            if (soDienThoai == null || soDienThoai.isBlank()) return ResponseEntity.badRequest().body("Số điện thoại không được để trống!");
            if (!soDienThoai.matches("^0[0-9]{9}$")) return ResponseEntity.badRequest().body("SĐT phải đủ 10 số và bắt đầu bằng 0!");
            if (email == null || email.isBlank()) return ResponseEntity.badRequest().body("Email không được để trống!");
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) return ResponseEntity.badRequest().body("Email không đúng định dạng!");
            if (ngaySinhStr == null || ngaySinhStr.isBlank()) return ResponseEntity.badRequest().body("Ngày sinh không được để trống!");

            java.sql.Date ngaySinh;
            try {
                ngaySinh = java.sql.Date.valueOf(ngaySinhStr);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Ngày sinh không hợp lệ!");
            }

            LocalDate hienTai = LocalDate.now();
            int tuoi = Period.between(ngaySinh.toLocalDate(), hienTai).getYears();
            if (tuoi < 15) return ResponseEntity.badRequest().body("Nhân viên phải từ đủ 15 tuổi trở lên!");

            ChucVu cv = chucVuRepository.findById(chucVuId)
                    .orElseThrow(() -> new RuntimeException("Chức vụ được chọn không tồn tại!"));

            NhanVien target;
            boolean isNew = (id == null);

            if (!isNew) {
                target = nhanVienRepository.findByIdWithTaiKhoan(id).orElseThrow(() -> new RuntimeException("Không tìm thấy NV"));
                if (target.getChucVu() != null && target.getChucVu().getId() == 1 && chucVuId != 1) {
                    return ResponseEntity.badRequest().body("Không thể giáng chức của Admin tối cao!");
                }
                if (nhanVienRepository.existsBySoDienThoaiAndIdNot(soDienThoai, id))
                    return ResponseEntity.badRequest().body("Số điện thoại này đã thuộc về nhân viên khác!");

                TaiKhoan tkCheck = taiKhoanRepository.findByTenDangNhap(email).orElse(null);
                if (tkCheck != null && (target.getTaiKhoan() == null || !tkCheck.getId().equals(target.getTaiKhoan().getId()))) {
                    return ResponseEntity.badRequest().body("Email này đã được đăng ký cho một tài khoản khác (Nhân viên/Khách hàng)!");
                }
            } else {
                if (nhanVienRepository.existsBySoDienThoai(soDienThoai))
                    return ResponseEntity.badRequest().body("Số điện thoại đã tồn tại!");
                if (taiKhoanRepository.existsByTenDangNhap(email))
                    return ResponseEntity.badRequest().body("Email này đã được đăng ký tài khoản!");

                target = new NhanVien();
                target.setMa("TEMP");
            }

            TaiKhoan tk = (target.getTaiKhoan() == null) ? new TaiKhoan() : target.getTaiKhoan();
            tk.setTenDangNhap(email);

            if (isNew) {
                tk.setMatKhau(passwordEncoder.encode("12345678"));
                tk.setTrangThai(true);
                VaiTro vt = new VaiTro();
                vt.setId(2);
                tk.setVaiTro(vt);
            } else {
                if (pass != null && !pass.trim().isEmpty()) {
                    if (!pass.equals(confirm)) return ResponseEntity.badRequest().body("Mật khẩu xác nhận không khớp!");
                    String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*])(?=.{8,}).*$";
                    if (!pass.matches(passwordRegex)) return ResponseEntity.badRequest().body("Mật khẩu phải từ 8 ký tự, gồm chữ hoa, thường, số và ký tự đặc biệt!");
                    tk.setMatKhau(passwordEncoder.encode(pass));
                }
            }

            TaiKhoan savedTk = taiKhoanRepository.save(tk);
            target.setTaiKhoan(savedTk);

            if (isNew) {
                GioHang gioHang = new GioHang();
                gioHang.setTaiKhoan(savedTk);
                gioHangRepository.save(gioHang);
            }

            target.setChucVu(cv);
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

    public String kiemTraDangNhap() {
        TaiKhoan admin = taiKhoanRepository.findById(1).orElse(null);
        if (admin == null) return "Không tìm thấy user admin01 trong DB!";
        String hashTrongDb = admin.getMatKhau();
        boolean isMatch = passwordEncoder.matches("123456", hashTrongDb);
        return "Mã Hash trong DB: " + hashTrongDb + "<br>Kết quả so sánh với '123456': " + (isMatch ? "KHỚP 100% (Mật khẩu đúng)" : "SAI BÉT (Mật khẩu sai)");
    }

    public ResponseEntity<?> layHoSoCuaToi(Authentication authentication) {
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
        data.put("id", nv.getId());
        data.put("ma", nv.getMa());
        data.put("hoTen", nv.getHoTen());
        data.put("hinhAnh", nv.getHinhAnh());
        data.put("gioiTinh", nv.getGioiTinh());
        data.put("ngaySinh", nv.getNgaySinh() != null ? nv.getNgaySinh().toString() : null);
        data.put("soDienThoai", nv.getSoDienThoai());
        data.put("email", nv.getEmail());
        data.put("diaChi", nv.getDiaChi());
        data.put("tenDangNhap", nv.getTaiKhoan() != null ? nv.getTaiKhoan().getTenDangNhap() : "");
        data.put("daDoiTenDangNhap", nv.getTaiKhoan() != null && Boolean.TRUE.equals(nv.getTaiKhoan().getDaDoiTenDangNhap()));
        data.put("vaiTro", nv.getTaiKhoan() != null && nv.getTaiKhoan().getVaiTro() != null ? nv.getTaiKhoan().getVaiTro().getMa() : "");

        Map<String, Object> chucVuMap = new HashMap<>();
        chucVuMap.put("ten", nv.getChucVu() != null ? nv.getChucVu().getTen() : "Nhân viên");
        data.put("chucVu", chucVuMap);
        data.put("trangThai", nv.getTrangThai());
        return ResponseEntity.ok(data);
    }

    @Transactional
    public ResponseEntity<?> capNhatHoSoCuaToi(Map<String, String> payload, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));

            String username = authentication.getName();
            Optional<NhanVien> nvOpt = nhanVienRepository.findByTaiKhoan_TenDangNhap(username);
            TaiKhoan tk;

            if (nvOpt.isEmpty()) {
                tk = taiKhoanRepository.findByTenDangNhap(username).orElse(null);
                if (tk == null) return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản không hợp lệ"));
            } else {
                tk = nvOpt.get().getTaiKhoan();
            }

            if (payload.containsKey("tenDangNhap") && tk != null) {
                String newUsername = payload.get("tenDangNhap").trim();
                String oldUsername = tk.getTenDangNhap();

                if (!newUsername.isEmpty() && !newUsername.equals(oldUsername)) {
                    if (Boolean.TRUE.equals(tk.getDaDoiTenDangNhap())) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Bạn chỉ được phép thay đổi tên đăng nhập 1 lần duy nhất!"));
                    }
                    if (taiKhoanRepository.findByTenDangNhap(newUsername).isPresent()) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Tên đăng nhập này đã có người sử dụng!"));
                    }
                    tk.setTenDangNhap(newUsername);
                    tk.setDaDoiTenDangNhap(true);
                    taiKhoanRepository.save(tk);
                }
            }

            if (nvOpt.isPresent()) {
                NhanVien nv = nvOpt.get();
                if (payload.containsKey("hoTen")) {
                    String hoTen = payload.get("hoTen").trim();
                    if (hoTen.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Họ tên không được để trống!"));
                    nv.setHoTen(hoTen);
                }

                if (payload.containsKey("soDienThoai") && !payload.get("soDienThoai").isEmpty()) {
                    String phone = payload.get("soDienThoai").trim();
                    if (!phone.matches("^0[0-9]{9}$")) return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại phải đủ 10 số và bắt đầu bằng số 0!"));
                    if (nhanVienRepository.existsBySoDienThoaiAndIdNot(phone, nv.getId())) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Số điện thoại này đã được người khác sử dụng!"));
                    }
                    nv.setSoDienThoai(phone);
                }

                if (payload.containsKey("email") && !payload.get("email").isEmpty()) {
                    String email = payload.get("email").trim();
                    if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) return ResponseEntity.badRequest().body(Map.of("message", "Email không đúng định dạng!"));
                    TaiKhoan tkCheck = taiKhoanRepository.findByTenDangNhap(email).orElse(null);
                    if (tkCheck != null && (nv.getTaiKhoan() == null || !tkCheck.getId().equals(nv.getTaiKhoan().getId()))) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Email này đã được đăng ký cho tài khoản khác!"));
                    }
                    nv.setEmail(email);
                }

                if (payload.containsKey("gioiTinh")) nv.setGioiTinh(payload.get("gioiTinh"));
                if (payload.containsKey("diaChi")) nv.setDiaChi(payload.get("diaChi"));
                if (payload.containsKey("hinhAnh")) nv.setHinhAnh(payload.get("hinhAnh"));

                if (payload.containsKey("ngaySinh") && payload.get("ngaySinh") != null && !payload.get("ngaySinh").isEmpty()) {
                    try {
                        nv.setNgaySinh(java.sql.Date.valueOf(payload.get("ngaySinh")));
                    } catch (Exception e) {}
                }
                nhanVienRepository.save(nv);
            }
            return ResponseEntity.ok(Map.of("message", "Cập nhật hồ sơ cá nhân thành công"));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Lưu thất bại! Có lỗi xảy ra."));
        }
    }

    @Transactional
    public ResponseEntity<?> doiMatKhauAnToan(Map<String, String> payload, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));

        String username = authentication.getName();
        TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(username).orElse(null);
        if (tk == null) return ResponseEntity.badRequest().body(Map.of("message", "Tài khoản không tồn tại"));

        String oldPw = payload.get("oldPassword");
        String newPw = payload.get("newPassword");

        if (oldPw == null || newPw == null) return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ"));
        if (oldPw.equals(newPw)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Mật khẩu mới không được trùng với mật khẩu hiện tại!"));
        }

        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*])(?=.{8,}).*$";
        if (!newPw.matches(passwordRegex)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt!"));
        }

        if (!passwordEncoder.matches(oldPw, tk.getMatKhau())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Mật khẩu hiện tại không đúng"));
        }

        tk.setMatKhau(passwordEncoder.encode(newPw));
        taiKhoanRepository.save(tk);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu an toàn thành công"));
    }
}