package com.example.bee.services;

import com.example.bee.entities.catalog.Hang;
import com.example.bee.repositories.catalog.HangRepository;
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
public class HangService {

    private final HangRepository hangRepository;
    private final SanPhamRepository sanPhamRepository;

    private String generateMa() {
        String timeStr = String.valueOf(System.currentTimeMillis());
        return "H" + timeStr.substring(timeStr.length() - 5);
    }

    public Page<Hang> layDanhSach(String q, Boolean trangThai, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return hangRepository.search(q, trangThai, pageable);
    }

    public List<Hang> layTatCaHoatDong() {
        return hangRepository.findByTrangThaiTrue();
    }

    public Hang layChiTiet(Integer id) {
        return hangRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Hang taoMoi(Hang body) {
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
        if (hangRepository.existsByTenIgnoreCase(ten))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên hãng này đã tồn tại!");
        if (hangRepository.existsByMaIgnoreCase(ma))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã hãng này đã tồn tại!");

        Hang entity = new Hang();
        entity.setMa(ma);
        entity.setTen(ten);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());
        return hangRepository.save(entity);
    }

    @Transactional
    public Hang capNhat(Integer id, Hang body) {
        Hang entity = hangRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";
        if (newTen.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên không được để trống!");
        if (newTen.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tối đa 100 ký tự!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && hangRepository.existsByTenIgnoreCase(newTen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên này đã tồn tại ở bản ghi khác!");
        }

        Boolean newTrangThai = body.getTrangThai();
        if (newTrangThai != null && !newTrangThai && Boolean.TRUE.equals(entity.getTrangThai())) {
            boolean isUsed = sanPhamRepository.existsByHang_IdAndTrangThaiTrue(id);
            if (isUsed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể ngừng hoạt động! Đang có sản phẩm thuộc hãng này đang được bày bán.");
            }
        }

        entity.setTen(newTen);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(newTrangThai != null ? newTrangThai : entity.getTrangThai());
        entity.setNgaySua(LocalDateTime.now());
        return hangRepository.save(entity);
    }

    @Transactional
    public ResponseEntity<?> doiTrangThai(Integer id) {
        Hang hang = hangRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (hang.getTrangThai() != null && hang.getTrangThai() == true) {
            boolean isUsed = sanPhamRepository.existsByHang_IdAndTrangThaiTrue(id);
            if (isUsed) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Không thể ngừng hoạt động! Đang có sản phẩm thuộc hãng này đang được bày bán."
                ));
            }
        }
        hang.setTrangThai(!hang.getTrangThai());
        hangRepository.save(hang);
        return ResponseEntity.ok().build();
    }
}