package com.example.bee.controllers.api;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation; // BẮT BUỘC IMPORT CÁI NÀY
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadApi {

    private final Cloudinary cloudinary;

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", "File trống!"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", "Chỉ chấp nhận file ảnh!"));
        }

        try {
            // Tích hợp Transformation bóp dung lượng ảnh ngay lúc upload
            Map result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "beemate/products",
                            "resource_type", "image",
                            // Ép Cloudinary tự động giảm chất lượng và chuyển định dạng nhẹ nhất (WebP/AVIF)
                            "transformation", new Transformation().quality("auto").fetchFormat("auto")
                    )
            );

            String url = (String) result.get("secure_url");
            return ResponseEntity.ok(Collections.singletonMap("url", url));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("message", "Upload thất bại: " + e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteImage(@RequestParam("url") String url) {
        try {
            String publicId = extractPublicId(url);
            if (publicId == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "URL không hợp lệ"));
            }

            // Bắn lệnh tiêu hủy lên Cloudinary
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

            return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa ảnh trên mây thành công"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("message", "Lỗi xóa ảnh: " + e.getMessage()));
        }
    }

    // --- HÀM TOOL: BÓC TÁCH PUBLIC_ID TỪ URL ---
    private String extractPublicId(String url) {
        try {
            // Cắt chuỗi từ đoạn /upload/
            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;

            String afterUpload = parts[1];

            // Xóa mảng version (ví dụ: v1712345678/) nếu có
            if (afterUpload.matches("^v\\d+/.*")) {
                afterUpload = afterUpload.replaceFirst("^v\\d+/", "");
            }

            // Xóa đuôi mở rộng (ví dụ: .jpg, .png, .webp)
            int lastDotIndex = afterUpload.lastIndexOf(".");
            if (lastDotIndex != -1) {
                afterUpload = afterUpload.substring(0, lastDotIndex);
            }

            return afterUpload;
        } catch (Exception e) {
            return null;
        }
    }
}