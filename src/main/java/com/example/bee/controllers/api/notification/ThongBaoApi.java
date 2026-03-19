package com.example.bee.controllers.api.notification;

import com.example.bee.entities.notification.ThongBao;
import com.example.bee.repositories.customer.KhachHangRepository;
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
    private final KhachHangRepository khachHangRepository;

    @GetMapping("/my-notifications")
    public ResponseEntity<?> getMyNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        String username = auth.getName();
        var kh = khachHangRepository.findByTaiKhoan_TenDangNhap(username).orElse(null);
        if (kh != null && kh.getTaiKhoan() != null) {
            List<ThongBao> list = thongBaoRepository.findByTaiKhoanIdOrderByNgayTaoDesc(kh.getTaiKhoan().getId());
            // 🌟 LỌC BỎ NHỮNG THÔNG BÁO ĐÃ XÓA MỀM TRƯỚC KHI TRẢ VỀ FRONTEND
            List<ThongBao> activeList = list.stream()
                    .filter(tb -> tb.getDaXoa() == null || !tb.getDaXoa())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(activeList);
        }
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = auth.getName();
        var kh = khachHangRepository.findByTaiKhoan_TenDangNhap(username).orElse(null);
        if (kh != null && kh.getTaiKhoan() != null) {
            Integer currentUserId = kh.getTaiKhoan().getId();
            List<ThongBao> unreadList = thongBaoRepository.findByTaiKhoanIdAndDaDocFalse(currentUserId);
            for (ThongBao tb : unreadList) {
                tb.setDaDoc(true);
            }
            thongBaoRepository.saveAll(unreadList);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        ThongBao tb = thongBaoRepository.findById(id).orElse(null);
        if (tb != null) {
            tb.setDaDoc(true);
            thongBaoRepository.save(tb);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // 🌟 API XÓA 1 THÔNG BÁO (SOFT DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        ThongBao tb = thongBaoRepository.findById(id).orElse(null);
        if (tb != null) {
            tb.setDaXoa(true); // Đánh dấu đã xóa
            thongBaoRepository.save(tb);
            return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa"));
        }
        return ResponseEntity.notFound().build();
    }

    // 🌟 API XÓA TẤT CẢ (SOFT DELETE)
    @DeleteMapping("/delete-all")
    public ResponseEntity<?> deleteAllMyNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = auth.getName();
        var kh = khachHangRepository.findByTaiKhoan_TenDangNhap(username).orElse(null);
        if (kh != null && kh.getTaiKhoan() != null) {
            List<ThongBao> list = thongBaoRepository.findByTaiKhoanIdOrderByNgayTaoDesc(kh.getTaiKhoan().getId());
            for (ThongBao tb : list) {
                tb.setDaXoa(true); // Đánh dấu tất cả là đã xóa
            }
            thongBaoRepository.saveAll(list);
            return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa tất cả"));
        }
        return ResponseEntity.badRequest().build();
    }
}