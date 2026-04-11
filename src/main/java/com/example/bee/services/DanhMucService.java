package com.example.bee.services;

import com.example.bee.entities.catalog.DanhMuc;
import com.example.bee.repositories.catalog.DanhMucRepository;
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
public class DanhMucService {

    private final DanhMucRepository danhMucRepository;
    private final SanPhamRepository sanPhamRepository;

    private String generateMa() {
        String timeStr = String.valueOf(System.currentTimeMillis());
        return "DM" + timeStr.substring(timeStr.length() - 5);
    }

    public Page<DanhMuc> list(String q, Boolean trangThai, Pageable pageable) {
        return danhMucRepository.search(q, trangThai, pageable);
    }

    public List<DanhMuc> getAllActive() {
        return danhMucRepository.findByTrangThaiTrue();
    }

    public DanhMuc getById(Integer id) {
        return danhMucRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dữ liệu"));
    }

    @Transactional
    public DanhMuc create(DanhMuc body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty())
                ? generateMa()
                : body.getMa().trim().toUpperCase();

        if (ma.length() > 20) throw new IllegalArgumentException("Mã thuộc tính tối đa 20 ký tự!");
        if (!ma.matches("^[A-Z0-9_]*$")) throw new IllegalArgumentException("Mã chỉ được chứa chữ hoa, số và dấu gạch dưới (_)");
        if (ten.isEmpty()) throw new IllegalArgumentException("Tên thuộc tính không được để trống!");
        if (ten.length() > 100) throw new IllegalArgumentException("Tên thuộc tính tối đa 100 ký tự!");
        if (danhMucRepository.existsByTenIgnoreCase(ten)) throw new IllegalArgumentException("Tên danh mục này đã tồn tại!");
        if (danhMucRepository.existsByMaIgnoreCase(ma)) throw new IllegalArgumentException("Mã danh mục này đã tồn tại!");

        DanhMuc entity = new DanhMuc();
        entity.setMa(ma);
        entity.setTen(ten);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());
        return danhMucRepository.save(entity);
    }

    @Transactional
    public DanhMuc update(Integer id, DanhMuc body) {
        DanhMuc entity = danhMucRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dữ liệu"));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";
        if (newTen.isEmpty()) throw new IllegalArgumentException("Tên không được để trống!");
        if (newTen.length() > 100) throw new IllegalArgumentException("Tên tối đa 100 ký tự!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && danhMucRepository.existsByTenIgnoreCase(newTen)) {
            throw new IllegalArgumentException("Tên này đã tồn tại ở bản ghi khác!");
        }

        Boolean newTrangThai = body.getTrangThai();
        if (newTrangThai != null && !newTrangThai && Boolean.TRUE.equals(entity.getTrangThai())) {
            boolean isUsed = sanPhamRepository.existsByDanhMuc_IdAndTrangThaiTrue(id);
            if (isUsed) {
                throw new IllegalArgumentException("Không thể ngừng hoạt động! Đang có sản phẩm thuộc danh mục này đang được bày bán.");
            }
        }

        entity.setTen(newTen);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(newTrangThai != null ? newTrangThai : entity.getTrangThai());
        entity.setNgaySua(LocalDateTime.now());
        return danhMucRepository.save(entity);
    }

    @Transactional
    public void toggleStatus(Integer id) {
        DanhMuc DanhMuc = danhMucRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dữ liệu"));
        if (DanhMuc.getTrangThai() != null && DanhMuc.getTrangThai()) {
            boolean isUsed = sanPhamRepository.existsByDanhMuc_IdAndTrangThaiTrue(id);
            if (isUsed) {
                throw new IllegalArgumentException("Không thể ngừng hoạt động! Đang có sản phẩm sử dụng danh mục này đang được bày bán.");
            }
        }
        DanhMuc.setTrangThai(!DanhMuc.getTrangThai());
        danhMucRepository.save(DanhMuc);
    }
}