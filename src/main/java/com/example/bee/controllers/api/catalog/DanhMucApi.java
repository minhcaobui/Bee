package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.DanhMuc;
import com.example.bee.repositories.catalog.DanhMucRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Random;

@RestController
@RequestMapping("/api/danh-muc")
@RequiredArgsConstructor
public class DanhMucApi {

    @Autowired
    private final DanhMucRepository danhMucRepository;
    @Autowired
    private final KhuyenMaiRepository khuyenMaiRepository;

    // Đã xóa 2 dòng static final thừa thãi đi rồi nhé

    private String generateMa() {
        String ma;
        Random random = new Random();
        do {
            int randomNum = 1000 + random.nextInt(9000);
            ma = "DM" + randomNum;
        } while (danhMucRepository.existsByMaIgnoreCase(ma));
        return ma;
    }

    @GetMapping
    public Page<DanhMuc> list(@RequestParam(required = false) String q,
                              @RequestParam(required = false) Boolean trangThai,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return danhMucRepository.search(q, trangThai, pageable);
    }

    @GetMapping("/{id}")
    public DanhMuc getById(@PathVariable Integer id) {
        return danhMucRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DanhMuc body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty())
                ? generateMa()
                : body.getMa().trim().toUpperCase();

        // 1. Check độ dài & Regex (PHẢI THÊM DẤU _ VÀO ĐÂY)
        if (ma.length() > 20) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã tối đa 20 ký tự thôi cu!");

        // SỬA LẠI REGEX: Thêm dấu _ vào sau số 9
        if (!ma.matches("^[A-Z0-9_]*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã chỉ được chứa chữ hoa, số và dấu gạch dưới (_)");
        }

        // 2. Check độ dài cho TÊN
        if (ten.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên đéo được để trống!");
        if (ten.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên dài quá (max 100), bớt văn vở lại!");

        // 3. Check trùng TÊN & MÃ
        if (danhMucRepository.existsByTenIgnoreCase(ten)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên này có thằng dùng rồi!");
        if (danhMucRepository.existsByMaIgnoreCase(ma)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã này bị trùng rồi!");

        // 4. Save
        DanhMuc entity = new DanhMuc();
        entity.setMa(ma);
        entity.setTen(ten);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());

        return ResponseEntity.ok(danhMucRepository.save(entity));
    }

    @PutMapping("/{id}")
    public DanhMuc update(@PathVariable Integer id, @Valid @RequestBody DanhMuc body) {
        DanhMuc entity = danhMucRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String newTen = body.getTen() != null ? body.getTen().trim() : "";

        if (newTen.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên không được để trống!");
        if (newTen.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tối đa 100 ký tự thôi!");

        if (!entity.getTen().equalsIgnoreCase(newTen) && danhMucRepository.existsByTenIgnoreCase(newTen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên này đã tồn tại ở bản ghi khác!");
        }

        entity.setTen(newTen);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : entity.getTrangThai());

        entity.setNgaySua(LocalDateTime.now());
        return danhMucRepository.save(entity);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id) {
        DanhMuc danhMuc = danhMucRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // LOGIC CHẶN MỚI THÊM:
        // Chỉ chặn khi đang định TẮT (từ true -> false)
        if (danhMuc.getTrangThai()) {
            long conflictCount = khuyenMaiRepository.countByDanhMuc(id);

            if (conflictCount > 0) {
                // Trả về lỗi Bad Request để Frontend hiện Toast đỏ
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không thể tắt danh mục này! Có " + conflictCount + " chương trình Khuyến Mãi đang chạy liên quan đến sản phẩm thuộc danh mục.");
            }
        }

        // Nếu không trùng thì cho tắt bình thường
        danhMuc.setTrangThai(!danhMuc.getTrangThai());
        danhMucRepository.save(danhMuc);

        return ResponseEntity.ok().build();
    }
}