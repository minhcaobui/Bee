package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.repositories.catalog.ChatLieuRepository;
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

import java.time.LocalDateTime;
import java.util.Random;

@RestController
@RequestMapping("/api/chat-lieu")
@RequiredArgsConstructor
public class ChatLieuApi {

    @Autowired
    private final ChatLieuRepository chatLieuRepository;
    @Autowired
    private final KhuyenMaiRepository khuyenMaiRepository;


    // --- 1. SỬA LẠI LOGIC GEN MÃ (CL_ + SỐ) ---
    private String generateMa() {
        String ma;
        Random random = new Random();
        do {
            int randomNum = 1000 + random.nextInt(9000);
            ma = "CL" + randomNum; // Ví dụ: CL_123456
        } while (chatLieuRepository.existsByMaIgnoreCase(ma));
        return ma;
    }

    @GetMapping
    public Page<ChatLieu> list(@RequestParam(required = false) String q,
                               @RequestParam(required = false) Boolean trangThai,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return chatLieuRepository.search(q, trangThai, pageable);
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

        // --- 2. SỬA REGEX CHO PHÉP DẤU _ ---
        if (ma.length() > 20 || !ma.matches("^[A-Z0-9_]*$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã max 20, không dấu tiếng Việt, cho phép '_'");
        }

        if (ten.isEmpty() || ten.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên max 100 chữ!");

        // --- 3. CHECK TRÙNG CẢ MÃ VÀ TÊN ---
        if (chatLieuRepository.existsByTenIgnoreCase(ten)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên chất liệu này có rồi!");
        if (chatLieuRepository.existsByMaIgnoreCase(ma)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã chất liệu này bị trùng!");

        ChatLieu entity = new ChatLieu();
        entity.setMa(ma);
        entity.setTen(ten);
        entity.setMoTa(body.getMoTa());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());
        return ResponseEntity.ok(chatLieuRepository.save(entity));
    }

    @PutMapping("/{id}")
    public ChatLieu update(@PathVariable Integer id, @Valid @RequestBody ChatLieu body) {
        ChatLieu entity = chatLieuRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";

        if (newTen.isEmpty() || newTen.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên max 100 chữ!");

        if (!entity.getTen().equalsIgnoreCase(newTen) && chatLieuRepository.existsByTenIgnoreCase(newTen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trùng tên chất liệu!");
        }

        entity.setTen(newTen);
        entity.setMoTa(body.getMoTa());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : entity.getTrangThai());
        entity.setNgaySua(LocalDateTime.now());
        return chatLieuRepository.save(entity);
    }
    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id) {
        ChatLieu chatLieu = chatLieuRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Nếu không trùng thì cho tắt bình thường
        chatLieu.setTrangThai(!chatLieu.getTrangThai());
        chatLieuRepository.save(chatLieu);

        return ResponseEntity.ok().build();
    }

}