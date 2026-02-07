package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.MauSac;
import com.example.bee.repositories.catalog.MauSacRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Random;

@RestController
@RequestMapping("/api/mau-sac")
@RequiredArgsConstructor
public class MauSacApi {
    @Autowired
    private MauSacRepository mauSacRepository;

    @Autowired
    private KhuyenMaiRepository khuyenMaiRepo;

    // --- GEN MÃ MÀU (MS_ + 6 số) ---
    private String generateMa() {
        String ma;
        Random random = new Random();
        do {
            int randomNum = 1000 + random.nextInt(9000);
            ma = "MS" + randomNum;
        } while (mauSacRepository.existsByMaIgnoreCase(ma));
        return ma;
    }

    @GetMapping
    public Page<MauSac> list(@RequestParam(required = false) String q,
                             @RequestParam(required = false) Boolean trangThai,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return mauSacRepository.search(q, trangThai, pageable);
    }

    @GetMapping("/{id}")
    public MauSac getDetail(@PathVariable Integer id) {
        return mauSacRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu!"));
    }

    @PostMapping
    public ResponseEntity<MauSac> create(@Valid @RequestBody MauSac body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty())
                ? generateMa()
                : body.getMa().trim().toUpperCase();

        if (ma.length() > 20 || !ma.matches("^[A-Z0-9_]*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã max 20, cho phép dấu '_'");
        }

        if (ten.isEmpty() || ten.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên màu max 100!");

        if (mauSacRepository.existsByTenIgnoreCase(ten)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Màu này có rồi!");
        if (mauSacRepository.existsByMaIgnoreCase(ma)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã màu bị trùng!");

        MauSac entity = new MauSac();
        entity.setMa(ma);
        entity.setTen(ten);
        // Đã xóa ngày tạo, ngày sửa
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);

        return ResponseEntity.ok(mauSacRepository.save(entity));
    }

    @PutMapping("/{id}")
    public MauSac update(@PathVariable Integer id, @Valid @RequestBody MauSac body) {
        MauSac entity = mauSacRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";

        if (newTen.isEmpty() || newTen.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên màu max 100!");

        if (!entity.getTen().equalsIgnoreCase(newTen) && mauSacRepository.existsByTenIgnoreCase(newTen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trùng tên màu!");
        }

        entity.setTen(newTen);
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : entity.getTrangThai());
        // Đã xóa ngày sửa, người sửa

        return mauSacRepository.save(entity);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id) {
        MauSac mauSac = mauSacRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Cứ thế mà đổi thôi, không sợ bố con thằng nào cả :))
        mauSac.setTrangThai(!mauSac.getTrangThai());

        mauSacRepository.save(mauSac);
        return ResponseEntity.ok().build();
    }
}