package com.example.bee.services;

import com.example.bee.entities.catalog.DanhMuc;
import com.example.bee.repositories.catalog.DanhMucRepository;
import com.example.bee.repositories.products.SanPhamRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DanhMucService {

    private final DanhMucRepository danhMucRepository;
    private final SanPhamRepository sanPhamRepository;

    private String generateMa() {
        String timeStr = String.valueOf(System.currentTimeMillis());
        return "DM" + timeStr.substring(timeStr.length() - 5);
    }

    public Page<DanhMuc> layDanhSach(String q, Boolean trangThai, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return danhMucRepository.search(q, trangThai, pageable);
    }

    public List<DanhMuc> layTatCaHoatDong() {
        return danhMucRepository.findByTrangThaiTrue();
    }

    public DanhMuc layChiTiet(Integer id) {
        return danhMucRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));
    }

    @Transactional
    public DanhMuc taoMoi(DanhMuc body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty())
                ? generateMa()
                : body.getMa().trim().toUpperCase();

        if (ma.length() > 20)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã thuộc tính tối đa 20 ký tự!");
        if (!ma.matches("^[A-Z0-9_]*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã chỉ được chứa chữ hoa, số và dấu gạch dưới (_)");
        }
        if (ten.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên thuộc tính không được để trống!");
        if (ten.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên thuộc tính tối đa 100 ký tự!");
        if (danhMucRepository.existsByTenIgnoreCase(ten))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên danh mục này đã tồn tại!");
        if (danhMucRepository.existsByMaIgnoreCase(ma))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã danh mục này đã tồn tại!");

        DanhMuc entity = new DanhMuc();
        entity.setMa(ma);
        entity.setTen(ten);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());
        return danhMucRepository.save(entity);
    }

    @Transactional
    public DanhMuc capNhat(Integer id, DanhMuc body) {
        DanhMuc entity = danhMucRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";
        if (newTen.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên không được để trống!");
        if (newTen.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tối đa 100 ký tự!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && danhMucRepository.existsByTenIgnoreCase(newTen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên này đã tồn tại ở bản ghi khác!");
        }

        Boolean newTrangThai = body.getTrangThai();
        if (newTrangThai != null && !newTrangThai && Boolean.TRUE.equals(entity.getTrangThai())) {
            boolean isUsed = sanPhamRepository.existsByDanhMuc_IdAndTrangThaiTrue(id);
            if (isUsed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể ngừng hoạt động! Đang có sản phẩm thuộc danh mục này đang được bày bán.");
            }
        }

        entity.setTen(newTen);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(newTrangThai != null ? newTrangThai : entity.getTrangThai());
        entity.setNgaySua(LocalDateTime.now());
        return danhMucRepository.save(entity);
    }

    @Transactional
    public ResponseEntity<?> doiTrangThai(Integer id) {
        DanhMuc danhMuc = danhMucRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (danhMuc.getTrangThai() != null && danhMuc.getTrangThai() == true) {
            boolean isUsed = sanPhamRepository.existsByDanhMuc_IdAndTrangThaiTrue(id);
            if (isUsed) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Không thể ngừng hoạt động! Đang có sản phẩm thuộc danh mục này đang được bày bán."
                ));
            }
        }
        danhMuc.setTrangThai(!danhMuc.getTrangThai());
        danhMucRepository.save(danhMuc);
        return ResponseEntity.ok().build();
    }
}