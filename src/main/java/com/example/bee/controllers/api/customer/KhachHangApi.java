package com.example.bee.controllers.api.customer;

import com.example.bee.dto.KhachHangRequest;
import com.example.bee.entities.customer.KhachHangChiTiet;
import com.example.bee.repositories.customer.KhachHangChiTietRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangApi {

    private final KhachHangChiTietRepository repo;

    private static final String UPLOAD_DIR = "uploads/avatars/";

    @GetMapping
    public Page<KhachHangChiTiet> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return repo.search(q, trangThai, pageable);
    }

    @GetMapping("/{id}")
    public KhachHangChiTiet getById(@PathVariable Integer id) {
        System.out.println("GET /api/khach-hang/" + id);
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KhachHangChiTiet> create(
            @RequestPart("data") String jsonData,
            @RequestPart(value = "hinhAnhFile", required = false) MultipartFile hinhAnhFile) {

        System.out.println("=== POST CREATE KHÁCH HÀNG - JSON + MULTIPART ===");
        System.out.println("jsonData: " + jsonData);
        System.out.println("Có file ảnh? " + (hinhAnhFile != null));
        if (hinhAnhFile != null) {
            System.out.println("Tên file: " + hinhAnhFile.getOriginalFilename());
            System.out.println("Kích thước file: " + hinhAnhFile.getSize() + " bytes");
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());  // <-- thêm dòng này ngay trước readValue

        KhachHangRequest dto;
        try {
            dto = mapper.readValue(jsonData, KhachHangRequest.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu JSON không hợp lệ: " + e.getMessage());
        }

        // Kiểm tra validation thủ công hoặc dùng Validator nếu cần
        if (dto.getHoTen() == null || dto.getHoTen().trim().isEmpty() || dto.getHoTen().length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ tên không được để trống và tối đa 150 ký tự");
        }

        String ma = generateMaKhachHang();

        if (repo.existsByMaIgnoreCase(ma)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã khách hàng đã tồn tại");
        }

        KhachHangChiTiet entity = new KhachHangChiTiet();
        entity.setMa(ma);
        entity.setHoTen(dto.getHoTen().trim());
        entity.setGioiTinh(dto.getGioiTinh());
        entity.setNgaySinh(dto.getNgaySinh());
        entity.setDiaChi(dto.getDiaChi());
        entity.setSoDienThoai(dto.getSoDienThoai() != null ? dto.getSoDienThoai().trim() : null);
        entity.setEmail(dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : null);
        entity.setHinhAnh(dto.getHinhAnh());
        entity.setIdTaiKhoan(dto.getIdTaiKhoan());
        entity.setTrangThai(dto.getTrangThai() != null ? dto.getTrangThai() : true);

        // Xử lý upload ảnh
        if (hinhAnhFile != null && !hinhAnhFile.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + hinhAnhFile.getOriginalFilename();
                Path uploadPath = Paths.get(UPLOAD_DIR + fileName);
                Files.createDirectories(uploadPath.getParent());
                Files.write(uploadPath, hinhAnhFile.getBytes());
                entity.setHinhAnh("/" + UPLOAD_DIR + fileName);
                System.out.println("Lưu ảnh thành công: " + entity.getHinhAnh());
            } catch (IOException e) {
                e.printStackTrace();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi lưu ảnh: " + e.getMessage());
            }
        }

        KhachHangChiTiet saved = repo.save(entity);
        return ResponseEntity.ok(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KhachHangChiTiet> update(
            @PathVariable Integer id,
            @RequestPart("data") String jsonData,
            @RequestPart(value = "hinhAnhFile", required = false) MultipartFile hinhAnhFile) {

        System.out.println("=== PUT UPDATE KHÁCH HÀNG - JSON + MULTIPART ===");
        System.out.println("ID: " + id);
        System.out.println("jsonData: " + jsonData);
        System.out.println("Có file ảnh mới? " + (hinhAnhFile != null));

        KhachHangChiTiet entity = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        KhachHangRequest dto;
        try {
            dto = mapper.readValue(jsonData, KhachHangRequest.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu JSON không hợp lệ: " + e.getMessage());
        }

        if (dto.getHoTen() != null && !dto.getHoTen().trim().isEmpty()) {
            entity.setHoTen(dto.getHoTen().trim());
        }
        if (dto.getGioiTinh() != null) {
            entity.setGioiTinh(dto.getGioiTinh());
        }
        if (dto.getNgaySinh() != null) {
            entity.setNgaySinh(dto.getNgaySinh());
        }
        if (dto.getDiaChi() != null) {
            entity.setDiaChi(dto.getDiaChi());
        }
        if (dto.getSoDienThoai() != null) {
            entity.setSoDienThoai(dto.getSoDienThoai().trim());
        }
        if (dto.getEmail() != null) {
            entity.setEmail(dto.getEmail().trim().toLowerCase());
        }
        if (dto.getHinhAnh() != null) {
            entity.setHinhAnh(dto.getHinhAnh());
        }
        if (dto.getIdTaiKhoan() != null) {
            entity.setIdTaiKhoan(dto.getIdTaiKhoan());
        }
        if (dto.getTrangThai() != null) {
            entity.setTrangThai(dto.getTrangThai());
        }

        // Nếu có ảnh mới thì cập nhật
        if (hinhAnhFile != null && !hinhAnhFile.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + hinhAnhFile.getOriginalFilename();
                Path uploadPath = Paths.get(UPLOAD_DIR + fileName);
                Files.createDirectories(uploadPath.getParent());
                Files.write(uploadPath, hinhAnhFile.getBytes());
                entity.setHinhAnh("/" + UPLOAD_DIR + fileName);
                System.out.println("Cập nhật ảnh thành công: " + entity.getHinhAnh());
            } catch (IOException e) {
                e.printStackTrace();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi lưu ảnh mới: " + e.getMessage());
            }
        }

        KhachHangChiTiet saved = repo.save(entity);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng");
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String generateMaKhachHang() {
        String ma;
        do {
            StringBuilder sb = new StringBuilder("KH");
            for (int i = 0; i < 6; i++) {
                sb.append("0123456789".charAt((int) (Math.random() * 10)));
            }
            ma = sb.toString();
        } while (repo.existsByMaIgnoreCase(ma));
        return ma;
    }

}