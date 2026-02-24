package com.example.bee.controllers.api.customer;

import com.example.bee.dto.DiaChiRequest;
import com.example.bee.dto.KhachHangRequest;
import com.example.bee.entities.customer.DiaChiKhachHang;
import com.example.bee.entities.customer.KhachHang;
import com.example.bee.repositories.customer.DiaChiKhachHangRepository;
import com.example.bee.repositories.customer.KhachHangRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangApi {

    private final KhachHangRepository khRepo;
    private final DiaChiKhachHangRepository dcRepo;

    private static final SecureRandom RAND = new SecureRandom();

    // --- HÀM TIỆN ÍCH ---
    private String generateMa() {
        String ma;
        do {
            StringBuilder sb = new StringBuilder("KH");
            for (int i = 0; i < 6; i++) sb.append(RAND.nextInt(10));
            ma = sb.toString();
        } while (khRepo.existsByMaIgnoreCase(ma));
        return ma;
    }

    // ================== API KHÁCH HÀNG ==================

    @GetMapping
    public Page<KhachHang> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return khRepo.search(q, trangThai, pageable);
    }

    @GetMapping("/{id}")
    public KhachHang getDetail(@PathVariable Integer id) {
        return khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<KhachHang> create(@RequestBody KhachHangRequest req) {

        // 1. Validate họ tên
        String ten = req.getHoTen() != null ? req.getHoTen().trim() : "";
        if (ten.isEmpty() || ten.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ tên không được để trống và tối đa 100 ký tự");
        }

        // 2. Validate SĐT
        String sdt = req.getSoDienThoai() != null ? req.getSoDienThoai().trim() : "";
        if (sdt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không được để trống");
        }
        if (!sdt.matches("^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không đúng định dạng");
        }
        if (khRepo.existsBySoDienThoai(sdt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại đã tồn tại trong hệ thống");
        }

        // 3. Validate Email (Thêm check format cùi bắp)
        String email = req.getEmail() != null ? req.getEmail().trim() : "";
        if (!email.isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không đúng định dạng");
            }
            if (khRepo.existsByEmail(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại trong hệ thống");
            }
        }

        // 4. Lưu Khách Hàng
        KhachHang kh = new KhachHang();
        kh.setMa(generateMa());
        kh.setHoTen(ten);
        kh.setGioiTinh(req.getGioiTinh() != null && req.getGioiTinh() ? "Nam" : "Nữ");
        kh.setNgaySinh(req.getNgaySinh());
        kh.setSoDienThoai(sdt);
        kh.setEmail(email.isEmpty() ? null : email);
        kh.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : true);
        if (req.getHinhAnh() != null) kh.setHinhAnh(req.getHinhAnh());

        // 5. XỬ LÝ ĐỊA CHỈ (CHỐNG LƯU RÁC)
        String tinh = req.getTinhThanhPho() != null ? req.getTinhThanhPho().trim() : "";
        String huyen = req.getQuanHuyen() != null ? req.getQuanHuyen().trim() : "";
        String xa = req.getPhuongXa() != null ? req.getPhuongXa().trim() : "";
        String chiTiet = req.getDiaChiChiTiet() != null ? req.getDiaChiChiTiet().trim() : "";

        // Xác định xem nó có thật sự nhập địa chỉ đàng hoàng không (bỏ qua mấy cái mặc định của frontend)
        boolean hasRealAddress = !tinh.isEmpty() && !tinh.equals("Chưa cập nhật") && !tinh.equals("null");

        if (hasRealAddress) {
            String fullAddress = String.format("%s, %s, %s, %s", chiTiet, xa, huyen, tinh);
            kh.setDiaChi(fullAddress);
        } else {
            kh.setDiaChi("Khách lẻ / Mua tại cửa hàng");
        }

        KhachHang savedKh = khRepo.save(kh);

        // 6. CHỈ LƯU SỔ ĐỊA CHỈ NẾU CÓ ĐỊA CHỈ THẬT
        if (hasRealAddress) {
            DiaChiKhachHang dc = new DiaChiKhachHang();
            dc.setKhachHang(savedKh);
            dc.setHoTenNhan(ten);
            dc.setSdtNhan(sdt);
            dc.setDiaChiChiTiet(chiTiet);
            dc.setPhuongXa(xa);
            dc.setQuanHuyen(huyen);
            dc.setTinhThanhPho(tinh);
            dc.setLoaiDiaChi("Nhà riêng");
            dc.setLaMacDinh(true);
            dc.setTrangThai(true);
            dcRepo.save(dc);
        }

        return ResponseEntity.ok(savedKh);
    }

    @PutMapping("/{id}")
    public ResponseEntity<KhachHang> update(@PathVariable Integer id, @RequestBody KhachHangRequest req) {

        KhachHang kh = khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        // 1. Validate họ tên (nếu có gửi lên)
        if (req.getHoTen() != null) {
            String ten = req.getHoTen().trim();
            if (ten.isEmpty() || ten.length() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ tên không được để trống và tối đa 100 ký tự");
            }
            kh.setHoTen(ten);
        }

        if (req.getGioiTinh() != null) kh.setGioiTinh(req.getGioiTinh() ? "Nam" : "Nữ");
        if (req.getNgaySinh() != null) kh.setNgaySinh(req.getNgaySinh());

        // 2. Validate SĐT y như lúc Thêm Mới
        if (req.getSoDienThoai() != null) {
            String sdtNew = req.getSoDienThoai().trim();
            if (sdtNew.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không được để trống");
            }
            if (!sdtNew.matches("^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không đúng định dạng");
            }
            // Chú ý dùng existsBySoDienThoaiAndIdNot để nó bỏ qua chính cái thằng đang sửa
            if (khRepo.existsBySoDienThoaiAndIdNot(sdtNew, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng bởi khách hàng khác");
            }
            kh.setSoDienThoai(sdtNew);
        }

        // 3. Validate Email
        if (req.getEmail() != null) {
            String emailNew = req.getEmail().trim();
            if (!emailNew.isEmpty()) {
                if (!emailNew.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không đúng định dạng");
                }
                if (khRepo.existsByEmailAndIdNot(emailNew, id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng bởi khách hàng khác");
                }
                kh.setEmail(emailNew);
            } else {
                kh.setEmail(null); // Nó bỏ trống thì lưu null
            }
        }

        if (req.getTrangThai() != null) kh.setTrangThai(req.getTrangThai());

        // Nhận URL ảnh từ Cloudinary
        if (req.getHinhAnh() != null) kh.setHinhAnh(req.getHinhAnh());

        return ResponseEntity.ok(khRepo.save(kh));
    }

    // ================== API ĐỊA CHỈ ==================

    @GetMapping("/{id}/dia-chi")
    public ResponseEntity<?> getAddressList(@PathVariable Integer id) {
        if (!khRepo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok(dcRepo.findByKhachHangId(id));
    }

    @PostMapping("/{id}/dia-chi")
    public ResponseEntity<?> addAddress(@PathVariable Integer id, @RequestBody DiaChiRequest req) {
        KhachHang kh = khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        DiaChiKhachHang dc = new DiaChiKhachHang();
        dc.setKhachHang(kh);
        dc.setDiaChiChiTiet(req.getDiaChiChiTiet());
        dc.setPhuongXa(req.getPhuongXa());
        dc.setQuanHuyen(req.getQuanHuyen());
        dc.setTinhThanhPho(req.getTinhThanhPho());
        dc.setHoTenNhan(req.getHoTenNhan() != null ? req.getHoTenNhan() : kh.getHoTen());
        dc.setSdtNhan(req.getSdtNhan() != null ? req.getSdtNhan() : kh.getSoDienThoai());
        dc.setLoaiDiaChi("Khác");
        dc.setLaMacDinh(false);
        dc.setTrangThai(true);

        return ResponseEntity.ok(dcRepo.save(dc));
    }

    @DeleteMapping("/dia-chi/{idDiaChi}")
    public ResponseEntity<?> deleteAddress(@PathVariable Integer idDiaChi) {
        DiaChiKhachHang dc = dcRepo.findById(idDiaChi)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (dc.getLaMacDinh()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa địa chỉ mặc định");
        }
        dcRepo.delete(dc);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/dia-chi/{idDiaChi}/mac-dinh")
    @Transactional
    public ResponseEntity<?> setDefaultAddress(@PathVariable Integer idDiaChi) {
        DiaChiKhachHang newDefault = dcRepo.findById(idDiaChi)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Integer idKhach = newDefault.getKhachHang().getId();
        dcRepo.findByKhachHangIdAndLaMacDinhTrue(idKhach).ifPresent(old -> {
            old.setLaMacDinh(false);
            dcRepo.save(old);
        });

        newDefault.setLaMacDinh(true);
        return ResponseEntity.ok(dcRepo.save(newDefault));
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> quickToggleStatus(@PathVariable Integer id) {
        KhachHang kh = khRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khách hàng không tồn tại"));

        kh.setTrangThai(!kh.getTrangThai());
        return ResponseEntity.ok(khRepo.save(kh));
    }
}