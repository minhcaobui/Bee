package com.example.bee.controllers.api.staff;

import com.example.bee.entities.staff.CV;
import com.example.bee.entities.staff.NV;
import com.example.bee.entities.staff.TK;
import com.example.bee.entities.staff.VT;
import com.example.bee.repositories.staff.NVRepository;
import com.example.bee.repositories.staff.TKRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/api/admin/nhan-vien")
public class NhanVienApi {
    @Autowired
    private NVRepository nvRepo;
    @Autowired private TKRepository tkRepo;

    // 1. LẤY CHI TIẾT (Khớp với window.StaffApp.openEdit trong JS)
    @GetMapping("/detail/{id}")
    @ResponseBody
    public ResponseEntity<NV> getOne(@PathVariable Integer id) {
        // Sử dụng findByIdWithTaiKhoan để tránh lỗi Lazy Loading khi lấy thông tin Email/Tài khoản
        NV nv = nvRepo.findByIdWithTaiKhoan(id).orElse(null);
        if (nv == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(nv);
    }

    // 2. LẤY DANH SÁCH JSON (Phục vụ việc vẽ lại bảng bằng JS mà không cần load lại trang)
    @GetMapping("/list-json")
    @ResponseBody
    public List<NV> getListJson(
            @RequestParam(value = "q", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "status", required = false) Boolean status) {
        return nvRepo.searchNhanVien(keyword, status);
    }

    // 3. BẬT TẮT TRẠNG THÁI NHANH (PatchMapping giúp gạt nút ngoài bảng mượt mà)
    @PatchMapping("/toggle-status/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id) {
        NV nv = nvRepo.findById(id).orElse(null);
        if (nv == null) return ResponseEntity.notFound().build();

        nv.setTrangThai(!nv.getTrangThai());
        nvRepo.save(nv);

        // Trả về text để Toast bên JS hiển thị
        return ResponseEntity.ok(nv.getTrangThai() ? "Hồ sơ đã được kích hoạt!" : "Nhân viên đã tạm nghỉ việc!");
    }

    // 4. LƯU HỒ SƠ (Cả Thêm mới và Cập nhật)
    @Transactional
    @PostMapping("/save")
    public ResponseEntity<?> save(@ModelAttribute NV nv,
                                  @RequestParam(value = "fileAnh", required = false) MultipartFile file,
                                  @RequestParam(value = "password", required = false) String pass,
                                  @RequestParam(value = "confirmPassword", required = false) String confirm) {
        try {
            // --- 1. VALIDATE TRỐNG VÀ ĐỊNH DẠNG ---
            if (nv.getHoTen() == null || nv.getHoTen().trim().isEmpty())
                return ResponseEntity.badRequest().body("Họ tên không được để trống!");

            if (nv.getSoDienThoai() == null || nv.getSoDienThoai().isBlank())
                return ResponseEntity.badRequest().body("Số điện thoại không được để trống!");

            String phoneRegex = "^0[0-9]{9}$";
            if (!nv.getSoDienThoai().matches(phoneRegex))
                return ResponseEntity.badRequest().body("SĐT phải đủ 10 số và bắt đầu bằng 0!");

            if (nv.getEmail() == null || nv.getEmail().isBlank())
                return ResponseEntity.badRequest().body("Email không được để trống!");

            String gmailRegex = "^[a-zA-Z0-9._%+-]+@gmail\\.com\\.vn$"; // Thêm \\.vn vào đây
            if (!nv.getEmail().matches(gmailRegex))
                return ResponseEntity.badRequest().body("Email phải đúng định dạng @gmail.com.vn!");

            if (nv.getNgaySinh() == null)
                return ResponseEntity.badRequest().body("Ngày sinh không được để trống!");

            // --- 2. VALIDATE TUỔI (TRÊN 15 TUỔI) ---
            // Vì Entity của b là LocalDate nên tính toán cực kỳ đơn giản và không bị lỗi
            java.time.LocalDate ngaySinh = nv.getNgaySinh();
            java.time.LocalDate hienTai = java.time.LocalDate.now();
            int tuoi = java.time.Period.between(ngaySinh, hienTai).getYears();

            if (tuoi < 15) {
                return ResponseEntity.badRequest().body("Nhân viên phải từ đủ 15 tuổi trở lên (Hiện tại: " + tuoi + " tuổi)!");
            }



            NV target;
            boolean isNew = (nv.getId() == null);

            // --- 3. KIỂM TRA TRÙNG LẶP (THÔNG MINH) ---
            if (!isNew) {
                target = nvRepo.findByIdWithTaiKhoan(nv.getId()).orElseThrow();

                if (target.getChucVu() != null && target.getChucVu().getId() == 1) {
                    return ResponseEntity.badRequest().body("Thông tin Admin là bảo mật, không thể xem hoặc sửa!");
                }

                // Sửa logic check trùng: Loại trừ ID hiện tại để b có thể lưu mà không cần đổi SĐT/Email cũ
                if (nvRepo.existsBySoDienThoaiAndIdNot(nv.getSoDienThoai(), nv.getId()))
                    return ResponseEntity.badRequest().body("Số điện thoại này đã thuộc về nhân viên khác!");

                if (nvRepo.existsByEmailAndIdNot(nv.getEmail(), nv.getId()))
                    return ResponseEntity.badRequest().body("Email/Tài khoản này đã tồn tại!");
            } else {
                // Thêm mới: Check trùng toàn hệ thống
                if (pass == null || pass.isBlank()) return ResponseEntity.badRequest().body("Mật khẩu không được để trống!");
                if (!pass.equals(confirm)) return ResponseEntity.badRequest().body("Mật khẩu xác nhận không khớp!");

                if (nvRepo.existsBySoDienThoai(nv.getSoDienThoai()))
                    return ResponseEntity.badRequest().body("Số điện thoại đã tồn tại!");

                if (nvRepo.existsByEmail(nv.getEmail()))
                    return ResponseEntity.badRequest().body("Email đã tồn tại!");

                target = new NV();
                target.setMa("TEMP");
                target.setTrangThai(true);

                CV cv = new CV();
                cv.setId(2);
                target.setChucVu(cv);
            }

            // --- 4. ĐỒNG BỘ TÀI KHOẢN (GIỮ NGUYÊN) ---
            TK tk = (target.getTaiKhoan() == null) ? new TK() : target.getTaiKhoan();
            tk.setTenDangNhap(nv.getEmail());
            if (isNew) {
                tk.setMatKhau(pass);
                tk.setTrangThai(true);
                VT vt = new VT();
                vt.setId(2);
                tk.setVaiTro(vt);
            }

            TK savedTk = tkRepo.save(tk);
            target.setTaiKhoan(savedTk);

            // --- 5. GÁN THÔNG TIN CÁ NHÂN (GIỮ NGUYÊN) ---
            target.setHoTen(nv.getHoTen());
            target.setSoDienThoai(nv.getSoDienThoai());
            target.setEmail(nv.getEmail());
            target.setNgaySinh(nv.getNgaySinh());
            target.setGioiTinh(nv.getGioiTinh());

            if (nv.getDiaChi() != null) {
                target.setDiaChi(nv.getDiaChi());
            }

            target.setTrangThai(nv.getTrangThai());

            // Xử lý ảnh Base64 (GIỮ NGUYÊN)
            if (file != null && !file.isEmpty()) {
                target.setHinhAnh("data:" + file.getContentType() + ";base64," +
                        Base64.getEncoder().encodeToString(file.getBytes()));
            }

            // --- 6. LƯU XUỐNG DATABASE ---
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

    // 5. SEARCH FRAGMENT (Giữ nguyên cho Thymeleaf)
    @GetMapping("/search")
    public String search(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "status", required = false) Boolean status,
            Model model) {
        List<NV> ketQua = nvRepo.searchNhanVien(keyword, status);
        model.addAttribute("list", ketQua);
        return "admin/staff/staff :: table_body";
    }
}