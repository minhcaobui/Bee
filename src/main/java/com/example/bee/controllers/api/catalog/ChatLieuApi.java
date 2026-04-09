package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.repositories.catalog.ChatLieuRepository;
import com.example.bee.repositories.products.SanPhamRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat-lieu")
@RequiredArgsConstructor
public class ChatLieuApi {

    private final ChatLieuRepository chatLieuRepository;
    private final SanPhamRepository sanPhamRepository;

    private String generateMa() {
        return "CL" + System.currentTimeMillis(); // Đổi "CL" thành "HANG", "KT", "MS" tương ứng với từng file
    }

    @GetMapping
    public Page<ChatLieu> list(@RequestParam(required = false) String q,
                               @RequestParam(required = false) Boolean trangThai,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return chatLieuRepository.search(q, trangThai, pageable);
    }

    @GetMapping("/all-active")
    public ResponseEntity<List<ChatLieu>> getAllActive() {
        return ResponseEntity.ok(chatLieuRepository.findByTrangThaiTrue());
    }

    @GetMapping("/{id}")
    public ChatLieu getById(@PathVariable Integer id) {
        return chatLieuRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<ChatLieu> create(@Valid @RequestBody ChatLieu body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty())
                ? generateMa()
                : body.getMa().trim().toUpperCase();

        // 🌟 ĐÃ FIX: Văn phong báo lỗi chuyên nghiệp
        if (ma.length() > 20) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã thuộc tính tối đa 20 ký tự!");
        if (!ma.matches("^[A-Z0-9_]*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã chỉ được chứa chữ hoa, số và dấu gạch dưới (_)");
        }
        if (ten.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên thuộc tính không được để trống!");
        if (ten.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên thuộc tính tối đa 100 ký tự!");
        if (chatLieuRepository.existsByTenIgnoreCase(ten))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên chất liệu này đã tồn tại!");
        if (chatLieuRepository.existsByMaIgnoreCase(ma))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã chất liệu này đã tồn tại!");

        ChatLieu entity = new ChatLieu();
        entity.setMa(ma);
        entity.setTen(ten);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());
        return ResponseEntity.ok(chatLieuRepository.save(entity));
    }

    @PutMapping("/{id}")
    public ChatLieu update(@PathVariable Integer id, @Valid @RequestBody ChatLieu body) {
        ChatLieu entity = chatLieuRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";
        if (newTen.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên không được để trống!");
        if (newTen.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tối đa 100 ký tự!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && chatLieuRepository.existsByTenIgnoreCase(newTen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên này đã tồn tại ở bản ghi khác!");
        }

        // 🌟 ĐÃ FIX BUG LÁCH LUẬT: Check điều kiện khi người dùng gạt tắt trạng thái ở form Edit
        Boolean newTrangThai = body.getTrangThai();
        if (newTrangThai != null && !newTrangThai && Boolean.TRUE.equals(entity.getTrangThai())) {
            boolean isUsed = sanPhamRepository.existsByChatLieu_IdAndTrangThaiTrue(id);
            if (isUsed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể ngừng hoạt động! Đang có sản phẩm thuộc chất liệu này đang được bày bán.");
            }
        }

        entity.setTen(newTen);
        if (body.getMoTa() != null) entity.setMoTa(body.getMoTa().trim());
        entity.setTrangThai(newTrangThai != null ? newTrangThai : entity.getTrangThai());
        entity.setNgaySua(LocalDateTime.now());
        return chatLieuRepository.save(entity);
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id) {
        ChatLieu chatLieu = chatLieuRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (chatLieu.getTrangThai() != null && chatLieu.getTrangThai() == true) {
            boolean isUsed = sanPhamRepository.existsByChatLieu_IdAndTrangThaiTrue(id);
            if (isUsed) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Không thể ngừng hoạt động! Đang có sản phẩm sử dụng chất liệu này đang được bày bán."
                ));
            }
        }
        chatLieu.setTrangThai(!chatLieu.getTrangThai());
        chatLieuRepository.save(chatLieu);
        return ResponseEntity.ok().build();
    }
}