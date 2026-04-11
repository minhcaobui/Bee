package com.example.bee.services;

import com.example.bee.dtos.PromotionRequest;
import com.example.bee.entities.product.SanPham;
import com.example.bee.entities.promotion.KhuyenMai;
import com.example.bee.entities.promotion.KhuyenMaiSanPham;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.notification.ThongBaoRepository;
import com.example.bee.repositories.products.SanPhamRepository;
import com.example.bee.repositories.promotion.KhuyenMaiRepository;
import com.example.bee.repositories.promotion.KhuyenMaiSanPhamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KhuyenMaiService {

    private final KhuyenMaiRepository khuyenMaiRepository;
    private final KhuyenMaiSanPhamRepository khuyenMaiSanPhamRepository;
    private final SanPhamRepository sanPhamRepo;
    private final ThongBaoRepository thongBaoRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    private void validateChongLo(PromotionRequest req, List<SanPham> danhSachSanPham) {
        if (req.getTen() == null || req.getTen().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên chương trình không được để trống.");
        }
        if (req.getNgayBatDau() == null || req.getNgayKetThuc() == null) {
            throw new IllegalArgumentException("Vui lòng thiết lập đầy đủ thời gian bắt đầu và kết thúc.");
        }
        if (req.getNgayKetThuc().isBefore(req.getNgayBatDau().plusMinutes(5))) {
            throw new IllegalArgumentException("Thời hạn chương trình phải có độ dài tối thiểu 5 phút.");
        }

        boolean isPercent = req.getLoai() != null &&
                (req.getLoai().equalsIgnoreCase("PERCENT") || req.getLoai().contains("%"));

        if (isPercent) {
            if (req.getGiaTri() == null || req.getGiaTri().compareTo(BigDecimal.ONE) < 0 || req.getGiaTri().compareTo(new BigDecimal("70")) > 0) {
                throw new IllegalArgumentException("Lỗi: Sale sản phẩm theo % chỉ được cấu hình từ 1% đến tối đa 70% (Quy định chống phá giá).");
            }
        } else {
            if (req.getGiaTri() == null || req.getGiaTri().compareTo(new BigDecimal("1000")) < 0) {
                throw new IllegalArgumentException("Mức giảm giá tối thiểu là 1.000 VNĐ.");
            }

            for (SanPham sp : danhSachSanPham) {
                if (sp.getChiTietSanPhams() == null || sp.getChiTietSanPhams().isEmpty()) {
                    throw new IllegalArgumentException("Sản phẩm '" + sp.getTen() + "' chưa có biến thể (SKU) nào nên không có giá gốc để đối chiếu. Vui lòng bổ sung SKU trước khi áp dụng Sale!");
                }

                BigDecimal minPrice = sp.getChiTietSanPhams().stream()
                        .map(ct -> ct.getGiaBan() != null ? ct.getGiaBan() : BigDecimal.ZERO)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                if (minPrice.compareTo(BigDecimal.ZERO) == 0) {
                    throw new IllegalArgumentException("Sản phẩm '" + sp.getTen() + "' đang có biến thể giá 0đ, không thể áp dụng Khuyến mãi tiền mặt.");
                }

                BigDecimal maxSafeDiscount = minPrice.multiply(new BigDecimal("0.7"));

                if (req.getGiaTri().compareTo(maxSafeDiscount) > 0) {
                    throw new IllegalArgumentException("Lỗi rủi ro lỗ vốn: Mức giảm " + String.format("%,.0f", req.getGiaTri()) + "đ quá lớn so với sản phẩm '" + sp.getTen() +
                            "' (Giá bán thấp nhất: " + String.format("%,.0f", minPrice) + "đ). Chỉ được giảm tối đa 70% giá gốc.");
                }
            }
        }
    }

    private void validateConflict(Integer currentId, PromotionRequest request) {
        if (request.getIdSanPhams() == null || request.getIdSanPhams().isEmpty()) {
            return;
        }

        Integer safeId = (currentId == null) ? -1 : currentId;
        List<KhuyenMai> conflicts = khuyenMaiRepository.checkTrungLich(
                request.getIdSanPhams(),
                request.getNgayBatDau(),
                request.getNgayKetThuc(),
                safeId
        );

        if (!conflicts.isEmpty()) {
            KhuyenMai km = conflicts.get(0);
            String tenSpTrung = "các sản phẩm đã chọn";
            if (km.getSanPhams() != null && !km.getSanPhams().isEmpty()) {
                tenSpTrung = km.getSanPhams().stream()
                        .filter(sp -> request.getIdSanPhams().contains(sp.getId()))
                        .map(SanPham::getTen)
                        .collect(Collectors.joining(", "));
            }

            throw new IllegalArgumentException("Sản phẩm [" + tenSpTrung + "] đang thuộc đợt Sale: '" + km.getTen() +
                    "'. Một sản phẩm không được phép tham gia 2 đợt Khuyến mãi giảm giá cùng lúc!");
        }
    }

    private boolean checkDaSuDung(Integer khuyenMaiId) {
        return false;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoUpdateStatus() {
        khuyenMaiRepository.autoDeactivateExpiredPromotions(LocalDateTime.now());
    }

    private String generateCode() {
        String timeStr = String.valueOf(System.currentTimeMillis());
        return "KM" + timeStr.substring(timeStr.length() - 8);
    }

    public List<Map<String, Object>> getAllProducts() {
        List<SanPham> list = sanPhamRepo.getAllActiveProducts();
        return list.stream().map(sp -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", sp.getId());
            item.put("ma", sp.getMa());
            item.put("ten", sp.getTen());
            return item;
        }).collect(Collectors.toList());
    }

    public Page<KhuyenMai> promoList(String q, Boolean trangThai, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        String keyword = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        return khuyenMaiRepository.searchEverything(keyword, trangThai, from, to, pageable);
    }

    public Map<String, Object> getById(Integer id) {
        KhuyenMai khuyenMai = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt khuyến mãi"));
        List<Integer> productIds = khuyenMaiSanPhamRepository.findAllByIdKhuyenMai(id)
                .stream().map(KhuyenMaiSanPham::getIdSanPham).collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("data", khuyenMai);
        response.put("productIds", productIds);
        response.put("canUpdateStartDate", !checkDaSuDung(id));
        return response;
    }

    @Transactional
    public KhuyenMai create(PromotionRequest body) {
        List<SanPham> danhSachSanPham = new ArrayList<>();
        if (body.getIdSanPhams() != null && !body.getIdSanPhams().isEmpty()) {
            danhSachSanPham = sanPhamRepo.findAllById(body.getIdSanPhams());
        }

        validateChongLo(body, danhSachSanPham);
        validateConflict(null, body);

        String code = (body.getMa() != null && !body.getMa().trim().isEmpty())
                ? body.getMa().trim().toUpperCase()
                : generateCode();
        if (khuyenMaiRepository.existsByMa(code)) {
            throw new IllegalArgumentException("Mã khuyến mãi đã tồn tại!");
        }

        KhuyenMai entity = new KhuyenMai();
        entity.setMa(code);
        mapToEntity(body, entity);
        KhuyenMai saved = khuyenMaiRepository.save(entity);
        saveProducts(saved.getId(), body.getIdSanPhams());

        try {
            List<com.example.bee.entities.account.TaiKhoan> khachHangs = taiKhoanRepository.findByVaiTro_Ma("ROLE_CUSTOMER");
            if (khachHangs != null && !khachHangs.isEmpty()) {
                List<com.example.bee.entities.notification.ThongBao> thongBaos = new ArrayList<>();
                for (com.example.bee.entities.account.TaiKhoan tk : khachHangs) {
                    com.example.bee.entities.notification.ThongBao tb = new com.example.bee.entities.notification.ThongBao();
                    tb.setTaiKhoanId(tk.getId());
                    tb.setTieuDe("Đợt Sale mới: " + saved.getTen());
                    tb.setNoiDung("Chương trình khuyến mãi giảm giá các mặt hàng đã bắt đầu. Nhanh tay săn sale kẻo lỡ!");
                    tb.setLoaiThongBao("VOUCHER");
                    tb.setDaDoc(false);
                    tb.setDaXoa(false);
                    thongBaos.add(tb);
                }
                thongBaoRepository.saveAll(thongBaos);
            }
        } catch (Exception e) {
            System.out.println("Lỗi gửi thông báo Sale: " + e.getMessage());
        }

        return saved;
    }

    @Transactional
    public KhuyenMai update(Integer id, PromotionRequest body) {
        KhuyenMai entity = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi"));

        List<SanPham> danhSachSanPham = new ArrayList<>();
        if (body.getIdSanPhams() != null && !body.getIdSanPhams().isEmpty()) {
            danhSachSanPham = sanPhamRepo.findAllById(body.getIdSanPhams());
        }

        validateChongLo(body, danhSachSanPham);
        validateConflict(id, body);

        if (entity.getNgayBatDau().isBefore(LocalDateTime.now())) {
            if (!entity.getNgayBatDau().isEqual(body.getNgayBatDau())) {
                throw new IllegalArgumentException("Chương trình đã hoặc đang diễn ra, không thể thay đổi Ngày Bắt Đầu!");
            }
        }

        mapToEntity(body, entity);
        KhuyenMai saved = khuyenMaiRepository.save(entity);
        khuyenMaiSanPhamRepository.deleteByIdKhuyenMai(id);
        saveProducts(saved.getId(), body.getIdSanPhams());
        return saved;
    }

    private void mapToEntity(PromotionRequest dto, KhuyenMai entity) {
        entity.setTen(dto.getTen().trim());
        entity.setLoai(dto.getLoai());
        entity.setGiaTri(dto.getGiaTri());
        entity.setNgayBatDau(dto.getNgayBatDau());
        entity.setNgayKetThuc(dto.getNgayKetThuc());
        entity.setChoPhepCongDon(dto.getChoPhepCongDon());
        entity.setTrangThai(dto.getTrangThai());
    }

    private void saveProducts(Integer kmId, List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        List<Integer> validIds = sanPhamRepo.findAllById(productIds).stream()
                .map(SanPham::getId).collect(Collectors.toList());

        List<KhuyenMaiSanPham> list = validIds.stream()
                .map(spId -> new KhuyenMaiSanPham(null, kmId, spId))
                .collect(Collectors.toList());
        if (!list.isEmpty()) khuyenMaiSanPhamRepository.saveAll(list);
    }

    @Transactional
    public String quickToggleStatus(Integer id) {
        KhuyenMai entity = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin chương trình yêu cầu."));
        boolean newStatus = !entity.getTrangThai();
        String message;

        if (newStatus) {
            if (entity.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Không thể kích hoạt chương trình đã quá hạn thời gian kết thúc.");
            }
            if (entity.getSanPhams() == null || entity.getSanPhams().isEmpty()) {
                throw new IllegalArgumentException("Kích hoạt thất bại: Đợt khuyến mãi hiện chưa có sản phẩm áp dụng. Vui lòng bổ sung sản phẩm trước khi vận hành.");
            }
            long validCount = khuyenMaiRepository.countValidProductsInPromotion(id);
            long totalCount = entity.getSanPhams().size();
            if (validCount == 0) {
                throw new IllegalArgumentException("Kích hoạt thất bại. Toàn bộ sản phẩm được chọn hiện đang ở trạng thái ngừng hoạt động.");
            }
            if (validCount < totalCount) {
                long diff = totalCount - validCount;
                message = "Kích hoạt thành công. Lưu ý: Có " + diff + " sản phẩm đang ngừng kinh doanh sẽ không được áp dụng chiết khấu.";
            } else {
                message = "Chiến dịch khuyến mãi đã được kích hoạt thành công trên toàn bộ danh mục sản phẩm.";
            }

            PromotionRequest fakeRequest = new PromotionRequest();
            fakeRequest.setTen(entity.getTen());
            fakeRequest.setIdSanPhams(entity.getSanPhams().stream().map(SanPham::getId).collect(Collectors.toList()));
            fakeRequest.setNgayBatDau(entity.getNgayBatDau());
            fakeRequest.setNgayKetThuc(entity.getNgayKetThuc());
            fakeRequest.setChoPhepCongDon(entity.getChoPhepCongDon());
            fakeRequest.setTrangThai(true);
            fakeRequest.setLoai(entity.getLoai());
            fakeRequest.setGiaTri(entity.getGiaTri());

            validateChongLo(fakeRequest, entity.getSanPhams());
            validateConflict(id, fakeRequest);
        } else {
            message = "Chương trình đã được chuyển sang trạng thái ngừng áp dụng.";
        }
        entity.setTrangThai(newStatus);
        khuyenMaiRepository.save(entity);
        return message;
    }
}