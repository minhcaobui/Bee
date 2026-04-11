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

    public ResponseEntity<?> taiLen(MultipartFile tep) {
        if (tep.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", "File trống!"));
        }
        String loaiTep = tep.getContentType();
        if (loaiTep == null || !loaiTep.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", "Chỉ chấp nhận file ảnh!"));
        }
        try {
            Map ketQua = cloudinary.uploader().upload(
                    tep.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "beemate/products",
                            "resource_type", "image",
                            "transformation", new Transformation().quality("auto").fetchFormat("auto")
                    )
            );

            String duongDan = (String) ketQua.get("secure_url");
            return ResponseEntity.ok(Collections.singletonMap("url", duongDan));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("message", "Upload thất bại: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> xoaAnh(String duongDan) {
        try {
            String idCongKhai = tachPublicId(duongDan);
            if (idCongKhai == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "URL không hợp lệ"));
            }
            cloudinary.uploader().destroy(idCongKhai, ObjectUtils.emptyMap());
            return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa ảnh trên mây thành công"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("message", "Lỗi xóa ảnh: " + e.getMessage()));
        }
    }

    private String tachPublicId(String duongDan) {
        try {
            String[] cacPhan = duongDan.split("/upload/");
            if (cacPhan.length < 2) return null;
            String sauUpload = cacPhan[1];
            if (sauUpload.matches("^v\\d+/.*")) {
                sauUpload = sauUpload.replaceFirst("^v\\d+/", "");
            }
            int viTriChamCuoi = sauUpload.lastIndexOf(".");
            if (viTriChamCuoi != -1) {
                sauUpload = sauUpload.substring(0, viTriChamCuoi);
            }
            return sauUpload;
        } catch (Exception e) {
            return null;
        }
    }
}