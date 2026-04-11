package com.example.bee.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final Cloudinary cloudinary;

    public ResponseEntity<?> upload(MultipartFile file) {
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
            Map result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "beemate/products",
                            "resource_type", "image",
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

    public ResponseEntity<?> deleteImage(String url) {
        try {
            String publicId = extractPublicId(url);
            if (publicId == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "URL không hợp lệ"));
            }
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa ảnh trên mây thành công"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("message", "Lỗi xóa ảnh: " + e.getMessage()));
        }
    }

    private String extractPublicId(String url) {
        try {
            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;
            String afterUpload = parts[1];
            if (afterUpload.matches("^v\\d+/.*")) {
                afterUpload = afterUpload.replaceFirst("^v\\d+/", "");
            }
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