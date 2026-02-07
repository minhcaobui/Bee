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

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<NV> getOne(@PathVariable Integer id) {
        // PHẢI dùng hàm có JOIN FETCH để lấy luôn tài khoản, tránh lỗi "no session"
        NV nv = nvRepo.findByIdWithTaiKhoan(id).orElse(null);
        if (nv == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(nv);
    }

    @Transactional
    @PostMapping("/save")
    public ResponseEntity<?> save(@ModelAttribute NV nv,
                                  @RequestParam(value = "fileAnh", required = false) MultipartFile file,
                                  @RequestParam(value = "password", required = false) String pass,
                                  @RequestParam(value = "confirmPassword", required = false) String confirm) {
        try {
            String phoneRegex = "^0[0-9]{9}$";
            String gmailRegex = "^[a-zA-Z0-9._%+-]+@gmail\\.com$";

            if (nv.getSoDienThoai() == null || !nv.getSoDienThoai().matches(phoneRegex))
                return ResponseEntity.badRequest().body("SĐT phải đủ 10 số và bắt đầu bằng 0!");

            if (nv.getEmail() == null || nv.getEmail().isBlank())
                return ResponseEntity.badRequest().body("Email không được để trống!");

            if (!nv.getEmail().matches(gmailRegex))
                return ResponseEntity.badRequest().body("Email phải đúng định dạng @gmail.com!");

            if (nv.getNgaySinh() == null)
                return ResponseEntity.badRequest().body("Ngày sinh không được để trống!");

            NV target;
            boolean isNew = (nv.getId() == null);

            if (!isNew) {
                target = nvRepo.findByIdWithTaiKhoan(nv.getId()).orElseThrow();

                if (target.getChucVu() != null && target.getChucVu().getId() == 1) {
                    return ResponseEntity.badRequest().body("Thông tin Admin là bảo mật, không thể xem hoặc sửa!");
                }
                if (nvRepo.existsBySoDienThoaiAndIdNot(nv.getSoDienThoai(), nv.getId()))
                    return ResponseEntity.badRequest().body("Số điện thoại này đã thuộc về nhân viên khác!");

                if (nvRepo.existsByEmailAndIdNot(nv.getEmail(), nv.getId()))
                    return ResponseEntity.badRequest().body("Email/Tài khoản này đã tồn tại!");
            } else {
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

            // --- BƯỚC 3: ĐỒNG BỘ TÀI KHOẢN (EMAIL = USERNAME) ---
            TK tk = (target.getTaiKhoan() == null) ? new TK() : target.getTaiKhoan();
            tk.setTenDangNhap(nv.getEmail());
            if (isNew) {
                tk.setMatKhau(pass);
                tk.setTrangThai(true);
                VT vt = new VT();
                vt.setId(2);
                tk.setVaiTro(vt);
            }

            // SỬA TẠI ĐÂY: Lưu TK riêng biệt để nó trở thành "Managed" trước khi gán vào NV
            TK savedTk = tkRepo.save(tk);
            target.setTaiKhoan(savedTk);

            // --- BƯỚC 4: GÁN THÔNG TIN CÁ NHÂN ---
            target.setHoTen(nv.getHoTen());
            target.setSoDienThoai(nv.getSoDienThoai());
            target.setEmail(nv.getEmail());
            target.setNgaySinh(nv.getNgaySinh());
            target.setGioiTinh(nv.getGioiTinh());
            target.setDiaChi(nv.getDiaChi());

            if (!isNew) {
                target.setTrangThai(nv.getTrangThai());
            }

            if (file != null && !file.isEmpty()) {
                target.setHinhAnh("data:" + file.getContentType() + ";base64," +
                        Base64.getEncoder().encodeToString(file.getBytes()));
            }

            // --- BƯỚC 5: LƯU NHÂN VIÊN ---
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
    @GetMapping("/search") // URL thực tế sẽ là /api/admin/nhan-vien/search
    public String search(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "status", required = false) Boolean status,
            Model model) {

        // Gọi hàm search từ repo (đảm bảo hàm này đã có LEFT JOIN FETCH n.chucVu)
        List<NV> ketQua = nvRepo.searchNhanVien(keyword, status);
        model.addAttribute("list", ketQua);

        // TRẢ VỀ FRAGMENT:
        // Phải khớp với đường dẫn templates/admin/staff/staff.html của bạn
        return "admin/staff/staff :: table_body";
    }
}
