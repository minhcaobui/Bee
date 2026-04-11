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

    private TaiKhoan layTaiKhoanDangNhap() {
        Authentication xacThuc = SecurityContextHolder.getContext().getAuthentication();
        if (xacThuc == null || !xacThuc.isAuthenticated() || xacThuc.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        return taiKhoanRepository.findByTenDangNhap(xacThuc.getName()).orElse(null);
    }

    public ResponseEntity<?> layThongBaoCuaToi() {
        TaiKhoan taiKhoan = layTaiKhoanDangNhap();
        if (taiKhoan == null) return ResponseEntity.ok(Collections.emptyList());

        List<ThongBao> danhSach = thongBaoRepository.findByTaiKhoanIdOrderByNgayTaoDesc(taiKhoan.getId())
                .stream()
                .filter(tb -> tb.getDaXoa() == null || !tb.getDaXoa())
                .collect(Collectors.toList());

        return ResponseEntity.ok(danhSach);
    }

    @Transactional
    public ResponseEntity<?> danhDauDaDoc(Long idThongBao) {
        ThongBao thongBao = thongBaoRepository.findById(idThongBao).orElse(null);
        if (thongBao != null) {
            thongBao.setDaDoc(true);
            thongBaoRepository.save(thongBao);
            return ResponseEntity.ok(Collections.singletonMap("message", "Đã đọc"));
        }
        return ResponseEntity.notFound().build();
    }

    @Transactional
    public ResponseEntity<?> docTatCaThongBao() {
        TaiKhoan taiKhoan = layTaiKhoanDangNhap();
        if (taiKhoan == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<ThongBao> danhSach = thongBaoRepository.findByTaiKhoanIdOrderByNgayTaoDesc(taiKhoan.getId());
        boolean coThayDoi = false;

        for (ThongBao thongBao : danhSach) {
            if (thongBao.getDaDoc() == null || !thongBao.getDaDoc()) {
                thongBao.setDaDoc(true);
                coThayDoi = true;
            }
        }

        if (coThayDoi) thongBaoRepository.saveAll(danhSach);
        return ResponseEntity.ok(Collections.singletonMap("message", "Đã đọc tất cả"));
    }

    @Transactional
    public ResponseEntity<?> xoaThongBao(Long idThongBao) {
        ThongBao thongBao = thongBaoRepository.findById(idThongBao).orElse(null);
        if (thongBao != null) {
            thongBao.setDaXoa(true);
            thongBaoRepository.save(thongBao);
            return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa"));
        }
        return ResponseEntity.notFound().build();
    }

    @Transactional
    public ResponseEntity<?> xoaTatCaThongBao() {
        TaiKhoan taiKhoan = layTaiKhoanDangNhap();
        if (taiKhoan == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<ThongBao> danhSach = thongBaoRepository.findByTaiKhoanIdOrderByNgayTaoDesc(taiKhoan.getId());
        boolean coThayDoi = false;

        for (ThongBao thongBao : danhSach) {
            if (thongBao.getDaXoa() == null || !thongBao.getDaXoa()) {
                thongBao.setDaXoa(true);
                coThayDoi = true;
            }
        }

        if (coThayDoi) thongBaoRepository.saveAll(danhSach);
        return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa tất cả"));
    }
}