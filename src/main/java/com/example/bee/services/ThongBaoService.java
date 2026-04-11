package com.example.bee.services;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.notification.ThongBao;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThongBaoService {

    private final ThongBaoRepository thongBaoRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    private TaiKhoan getLoggedInAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        return taiKhoanRepository.findByTenDangNhap(auth.getName()).orElse(null);
    }

    public ResponseEntity<?> layThongBaoCuaToi() {
        TaiKhoan tk = getLoggedInAccount();
        if (tk == null) return ResponseEntity.ok(Collections.emptyList());

        List<ThongBao> list = thongBaoRepository.findByTaiKhoanIdOrderByNgayTaoDesc(tk.getId())
                .stream()
                .filter(tb -> tb.getDaXoa() == null || !tb.getDaXoa())
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    @Transactional
    public ResponseEntity<?> danhDauDaDoc(Long id) {
        ThongBao tb = thongBaoRepository.findById(id).orElse(null);
        if (tb != null) {
            tb.setDaDoc(true);
            thongBaoRepository.save(tb);
            return ResponseEntity.ok(Collections.singletonMap("message", "Đã đọc"));
        }
        return ResponseEntity.notFound().build();
    }

    @Transactional
    public ResponseEntity<?> docTatCa() {
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

    @Transactional
    public ResponseEntity<?> xoaThongBao(Long id) {
        ThongBao tb = thongBaoRepository.findById(id).orElse(null);
        if (tb != null) {
            tb.setDaXoa(true);
            thongBaoRepository.save(tb);
            return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa"));
        }
        return ResponseEntity.notFound().build();
    }

    @Transactional
    public ResponseEntity<?> xoaTatCaThongBao() {
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
}