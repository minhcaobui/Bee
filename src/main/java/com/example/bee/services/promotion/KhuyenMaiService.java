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

    private void kiemTraChongLo(PromotionRequest yeuCau, List<SanPham> danhSachSanPham) {
        if (yeuCau.getTen() == null || yeuCau.getTen().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên chương trình không được để trống.");
        }
        if (yeuCau.getNgayBatDau() == null || yeuCau.getNgayKetThuc() == null) {
            throw new IllegalArgumentException("Vui lòng thiết lập đầy đủ thời gian bắt đầu và kết thúc.");
        }
        if (yeuCau.getNgayKetThuc().isBefore(yeuCau.getNgayBatDau().plusMinutes(5))) {
            throw new IllegalArgumentException("Thời hạn chương trình phải có độ dài tối thiểu 5 phút.");
        }

        boolean laPhanTram = yeuCau.getLoai() != null &&
                (yeuCau.getLoai().equalsIgnoreCase("PERCENT") || yeuCau.getLoai().contains("%"));

        if (laPhanTram) {
            if (yeuCau.getGiaTri() == null || yeuCau.getGiaTri().compareTo(BigDecimal.ONE) < 0 || yeuCau.getGiaTri().compareTo(new BigDecimal("70")) > 0) {
                throw new IllegalArgumentException("Lỗi: Sale sản phẩm theo % chỉ được cấu hình từ 1% đến tối đa 70% (Quy định chống phá giá).");
            }
        } else {
            if (yeuCau.getGiaTri() == null || yeuCau.getGiaTri().compareTo(new BigDecimal("1000")) < 0) {
                throw new IllegalArgumentException("Mức giảm giá tối thiểu là 1.000 VNĐ.");
            }

            for (SanPham sp : danhSachSanPham) {
                if (sp.getChiTietSanPhams() == null || sp.getChiTietSanPhams().isEmpty()) {
                    throw new IllegalArgumentException("Sản phẩm '" + sp.getTen() + "' chưa có biến thể (SKU) nào nên không có giá gốc để đối chiếu. Vui lòng bổ sung SKU trước khi áp dụng Sale!");
                }

                BigDecimal giaThapNhat = sp.getChiTietSanPhams().stream()
                        .map(ct -> ct.getGiaBan() != null ? ct.getGiaBan() : BigDecimal.ZERO)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                if (giaThapNhat.compareTo(BigDecimal.ZERO) == 0) {
                    throw new IllegalArgumentException("Sản phẩm '" + sp.getTen() + "' đang có biến thể giá 0đ, không thể áp dụng Khuyến mãi tiền mặt.");
                }

                BigDecimal mucGiamToiDaAnToan = giaThapNhat.multiply(new BigDecimal("0.7"));

                if (yeuCau.getGiaTri().compareTo(mucGiamToiDaAnToan) > 0) {
                    throw new IllegalArgumentException("Lỗi rủi ro lỗ vốn: Mức giảm " + String.format("%,.0f", yeuCau.getGiaTri()) + "đ quá lớn so với sản phẩm '" + sp.getTen() +
                            "' (Giá bán thấp nhất: " + String.format("%,.0f", giaThapNhat) + "đ). Chỉ được giảm tối đa 70% giá gốc.");
                }
            }
        }
    }

    private void kiemTraTrungLich(Integer idHienTai, PromotionRequest yeuCau) {
        if (yeuCau.getIdSanPhams() == null || yeuCau.getIdSanPhams().isEmpty()) {
            return;
        }

        Integer idAnToan = (idHienTai == null) ? -1 : idHienTai;
        List<KhuyenMai> danhSachTrungLich = khuyenMaiRepository.checkTrungLich(
                yeuCau.getIdSanPhams(),
                yeuCau.getNgayBatDau(),
                yeuCau.getNgayKetThuc(),
                idAnToan
        );

        if (!danhSachTrungLich.isEmpty()) {
            KhuyenMai km = danhSachTrungLich.get(0);
            String tenSpTrung = "các sản phẩm đã chọn";
            if (km.getSanPhams() != null && !km.getSanPhams().isEmpty()) {
                tenSpTrung = km.getSanPhams().stream()
                        .filter(sp -> yeuCau.getIdSanPhams().contains(sp.getId()))
                        .map(SanPham::getTen)
                        .collect(Collectors.joining(", "));
            }

            throw new IllegalArgumentException("Sản phẩm [" + tenSpTrung + "] đang thuộc đợt Sale: '" + km.getTen() +
                    "'. Một sản phẩm không được phép tham gia 2 đợt Khuyến mãi giảm giá cùng lúc!");
        }
    }

    private boolean kiemTraDaSuDung(Integer idKhuyenMai) {
        return false;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void tuDongCapNhatTrangThai() {
        khuyenMaiRepository.autoDeactivateExpiredPromotions(LocalDateTime.now());
    }

    private String taoMaTuDong() {
        String timeStr = String.valueOf(System.currentTimeMillis());
        return "KM" + timeStr.substring(timeStr.length() - 8);
    }

    public List<Map<String, Object>> layTatCaSanPham() {
        List<SanPham> danhSach = sanPhamRepo.getAllActiveProducts();
        return danhSach.stream().map(sp -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", sp.getId());
            item.put("ma", sp.getMa());
            item.put("ten", sp.getTen());
            return item;
        }).collect(Collectors.toList());
    }

    public Page<KhuyenMai> danhSachKhuyenMai(String tuKhoa, Boolean trangThai, LocalDateTime tuNgay, LocalDateTime denNgay, Pageable pageable) {
        String keyword = (tuKhoa != null && !tuKhoa.trim().isEmpty()) ? tuKhoa.trim() : null;
        return khuyenMaiRepository.searchEverything(keyword, trangThai, tuNgay, denNgay, pageable);
    }

    public Map<String, Object> layChiTiet(Integer id) {
        KhuyenMai khuyenMai = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt khuyến mãi"));
        List<Integer> danhSachIdSanPham = khuyenMaiSanPhamRepository.findAllByIdKhuyenMai(id)
                .stream().map(KhuyenMaiSanPham::getIdSanPham).collect(Collectors.toList());
        Map<String, Object> phanHoi = new HashMap<>();
        phanHoi.put("data", khuyenMai);
        phanHoi.put("productIds", danhSachIdSanPham);
        phanHoi.put("canUpdateStartDate", !kiemTraDaSuDung(id));
        return phanHoi;
    }

    @Transactional
    public KhuyenMai taoMoi(PromotionRequest duLieu) {
        List<SanPham> danhSachSanPham = new ArrayList<>();
        if (duLieu.getIdSanPhams() != null && !duLieu.getIdSanPhams().isEmpty()) {
            danhSachSanPham = sanPhamRepo.findAllById(duLieu.getIdSanPhams());
        }

        kiemTraChongLo(duLieu, danhSachSanPham);
        kiemTraTrungLich(null, duLieu);

        String maKhuyenMai = (duLieu.getMa() != null && !duLieu.getMa().trim().isEmpty())
                ? duLieu.getMa().trim().toUpperCase()
                : taoMaTuDong();
        if (khuyenMaiRepository.existsByMa(maKhuyenMai)) {
            throw new IllegalArgumentException("Mã khuyến mãi đã tồn tại!");
        }

        KhuyenMai entity = new KhuyenMai();
        entity.setMa(maKhuyenMai);
        chuyenDuLieu(duLieu, entity);
        KhuyenMai khuyenMaiDaLuu = khuyenMaiRepository.save(entity);
        luuSanhPhamApDung(khuyenMaiDaLuu.getId(), duLieu.getIdSanPhams());

        try {
            List<com.example.bee.entities.account.TaiKhoan> khachHangs = taiKhoanRepository.findByVaiTro_Ma("ROLE_CUSTOMER");
            if (khachHangs != null && !khachHangs.isEmpty()) {
                List<com.example.bee.entities.notification.ThongBao> thongBaos = new ArrayList<>();
                for (com.example.bee.entities.account.TaiKhoan tk : khachHangs) {
                    com.example.bee.entities.notification.ThongBao tb = new com.example.bee.entities.notification.ThongBao();
                    tb.setTaiKhoanId(tk.getId());
                    tb.setTieuDe("Đợt Sale mới: " + khuyenMaiDaLuu.getTen());
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

        return khuyenMaiDaLuu;
    }

    @Transactional
    public KhuyenMai capNhat(Integer id, PromotionRequest duLieu) {
        KhuyenMai entity = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi"));

        List<SanPham> danhSachSanPham = new ArrayList<>();
        if (duLieu.getIdSanPhams() != null && !duLieu.getIdSanPhams().isEmpty()) {
            danhSachSanPham = sanPhamRepo.findAllById(duLieu.getIdSanPhams());
        }

        kiemTraChongLo(duLieu, danhSachSanPham);
        kiemTraTrungLich(id, duLieu);

        if (entity.getNgayBatDau().isBefore(LocalDateTime.now())) {
            if (!entity.getNgayBatDau().isEqual(duLieu.getNgayBatDau())) {
                throw new IllegalArgumentException("Chương trình đã hoặc đang diễn ra, không thể thay đổi Ngày Bắt Đầu!");
            }
        }

        chuyenDuLieu(duLieu, entity);
        KhuyenMai khuyenMaiDaLuu = khuyenMaiRepository.save(entity);
        khuyenMaiSanPhamRepository.deleteByIdKhuyenMai(id);
        luuSanhPhamApDung(khuyenMaiDaLuu.getId(), duLieu.getIdSanPhams());
        return khuyenMaiDaLuu;
    }

    private void chuyenDuLieu(PromotionRequest tuRequest, KhuyenMai sangEntity) {
        sangEntity.setTen(tuRequest.getTen().trim());
        sangEntity.setLoai(tuRequest.getLoai());
        sangEntity.setGiaTri(tuRequest.getGiaTri());
        sangEntity.setNgayBatDau(tuRequest.getNgayBatDau());
        sangEntity.setNgayKetThuc(tuRequest.getNgayKetThuc());
        sangEntity.setChoPhepCongDon(tuRequest.getChoPhepCongDon());
        sangEntity.setTrangThai(tuRequest.getTrangThai());
    }

    private void luuSanhPhamApDung(Integer idKhuyenMai, List<Integer> danhSachIdSanPham) {
        if (danhSachIdSanPham == null || danhSachIdSanPham.isEmpty()) return;
        List<Integer> danhSachIdHopLe = sanPhamRepo.findAllById(danhSachIdSanPham).stream()
                .map(SanPham::getId).collect(Collectors.toList());

        List<KhuyenMaiSanPham> danhSachKM = danhSachIdHopLe.stream()
                .map(idSP -> new KhuyenMaiSanPham(null, idKhuyenMai, idSP))
                .collect(Collectors.toList());
        if (!danhSachKM.isEmpty()) khuyenMaiSanPhamRepository.saveAll(danhSachKM);
    }

    @Transactional
    public String doiTrangThaiNhanh(Integer id) {
        KhuyenMai entity = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin chương trình yêu cầu."));
        boolean trangThaiMoi = !entity.getTrangThai();
        String thongBaoGhiNhan;

        if (trangThaiMoi) {
            if (entity.getNgayKetThuc().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Không thể kích hoạt chương trình đã quá hạn thời gian kết thúc.");
            }
            if (entity.getSanPhams() == null || entity.getSanPhams().isEmpty()) {
                throw new IllegalArgumentException("Kích hoạt thất bại: Đợt khuyến mãi hiện chưa có sản phẩm áp dụng. Vui lòng bổ sung sản phẩm trước khi vận hành.");
            }
            long soSanPhamHopLe = khuyenMaiRepository.countValidProductsInPromotion(id);
            long tongSoSanPham = entity.getSanPhams().size();
            if (soSanPhamHopLe == 0) {
                throw new IllegalArgumentException("Kích hoạt thất bại. Toàn bộ sản phẩm được chọn hiện đang ở trạng thái ngừng hoạt động.");
            }
            if (soSanPhamHopLe < tongSoSanPham) {
                long chenhLech = tongSoSanPham - soSanPhamHopLe;
                thongBaoGhiNhan = "Kích hoạt thành công. Lưu ý: Có " + chenhLech + " sản phẩm đang ngừng kinh doanh sẽ không được áp dụng chiết khấu.";
            } else {
                thongBaoGhiNhan = "Chiến dịch khuyến mãi đã được kích hoạt thành công trên toàn bộ danh mục sản phẩm.";
            }

            PromotionRequest yeuCauGiaLap = new PromotionRequest();
            yeuCauGiaLap.setTen(entity.getTen());
            yeuCauGiaLap.setIdSanPhams(entity.getSanPhams().stream().map(SanPham::getId).collect(Collectors.toList()));
            yeuCauGiaLap.setNgayBatDau(entity.getNgayBatDau());
            yeuCauGiaLap.setNgayKetThuc(entity.getNgayKetThuc());
            yeuCauGiaLap.setChoPhepCongDon(entity.getChoPhepCongDon());
            yeuCauGiaLap.setTrangThai(true);
            yeuCauGiaLap.setLoai(entity.getLoai());
            yeuCauGiaLap.setGiaTri(entity.getGiaTri());

            kiemTraChongLo(yeuCauGiaLap, entity.getSanPhams());
            kiemTraTrungLich(id, yeuCauGiaLap);
        } else {
            thongBaoGhiNhan = "Chương trình đã được chuyển sang trạng thái ngừng áp dụng.";
        }
        entity.setTrangThai(trangThaiMoi);
        khuyenMaiRepository.save(entity);
        return thongBaoGhiNhan;
    }
}