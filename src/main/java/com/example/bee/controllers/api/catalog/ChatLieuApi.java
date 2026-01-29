package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.repositories.catalog.ChatLieuRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/chat-lieu")
@RequiredArgsConstructor
public class ChatLieuApi {
    private final ChatLieuRepository repo;
    private static final String MA_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RAND = new SecureRandom();

    private String generateMa() {
        String ma;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(MA_CHARS.charAt(RAND.nextInt(MA_CHARS.length())));
            ma = sb.toString();
        } while (repo.existsByMaIgnoreCase(ma));
        return ma;
    }

    // --- PHÂN TRANG CHẤT LIỆU ---
    @GetMapping
    public Page<ChatLieu> list(@RequestParam(required = false) String q,
                               @RequestParam(required = false) Boolean trangThai,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return repo.search(q, trangThai, pageable);
    }

    @GetMapping("/{id}")
    public ChatLieu getById(@PathVariable Integer id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<ChatLieu> create(@Valid @RequestBody ChatLieu body) {
        String ten = body.getTen() != null ? body.getTen().trim() : "";
        String ma = (body.getMa() == null || body.getMa().trim().isEmpty()) ? generateMa() : body.getMa().trim().toUpperCase();

        if (ma.length() > 20 || !ma.matches("^[A-Z0-9]*$")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã max 20, không tiếng Việt!");
        if (ten.isEmpty() || ten.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên max 100 chữ!");
        if (repo.existsByTenIgnoreCase(ten)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Chất liệu này có rồi!");

        ChatLieu entity = new ChatLieu();
        entity.setMa(ma);
        entity.setTen(ten);
        entity.setMoTa(body.getMoTa());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());
        return ResponseEntity.ok(repo.save(entity));
    }

    @PutMapping("/{id}")
    public ChatLieu update(@PathVariable Integer id, @Valid @RequestBody ChatLieu body) {
        ChatLieu entity = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newTen = body.getTen() != null ? body.getTen().trim() : "";

        if (newTen.isEmpty() || newTen.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên max 100 chữ!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && repo.existsByTenIgnoreCase(newTen)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Trùng tên chất liệu!");

        entity.setTen(newTen);
        entity.setMoTa(body.getMoTa());
        entity.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : entity.getTrangThai());
        entity.setNgaySua(LocalDateTime.now());
        return repo.save(entity);
    }
}