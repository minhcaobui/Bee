package com.example.bee.services;

import com.example.bee.entities.catalog.ChatLieu;
import com.example.bee.repositories.catalog.ChatLieuRepository;
import com.example.bee.repositories.products.SanPhamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatLieuService {

    private final ChatLieuRepository chatLieuRepository;
    private final SanPhamRepository sanPhamRepository;

    private String taoMaTuDong() {
        String timeStr = String.valueOf(System.currentTimeMillis());
        return "CL" + timeStr.substring(timeStr.length() - 5);
    }

    public Page<ChatLieu> layDanhSach(String tuKhoa, Boolean trangThai, Pageable pageable) {
        return chatLieuRepository.search(tuKhoa, trangThai, pageable);
    }

    public List<ChatLieu> layTatCaHoatDong() {
        return chatLieuRepository.findByTrangThaiTrue();
    }

    public ChatLieu layChiTiet(Integer id) {
        return chatLieuRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dữ liệu"));
    }

    @Transactional
    public ChatLieu taoMoi(ChatLieu duLieu) {
        String ten = duLieu.getTen() != null ? duLieu.getTen().trim() : "";
        String ma = (duLieu.getMa() == null || duLieu.getMa().trim().isEmpty())
                ? taoMaTuDong()
                : duLieu.getMa().trim().toUpperCase();

        if (ma.length() > 20) throw new IllegalArgumentException("Mã thuộc tính tối đa 20 ký tự!");
        if (!ma.matches("^[A-Z0-9_]*$")) throw new IllegalArgumentException("Mã chỉ được chứa chữ hoa, số và dấu gạch dưới (_)");
        if (ten.isEmpty()) throw new IllegalArgumentException("Tên thuộc tính không được để trống!");
        if (ten.length() > 100) throw new IllegalArgumentException("Tên thuộc tính tối đa 100 ký tự!");
        if (chatLieuRepository.existsByTenIgnoreCase(ten)) throw new IllegalArgumentException("Tên chất liệu này đã tồn tại!");
        if (chatLieuRepository.existsByMaIgnoreCase(ma)) throw new IllegalArgumentException("Mã chất liệu này đã tồn tại!");

        ChatLieu entity = new ChatLieu();
        entity.setMa(ma);
        entity.setTen(ten);
        if (duLieu.getMoTa() != null) entity.setMoTa(duLieu.getMoTa().trim());
        entity.setTrangThai(duLieu.getTrangThai() != null ? duLieu.getTrangThai() : true);
        entity.setNgayTao(LocalDateTime.now());
        return chatLieuRepository.save(entity);
    }

    @Transactional
    public ChatLieu capNhat(Integer id, ChatLieu duLieu) {
        ChatLieu entity = chatLieuRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dữ liệu"));
        String newTen = duLieu.getTen() != null ? duLieu.getTen().trim() : "";
        if (newTen.isEmpty()) throw new IllegalArgumentException("Tên không được để trống!");
        if (newTen.length() > 100) throw new IllegalArgumentException("Tên tối đa 100 ký tự!");
        if (!entity.getTen().equalsIgnoreCase(newTen) && chatLieuRepository.existsByTenIgnoreCase(newTen)) {
            throw new IllegalArgumentException("Tên này đã tồn tại ở bản ghi khác!");
        }

        Boolean newTrangThai = duLieu.getTrangThai();
        if (newTrangThai != null && !newTrangThai && Boolean.TRUE.equals(entity.getTrangThai())) {
            boolean isUsed = sanPhamRepository.existsByChatLieu_IdAndTrangThaiTrue(id);
            if (isUsed) {
                throw new IllegalArgumentException("Không thể ngừng hoạt động! Đang có sản phẩm thuộc chất liệu này đang được bày bán.");
            }
        }

        entity.setTen(newTen);
        if (duLieu.getMoTa() != null) entity.setMoTa(duLieu.getMoTa().trim());
        entity.setTrangThai(newTrangThai != null ? newTrangThai : entity.getTrangThai());
        entity.setNgaySua(LocalDateTime.now());
        return chatLieuRepository.save(entity);
    }

    @Transactional
    public void doiTrangThai(Integer id) {
        ChatLieu chatLieu = chatLieuRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dữ liệu"));
        if (chatLieu.getTrangThai() != null && chatLieu.getTrangThai()) {
            boolean isUsed = sanPhamRepository.existsByChatLieu_IdAndTrangThaiTrue(id);
            if (isUsed) {
                throw new IllegalArgumentException("Không thể ngừng hoạt động! Đang có sản phẩm sử dụng chất liệu này đang được bày bán.");
            }
        }
        chatLieu.setTrangThai(!chatLieu.getTrangThai());
        chatLieuRepository.save(chatLieu);
    }
}