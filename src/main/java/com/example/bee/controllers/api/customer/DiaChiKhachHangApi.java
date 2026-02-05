package com.example.bee.controllers.api.customer;

import com.example.bee.entities.customer.DiaChiKhachHang;
import com.example.bee.repositories.customer.DiaChiKhachHangRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/dia-chi-khach-hang")
@RequiredArgsConstructor
public class DiaChiKhachHangApi {

    private final DiaChiKhachHangRepository repo;

    @GetMapping("/khach-hang/{khachHangChiTietId}")
    public List<DiaChiKhachHang> getByKhachHang(@PathVariable Integer khachHangChiTietId) {
        return repo.findByKhachHangChiTietIdAndTrangThaiTrue(khachHangChiTietId);
    }

    @GetMapping("/{id}")
    public DiaChiKhachHang getById(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"));
    }

    @PostMapping
    public DiaChiKhachHang create(@Valid @RequestBody DiaChiKhachHang body) {
        if (body.getKhachHangChiTiet() == null || body.getKhachHangChiTiet().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phải cung cấp thông tin khách hàng chi tiết (id)");
        }

        if (body.getDiaChiChiTiet() == null || body.getDiaChiChiTiet().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ chi tiết không được để trống");
        }

        DiaChiKhachHang entity = new DiaChiKhachHang();
        entity.setKhachHangChiTiet(body.getKhachHangChiTiet());
        entity.setHoTenNhan(body.getHoTenNhan() != null ? body.getHoTenNhan().trim() : null);
        entity.setSdtNhan(body.getSdtNhan() != null ? body.getSdtNhan().trim() : null);
        entity.setDiaChiChiTiet(body.getDiaChiChiTiet().trim());
        entity.setPhuongXa(body.getPhuongXa() != null ? body.getPhuongXa().trim() : null);
        entity.setQuanHuyen(body.getQuanHuyen() != null ? body.getQuanHuyen().trim() : null);
        entity.setTinhThanhPho(body.getTinhThanhPho() != null ? body.getTinhThanhPho().trim() : null);
        entity.setLoaiDiaChi(body.getLoaiDiaChi() != null && !body.getLoaiDiaChi().trim().isEmpty()
                ? body.getLoaiDiaChi().trim()
                : "Nhà riêng");
        entity.setLaMacDinh(body.getLaMacDinh() != null ? body.getLaMacDinh() : false);
        entity.setTrangThai(true);
        entity.setNgayTao(LocalDateTime.now());

        if (entity.getLaMacDinh()) {
            repo.updateLaMacDinhToFalseByKhachHangChiTietId(
                    entity.getKhachHangChiTiet().getId(),
                    entity.getId()
            );
        }

        return repo.save(entity);
    }

    @PutMapping("/{id}")
    public DiaChiKhachHang update(@PathVariable Integer id, @Valid @RequestBody DiaChiKhachHang body) {
        DiaChiKhachHang entity = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"));

        entity.setHoTenNhan(body.getHoTenNhan() != null ? body.getHoTenNhan().trim() : entity.getHoTenNhan());
        entity.setSdtNhan(body.getSdtNhan() != null ? body.getSdtNhan().trim() : entity.getSdtNhan());
        entity.setDiaChiChiTiet(body.getDiaChiChiTiet() != null && !body.getDiaChiChiTiet().trim().isEmpty()
                ? body.getDiaChiChiTiet().trim()
                : entity.getDiaChiChiTiet());
        entity.setPhuongXa(body.getPhuongXa() != null ? body.getPhuongXa().trim() : entity.getPhuongXa());
        entity.setQuanHuyen(body.getQuanHuyen() != null ? body.getQuanHuyen().trim() : entity.getQuanHuyen());
        entity.setTinhThanhPho(body.getTinhThanhPho() != null ? body.getTinhThanhPho().trim() : entity.getTinhThanhPho());
        entity.setLoaiDiaChi(body.getLoaiDiaChi() != null && !body.getLoaiDiaChi().trim().isEmpty()
                ? body.getLoaiDiaChi().trim()
                : entity.getLoaiDiaChi());

        Boolean newMacDinh = body.getLaMacDinh() != null ? body.getLaMacDinh() : entity.getLaMacDinh();
        entity.setLaMacDinh(newMacDinh);
        entity.setNgaySua(LocalDateTime.now());

        if (newMacDinh && !entity.getLaMacDinh()) {
            repo.updateLaMacDinhToFalseByKhachHangChiTietId(
                    entity.getKhachHangChiTiet().getId(),
                    entity.getId()
            );
        }

        return repo.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ để xóa");
        }

        repo.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}