package com.example.bee.controllers.api.catalog;

import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.services.ChatLieuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/chat-lieu")
@RequiredArgsConstructor
public class ChatLieuApi {

    private final ChatLieuService chatLieuService;

    @GetMapping
    public ResponseEntity<?> layDanhSach(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) Boolean trangThai,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(chatLieuService.list(q, trangThai, PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @GetMapping("/hoat-dong")
    public ResponseEntity<?> layTatCaHoatDong() {
        return ResponseEntity.ok(chatLieuService.getAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTiet(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(chatLieuService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> taoMoi(@Valid @RequestBody ChatLieu body) {
        try {
            return ResponseEntity.ok(chatLieuService.create(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(@PathVariable Integer id, @Valid @RequestBody ChatLieu body) {
        try {
            return ResponseEntity.ok(chatLieuService.update(id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable Integer id) {
        try {
            chatLieuService.toggleStatus(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }
}