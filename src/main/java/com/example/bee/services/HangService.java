package com.example.bee.services;

import com.example.bee.entities.catalog.Hang;
import com.example.bee.repositories.catalog.HangRepository;
import com.example.bee.repositories.products.SanPhamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HangService {

    private final HangRepository hangRepository;
    private final SanPhamRepository sanPhamRepository;

    private String generateMa() {
        String timeStr = String.valueOf(System.currentTimeMillis());
        return "H" + timeStr.substring(timeStr.length() - 5);
    }

    public Page<Hang> list(String q, Boolean trangThai, Pageable pageable) {
        return hangRepository.search(q, trangThai, pageable);
    }

    public List<Hang> getAllActive() {
        return hangRepository.findByTrangThaiTrue();
    }

    public Hang getById(Integer id) {
        return hangRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dữ liệu"));
    }

    @Transactional
    public Hang create(Hang body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty())
                ? generateMa()
                : body.getMa().trim().toUpperCase();

        if (ma.length() > 20) throw new IllegalArgumentException("Mã thuộc tính tối đa 20 ký tự!");
        if (!ma.matches("^[A-Z0-9_]*$")) throw new IllegalArgumentException("Mã chỉ được chứa chữ hoa, số và dấu gạch dưới (_)");
        if (ten.isEmpty()) throw new IllegalArgumentException("Tên thuộc tính không được để trống!");
        if (ten.length() > 100) throw new IllegalArgumentException("Tên thuộc tính tối đa 100 ký tự!");
        if (hangRepository.existsByTenIgnoreCase(ten)) throw new IllegalArgumentException("Tên hãng này đã tồn tại!");
        if (hangRepository.existsByMaIgnoreCase(ma)) throw new IllegalArgumentException("Mã hãng này đã tồn tại!");

        Hang entity = new Hang();
        entity.setMa(ma);
        entity.setTen(ten);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());
        return hangRepository.save(entity);
    }

    @Transactional
    public Hang update(Integer id, Hang body) {
        Hang entity = hangRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dữ liệu"));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";
        if (newTen.isEmpty()) throw new IllegalArgumentException("Tên không được để trống!");
        if (newTen.length() > 100) throw new IllegalArgumentException("Tên tối đa 100 ký tự!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && hangRepository.existsByTenIgnoreCase(newTen)) {
            throw new IllegalArgumentException("Tên này đã tồn tại ở bản ghi khác!");
        }

        Boolean newTrangThai = body.getTrangThai();
        if (newTrangThai != null && !newTrangThai && Boolean.TRUE.equals(entity.getTrangThai())) {
            boolean isUsed = sanPhamRepository.existsByHang_IdAndTrangThaiTrue(id);
            if (isUsed) {
                throw new IllegalArgumentException("Không thể ngừng hoạt động! Đang có sản phẩm thuộc hãng này đang được bày bán.");
            }
        }

        entity.setTen(newTen);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(newTrangThai != null ? newTrangThai : entity.getTrangThai());
        entity.setNgaySua(LocalDateTime.now());
        return hangRepository.save(entity);
    }

    @Transactional
    public void toggleStatus(Integer id) {
        Hang Hang = hangRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dữ liệu"));
        if (Hang.getTrangThai() != null && Hang.getTrangThai()) {
            boolean isUsed = sanPhamRepository.existsByHang_IdAndTrangThaiTrue(id);
            if (isUsed) {
                throw new IllegalArgumentException("Không thể ngừng hoạt động! Đang có sản phẩm sử dụng hãng này đang được bày bán.");
            }
        }
        Hang.setTrangThai(!Hang.getTrangThai());
        hangRepository.save(Hang);
    }
}