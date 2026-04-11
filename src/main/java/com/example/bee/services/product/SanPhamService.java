package com.example.bee.services;

import com.example.bee.dtos.SanPhamRequest;
import com.example.bee.dtos.VariantRequest;
import com.example.bee.entities.product.HinhAnhSanPham;
import com.example.bee.entities.product.SanPham;
import com.example.bee.entities.product.SanPhamChiTiet;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.repositories.catalog.*;
import com.example.bee.repositories.products.SanPhamChiTietRepository;
import com.example.bee.repositories.products.SanPhamRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SanPhamService {

    private final SanPhamRepository sanPhamrepo;
    private final DanhMucRepository danhMucRepo;
    private final HangRepository hangRepo;
    private final ChatLieuRepository chatLieuRepo;
    private final SanPhamChiTietRepository variantRepo;
    private final MauSacRepository mauSacRepo;
    private final KichThuocRepository kichThuocRepo;
    private final KhuyenMaiRepository khuyenMaiRepo;

    public Page<SanPham> layDanhSachSanPham(String q, Boolean trangThai, Integer idDanhMuc, Integer idHang, Integer idChatLieu, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<SanPham> pageResult = sanPhamrepo.search(q, trangThai, idDanhMuc, idHang, idChatLieu, pageable);

        for (SanPham sp : pageResult.getContent()) {
            tinhGiaKhuyenMai(sp);
        }
        return pageResult;
    }

    public SanPham layChiTietSanPham(Integer id) {
        SanPham sp = sanPhamrepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
        tinhGiaKhuyenMai(sp);
        return sp;
    }

    @Transactional
    public SanPham taoMoiSanPham(SanPhamRequest body) {
        SanPham sp = new SanPham();

        if (isBlank(body.getMa())) {
            sp.setMa(taoMaTuDong());
        } else {
            String ma = body.getMa().trim().toUpperCase();
            if (sanPhamrepo.existsByMaIgnoreCase(ma)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã sản phẩm này đã tồn tại!");
            }
            sp.setMa(ma);
        }

        kiemTraHopLeSanPham(body, sp.getMa(), null);
        mapCommonFields(sp, body);
        return sanPhamrepo.save(sp);
    }

    @Transactional
    public SanPham capNhatSanPham(Integer id, SanPhamRequest body) {
        SanPham sp = sanPhamrepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));

        kiemTraHopLeSanPham(body, sp.getMa(), id);
        mapCommonFields(sp, body);
        return sanPhamrepo.save(sp);
    }

    @Transactional
    public Map<String, String> doiTrangThaiSanPham(Integer id) {
        SanPham sp = sanPhamrepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
        sp.setTrangThai(!sp.getTrangThai());
        sanPhamrepo.save(sp);
        return Collections.singletonMap("message", "Đã " + (sp.getTrangThai() ? "mở bán" : "ngừng bán") + " sản phẩm!");
    }

    public List<SanPhamChiTiet> layDanhSachBienThe(Integer id) {
        List<SanPhamChiTiet> variants = variantRepo.findBySanPhamId(id);
        List<KhuyenMai> activeSales = khuyenMaiRepo.findActiveKhuyenMaiBySanPhamId(id);

        for (SanPhamChiTiet spct : variants) {
            tinhGiaChoBienThe(spct, activeSales);
        }
        return variants;
    }

    @Transactional
    public SanPhamChiTiet taoMoiBienThe(Integer productId, VariantRequest req) {
        SanPham sp = sanPhamrepo.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm gốc"));

        if (variantRepo.existsBySanPhamIdAndMauSacIdAndKichThuocId(productId, req.getIdMauSac(), req.getIdKichThuoc())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Biến thể Màu + Size này đã tồn tại!");
        }

        SanPhamChiTiet variant = new SanPhamChiTiet();
        variant.setSanPham(sp);

        var mauSac = mauSacRepo.findById(req.getIdMauSac())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Màu sắc không hợp lệ"));
        var kichThuoc = kichThuocRepo.findById(req.getIdKichThuoc())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích thước không hợp lệ"));

        variant.setMauSac(mauSac);
        variant.setKichThuoc(kichThuoc);
        variant.setGiaBan(req.getGiaBan());
        variant.setHinhAnh(req.getHinhAnh());

        int soLuongNhap = (req.getSoLuong() != null) ? req.getSoLuong() : 0;
        variant.setSoLuong(soLuongNhap);
        variant.setTrangThai(soLuongNhap > 0);

        String skuCode = String.format("%s-%s-%s", sp.getMa(), mauSac.getMa(), kichThuoc.getMa());
        if (variantRepo.existsBySku(skuCode)) {
            skuCode += "-" + System.currentTimeMillis();
        }
        variant.setSku(skuCode);

        return variantRepo.save(variant);
    }

    @Transactional
    public Map<String, String> capNhatBienThe(Integer id, VariantRequest req) {
        SanPhamChiTiet variant = variantRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể"));

        Integer currentMauId = variant.getMauSac() != null ? variant.getMauSac().getId() : null;
        Integer currentSizeId = variant.getKichThuoc() != null ? variant.getKichThuoc().getId() : null;

        boolean isChangingAttr = false;
        if (req.getIdMauSac() != null && !req.getIdMauSac().equals(currentMauId)) isChangingAttr = true;
        if (req.getIdKichThuoc() != null && !req.getIdKichThuoc().equals(currentSizeId)) isChangingAttr = true;

        if (isChangingAttr && variantRepo.existsBySanPhamIdAndMauSacIdAndKichThuocId(
                variant.getSanPham().getId(), req.getIdMauSac(), req.getIdKichThuoc())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Biến thể Màu + Size này đã tồn tại!");
        }

        if (req.getGiaBan() != null) {
            if (req.getGiaBan().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá bán không được nhỏ hơn 0!");
            }
            variant.setGiaBan(req.getGiaBan());
        }

        if (req.getIdMauSac() != null) variant.setMauSac(mauSacRepo.findById(req.getIdMauSac()).orElse(variant.getMauSac()));
        if (req.getIdKichThuoc() != null) variant.setKichThuoc(kichThuocRepo.findById(req.getIdKichThuoc()).orElse(variant.getKichThuoc()));
        if (req.getHinhAnh() != null) variant.setHinhAnh(req.getHinhAnh());

        variantRepo.save(variant);
        return Collections.singletonMap("message", "Cập nhật biến thể thành công!");
    }

    @Transactional
    public Map<String, String> capNhatTonKhoBienThe(Integer id, Map<String, Integer> body) {
        SanPhamChiTiet variant = variantRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không thấy biến thể"));

        Integer newQty = body.get("soLuong");
        if (newQty == null || newQty < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng tồn kho không được nhỏ hơn 0!");
        }

        variant.setSoLuong(newQty);
        if (newQty > 0 && (variant.getTrangThai() == null || !variant.getTrangThai())) {
            variant.setTrangThai(true);
        } else if (newQty == 0 && (variant.getTrangThai() == null || variant.getTrangThai())) {
            variant.setTrangThai(false);
        }
        variantRepo.save(variant);
        return Collections.singletonMap("message", "Cập nhật kho và trạng thái thành công!");
    }

    @Transactional
    public Map<String, String> doiTrangThaiBienThe(Integer id) {
        SanPhamChiTiet variant = variantRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không thấy biến thể"));
        variant.setTrangThai(!variant.getTrangThai());
        variantRepo.save(variant);
        return Collections.singletonMap("message", "Biến thể [" + variant.getSku() + "] hiện " + (variant.getTrangThai() ? "đang kinh doanh" : "ngừng kinh doanh"));
    }

    // --- CÁC HÀM TIỆN ÍCH (PRIVATE) ---

    private void tinhGiaKhuyenMai(SanPham sp) {
        List<KhuyenMai> activeSales = khuyenMaiRepo.findActiveKhuyenMaiBySanPhamId(sp.getId());
        if (sp.getChiTietSanPhams() != null) {
            for (SanPhamChiTiet spct : sp.getChiTietSanPhams()) {
                tinhGiaChoBienThe(spct, activeSales);
            }
        }
    }

    private void tinhGiaChoBienThe(SanPhamChiTiet spct, List<KhuyenMai> activeSales) {
        BigDecimal giaGoc = spct.getGiaBan() == null ? BigDecimal.ZERO : spct.getGiaBan();
        BigDecimal giaSauKM = giaGoc;

        if (activeSales != null && !activeSales.isEmpty()) {
            KhuyenMai km = activeSales.get(0);
            if ("PERCENT".equals(km.getLoai())) {
                BigDecimal tyLe = km.getGiaTri().divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                BigDecimal tienGiam = giaGoc.multiply(tyLe).setScale(0, java.math.RoundingMode.HALF_UP);
                giaSauKM = giaGoc.subtract(tienGiam);
            } else if (km.getGiaTri() != null) {
                giaSauKM = giaGoc.subtract(km.getGiaTri());
            }
        }
        if (giaSauKM.compareTo(BigDecimal.ZERO) < 0) giaSauKM = BigDecimal.ZERO;
        spct.setGiaSauKhuyenMai(giaSauKM);
    }

    private void kiemTraHopLeSanPham(SanPhamRequest body, String ma, Integer idSkip) {
        String ten = body.getTen().trim();
        if (ten.length() > 255) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên sản phẩm tối đa 255 ký tự!");
        if (ma != null && ma.length() > 50) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã sản phẩm tối đa 50 ký tự!");

        boolean exists = (idSkip == null)
                ? sanPhamrepo.existsByTenIgnoreCaseAndDanhMuc_IdAndHang_IdAndChatLieu_Id(ten, body.getIdDanhMuc(), body.getIdHang(), body.getIdChatLieu())
                : sanPhamrepo.existsByTenIgnoreCaseAndDanhMuc_IdAndHang_IdAndChatLieu_IdAndIdNot(ten, body.getIdDanhMuc(), body.getIdHang(), body.getIdChatLieu(), idSkip);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sản phẩm với Tên, Danh mục, Hãng và Chất liệu này đã tồn tại!");
        }
    }

    private void mapCommonFields(SanPham sp, SanPhamRequest body) {
        sp.setTen(body.getTen().trim());
        sp.setMoTa(safeTrim(body.getMoTa()));
        sp.setTrangThai(body.getTrangThai() != null ? body.getTrangThai() : true);
        if (sp.getId() == null) sp.setNgayTao(new Date());
        else sp.setNgaySua(new Date());

        sp.setDanhMuc(body.getIdDanhMuc() != null ? danhMucRepo.findById(body.getIdDanhMuc()).orElse(null) : null);
        sp.setHang(body.getIdHang() != null ? hangRepo.findById(body.getIdHang()).orElse(null) : null);
        sp.setChatLieu(body.getIdChatLieu() != null ? chatLieuRepo.findById(body.getIdChatLieu()).orElse(null) : null);

        if (body.getDanhSachHinhAnh() != null) {
            if (sp.getHinhAnhs() == null) sp.setHinhAnhs(new ArrayList<>());
            else sp.getHinhAnhs().clear();

            for (String path : body.getDanhSachHinhAnh()) {
                HinhAnhSanPham ha = new HinhAnhSanPham();
                ha.setUrl(path);
                ha.setSanPham(sp);
                sp.getHinhAnhs().add(ha);
            }
        }
    }

    private String taoMaTuDong() {
        return "SP" + System.currentTimeMillis();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}