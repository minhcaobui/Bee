package com.example.bee.controllers.api.notification;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.notification.ThongBao;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/thong-bao")
@RequiredArgsConstructor
public class ThongBaoApi {

    private final ThongBaoRepository thongBaoRepository;
    // 🌟 ĐÃ FIX LỖI 1: Chuyển sang dùng TaiKhoanRepository để dùng chung cho cả Khách và Admin
    private final TaiKhoanRepository taiKhoanRepository;

    // Hàm dùng chung để lấy Tài khoản đang đăng nhập
    private TaiKhoan getLoggedInAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        return taiKhoanRepository.findByTenDangNhap(auth.getName()).orElse(null);
    }

    @GetMapping("/my-notifications")
    public ResponseEntity<?> getMyNotifications() {
        TaiKhoan tk = getLoggedInAccount();
        if (tk == null) return ResponseEntity.ok(Collections.emptyList());

        // 🌟 ĐÃ FIX LỖI 2: Chỉ lấy những thông báo CHƯA XÓA (Loại bỏ Zombie)
        List<ThongBao> list = thongBaoRepository.findByTaiKhoanIdOrderByNgayTaoDesc(tk.getId())
                .stream()
                .filter(tb -> tb.getDaXoa() == null || !tb.getDaXoa())
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        ThongBao tb = thongBaoRepository.findById(id).orElse(null);
        if (tb != null) {
            tb.setDaDoc(true);
            thongBaoRepository.save(tb);
            return ResponseEntity.ok(Collections.singletonMap("message", "Đã đọc"));
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead() {
        TaiKhoan tk = getLoggedInAccount();
        if (tk == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<ThongBao> list = thongBaoRepository.findByTaiKhoanIdOrderByNgayTaoDesc(tk.getId());
        boolean hasChanges = false;

        for (ThongBao tb : list) {
            // 🌟 ĐÃ FIX LỖI 3: Chỉ update những cái CHƯA ĐỌC, tiết kiệm tài nguyên DB
            if (tb.getDaDoc() == null || !tb.getDaDoc()) {
                tb.setDaDoc(true);
                hasChanges = true;
            }
        }

        if (hasChanges) thongBaoRepository.saveAll(list);
        return ResponseEntity.ok(Collections.singletonMap("message", "Đã đọc tất cả"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        ThongBao tb = thongBaoRepository.findById(id).orElse(null);
        if (tb != null) {
            tb.setDaXoa(true);
            thongBaoRepository.save(tb);
            return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa"));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<?> deleteAllMyNotifications() {
        TaiKhoan tk = getLoggedInAccount();
        if (tk == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<ThongBao> list = thongBaoRepository.findByTaiKhoanIdOrderByNgayTaoDesc(tk.getId());
        boolean hasChanges = false;

        for (ThongBao tb : list) {
            // 🌟 ĐÃ FIX LỖI 3: Chỉ đánh dấu xóa những cái CHƯA XÓA
            if (tb.getDaXoa() == null || !tb.getDaXoa()) {
                tb.setDaXoa(true);
                hasChanges = true;
            }
        }

        if (hasChanges) thongBaoRepository.saveAll(list);
        return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa tất cả"));
    }
}