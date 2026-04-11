package com.example.bee.controllers.api;

import com.example.bee.services.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tai-len")
@RequiredArgsConstructor
public class UploadApi {

    private final UploadService uploadService;

    @PostMapping
    public ResponseEntity<?> taiLen(@RequestParam("file") MultipartFile file) {
        return uploadService.taiLen(file);
    }

    @DeleteMapping
    public ResponseEntity<?> xoaAnh(@RequestParam("url") String url) {
        return uploadService.xoaAnh(url);
    }
}