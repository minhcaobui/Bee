package com.example.bee.controllers.api.product;

import com.example.bee.entities.customer.KhachHang;
import com.example.bee.entities.reviews.DanhGia;
import com.example.bee.entities.user.NhanVien;
import com.example.bee.repositories.customer.KhachHangRepository;
import com.example.bee.repositories.reviews.DanhGiaRepository;
import com.example.bee.repositories.role.NhanVienRepository;
import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter; // 🌟 Thêm import này
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class AdminReviewApi {

    private final DanhGiaRepository danhGiaRepo;
    private final KhachHangRepository khRepo;
    private final NhanVienRepository nvRepo;

    // 1. API Lấy danh sách đánh giá (Có Lọc & Phân trang)
    @GetMapping
    public ResponseEntity<?> getReviews(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer soSao,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Xử lý Sắp xếp
        Sort sortObj = Sort.by(Sort.Direction.DESC, "noiDungTraLoi");

        if ("OLDEST".equalsIgnoreCase(sort)) {
            sortObj = sortObj.and(Sort.by(Sort.Direction.ASC, "ngayTao"));
        } else {
            sortObj = sortObj.and(Sort.by(Sort.Direction.DESC, "ngayTao"));
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);

        // Chuẩn hóa chuỗi tìm kiếm
        String keyword = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        String status = (trangThai != null && !trangThai.trim().isEmpty()) ? trangThai.trim() : null;

        Page<DanhGia> resultPage = danhGiaRepo.findAdminReviews(keyword, soSao, status, pageable);

        // 🌟 BỘ FORMATTER KÉP: Xử lý mượt mà cả Date cũ và LocalDateTime mới
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        Page<ReviewAdminResponse> responsePage = resultPage.map(dg -> {
            // Tìm tên khách hàng từ TaiKhoan_Id
            String tenKH = "Khách vãng lai";
            if (dg.getTaiKhoan() != null) {
                KhachHang kh = khRepo.findByTaiKhoan_Id(dg.getTaiKhoan().getId()).orElse(null);
                if (kh != null) tenKH = kh.getHoTen();
            }

            return ReviewAdminResponse.builder()
                    .id(dg.getId())
                    .tenKhachHang(tenKH)
                    .sanPhamId(dg.getSanPham() != null ? dg.getSanPham().getId() : null)
                    .tenSanPham(dg.getSanPham() != null ? dg.getSanPham().getTen() : "Sản phẩm")
                    .soSao(dg.getSoSao())
                    .noiDung(dg.getNoiDung())
                    .phanLoai(dg.getPhanLoai())
                    .danhSachHinhAnh(dg.getDanhSachHinhAnh())
                    .ngayTao(dg.getNgayTao() != null ? dg.getNgayTao().format(dtf) : null) // 🌟 Dùng dtf cho LocalDateTime
                    .tenNhanVienTraLoi(dg.getNhanVienTraLoi() != null ? dg.getNhanVienTraLoi().getHoTen() : null)
                    .noiDungTraLoi(dg.getNoiDungTraLoi())
                    .ngayTraLoi(dg.getNgayTraLoi() != null ? sdf.format(dg.getNgayTraLoi()) : null) // 🌟 Dùng sdf cho Date
                    .build();
        });

        return ResponseEntity.ok(responsePage);
    }

    // 2. API Nhân viên gửi câu trả lời
    @PostMapping("/{id}/reply")
    @Transactional // 🌟 ĐÃ THÊM TRANSACTIONAL ĐỂ BẢO VỆ DATABASE
    public ResponseEntity<?> replyReview(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String noiDungTraLoi = payload.get("noiDungTraLoi");

        if (noiDungTraLoi == null || noiDungTraLoi.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung trả lời không được để trống!");
        }

        // 🌟 ĐÃ THÊM VALIDATE CHỐNG TRÀN DATABASE
        if (noiDungTraLoi.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung trả lời quá dài (Tối đa 1000 ký tự)!");
        }

        // Lấy thông tin Nhân viên đang đăng nhập (Bảo mật)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập bằng tài khoản Nhân viên!");
        }

        NhanVien nv = nvRepo.findByTaiKhoan_TenDangNhap(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không có quyền thao tác!"));

        DanhGia dg = danhGiaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá!"));

        // Ghi nhận phản hồi
        dg.setNhanVienTraLoi(nv);
        dg.setNoiDungTraLoi(noiDungTraLoi.trim());
        dg.setNgayTraLoi(new Date());

        danhGiaRepo.save(dg);

        return ResponseEntity.ok(Map.of("message", "Đã gửi phản hồi thành công!"));
    }

    // ==========================================
    // DTO Dành cho Response
    // ==========================================
    @Data
    @Builder
    public static class ReviewAdminResponse {
        private Long id;
        private String tenKhachHang;
        private Integer sanPhamId;
        private String tenSanPham;
        private Integer soSao;
        private String noiDung;
        private String phanLoai;
        private String danhSachHinhAnh;
        private String ngayTao;
        private String tenNhanVienTraLoi;
        private String noiDungTraLoi;
        private String ngayTraLoi;
    }
}