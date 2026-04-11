package com.example.bee.services;

import com.example.bee.entities.catalog.KichThuoc;
import com.example.bee.repositories.catalog.KichThuocRepository;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KichThuocService {

    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final KichThuocRepository kichThuocRepository;

    private String taoMaTuDong() {
        String timeStr = String.valueOf(System.currentTimeMillis());
        return "KT" + timeStr.substring(timeStr.length() - 5);
    }

    public Page<KichThuoc> layDanhSach(String q, Boolean trangThai, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return kichThuocRepository.search(q, trangThai, pageable);
    }

    public List<KichThuoc> layTatCaHoatDong() {
        return kichThuocRepository.findByTrangThaiTrue();
    }

    public KichThuoc layChiTiet(Integer id) {
        return kichThuocRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu!"));
    }

    @Transactional
    public KichThuoc taoMoi(KichThuoc body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty())
                ? taoMaTuDong()
                : body.getMa().trim().toUpperCase();
        if (ma.length() > 20 || !ma.matches("^[A-Z0-9_]*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã max 20, cho phép dấu '_'");
        }
        if (ten.isEmpty() || ten.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên size max 100!");
        if (kichThuocRepository.existsByTenIgnoreCase(ten))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Size này có rồi!");
        if (kichThuocRepository.existsByMaIgnoreCase(ma))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã size bị trùng!");
        KichThuoc entity = new KichThuoc();
        entity.setMa(ma);
        entity.setTen(ten);
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        return kichThuocRepository.save(entity);
    }

    @Transactional
    public KichThuoc capNhat(Integer id, KichThuoc body) {
        KichThuoc entity = kichThuocRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";
        if (newTen.isEmpty() || newTen.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên size max 100!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && kichThuocRepository.existsByTenIgnoreCase(newTen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trùng tên size!");
        }
        Boolean newTrangThai = body.getTrangThai();
        if (newTrangThai != null && !newTrangThai && Boolean.TRUE.equals(entity.getTrangThai())) {
            boolean isUsed = sanPhamChiTietRepository.existsByKichThuoc_IdAndTrangThaiTrue(id);
            if (isUsed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể ngừng hoạt động! Đang có sản phẩm sử dụng kích thước này.");
            }
        }

        entity.setTen(newTen);
        entity.setTrangThai(newTrangThai != null ? newTrangThai : entity.getTrangThai());
        return kichThuocRepository.save(entity);
    }

    @Transactional
    public ResponseEntity<?> doiTrangThai(Integer id) {
        KichThuoc kichThuoc = kichThuocRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (kichThuoc.getTrangThai() != null && kichThuoc.getTrangThai() == true) {
            boolean isUsed = sanPhamChiTietRepository.existsByKichThuoc_IdAndTrangThaiTrue(id);
            if (isUsed) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Không thể tắt size này! Đang có sản phẩm chi tiết sử dụng kích thước này."
                ));
            }
        }
        kichThuoc.setTrangThai(!kichThuoc.getTrangThai());
        kichThuocRepository.save(kichThuoc);
        return ResponseEntity.ok().build();
    }
}