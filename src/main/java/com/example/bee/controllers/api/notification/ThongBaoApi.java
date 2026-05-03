package com.example.bee.controllers.api.notification;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.notification.ThongBao;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/thong-bao")
@RequiredArgsConstructor
public class ThongBaoApi {

    private final ThongBaoRepository thongBaoRepository;
    private final TaiKhoanRepository taiKhoanRepository;

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
            if (tb.getDaXoa() == null || !tb.getDaXoa()) {
                tb.setDaXoa(true);
                hasChanges = true;
            }
        }
        if (hasChanges) thongBaoRepository.saveAll(list);
        return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa tất cả"));
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            List<ThongBao> oldNotifs = thongBaoRepository.findAll().stream()
                    .filter(tb -> (tb.getDaXoa() != null && tb.getDaXoa()) ||
                            (tb.getNgayTao() != null && tb.getNgayTao().isBefore(thirtyDaysAgo)))
                    .collect(Collectors.toList());
            if (!oldNotifs.isEmpty()) {
                thongBaoRepository.deleteAll(oldNotifs);
                System.out.println("Scheduler: Đã dọn dẹp " + oldNotifs.size() + " thông báo cũ/rác.");
            }
        } catch (Exception e) {
            System.err.println("Scheduler Lỗi: Không thể dọn dẹp thông báo. " + e.getMessage());
        }
    }
}