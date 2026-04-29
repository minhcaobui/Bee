package com.example.bee.configs;

import com.example.bee.entities.account.TaiKhoan;
import com.example.bee.entities.account.VaiTro;
import com.example.bee.entities.cart.GioHang;
import com.example.bee.entities.order.TrangThaiHoaDon;
import com.example.bee.entities.staff.ChucVu;
import com.example.bee.entities.staff.NhanVien;
import com.example.bee.repositories.account.TaiKhoanRepository;
import com.example.bee.repositories.account.VaiTroRepository;
import com.example.bee.repositories.cart.GioHangRepository;
import com.example.bee.repositories.staff.ChucVuRepository;
import com.example.bee.repositories.staff.NhanVienRepository;
import com.example.bee.repositories.order.TrangThaiHoaDonRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final VaiTroRepository vaiTroRepository;
    private final ChucVuRepository chucVuRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final GioHangRepository gioHangRepository;
    private final TrangThaiHoaDonRepository trangThaiHoaDonRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("⏳ Đang kiểm tra và khởi tạo dữ liệu mầm (Seed Data)...");

        // 1. KHỞI TẠO VAI TRÒ (ROLE) - Cập nhật đủ 3 vai trò theo DB
        VaiTro roleAdmin = vaiTroRepository.findById(1).orElseGet(() -> {
            VaiTro vt = new VaiTro();
            vt.setId(1);
            vt.setMa("ROLE_ADMIN");
            vt.setTen("Quản trị viên");
            return vaiTroRepository.save(vt);
        });

        VaiTro roleStaff = vaiTroRepository.findById(2).orElseGet(() -> {
            VaiTro vt = new VaiTro();
            vt.setId(2);
            vt.setMa("ROLE_STAFF");
            vt.setTen("Nhân viên");
            return vaiTroRepository.save(vt);
        });

        vaiTroRepository.findById(3).orElseGet(() -> {
            VaiTro vt = new VaiTro();
            vt.setId(3);
            vt.setMa("ROLE_CUSTOMER");
            vt.setTen("Khách hàng");
            return vaiTroRepository.save(vt);
        });

        // 2. KHỞI TẠO CHỨC VỤ MẶC ĐỊNH - Cập nhật tên và mô tả khớp với DB
        ChucVu cvAdmin = chucVuRepository.findById(1).orElseGet(() -> {
            ChucVu cv = new ChucVu();
            cv.setId(1);
            cv.setMa("QUAN_LY");
            cv.setTen("Quản lý");
            cv.setMoTa(null); // Trùng khớp với ảnh DB
            return chucVuRepository.save(cv);
        });

        chucVuRepository.findById(2).orElseGet(() -> {
            ChucVu cv = new ChucVu();
            cv.setId(2);
            cv.setMa("NHAN_VIEN_BAN_HANG");
            cv.setTen("Nhân viên bán hàng");
            cv.setMoTa(null); // Trùng khớp với ảnh DB
            return chucVuRepository.save(cv);
        });

        // 3. KHỞI TẠO TRẠNG THÁI HÓA ĐƠN - Đủ 11 trạng thái như trong ảnh
        if (trangThaiHoaDonRepository.count() == 0) {
            List<TrangThaiHoaDon> danhSachTrangThai = Arrays.asList(
                    new TrangThaiHoaDon(1, "CHO_XAC_NHAN", "Chờ xác nhận"),
                    new TrangThaiHoaDon(2, "CHO_THANH_TOAN", "Chờ thanh toán"),
                    new TrangThaiHoaDon(3, "DA_XAC_NHAN", "Đã xác nhận / Đang xử lý"),
                    new TrangThaiHoaDon(4, "CHO_GIAO_VAN_CHUYEN", "Chờ giao cho đơn vị vận chuyển"),
                    new TrangThaiHoaDon(5, "DANG_GIAO_HANG", "Đang giao hàng"),
                    new TrangThaiHoaDon(6, "CHO_KHACH_LAY", "Chờ khách đến lấy (Tại cửa hàng)"),
                    new TrangThaiHoaDon(7, "HOAN_THANH", "Hoàn thành"),
                    new TrangThaiHoaDon(8, "DA_HUY", "Đã hủy"),
                    new TrangThaiHoaDon(9, "GIAO_THAT_BAI", "Giao hàng thất bại"),
                    new TrangThaiHoaDon(10, "DANG_HOAN_HANG", "Đang hoàn hàng về kho"),
                    new TrangThaiHoaDon(11, "DA_HOAN_HANG", "Đã nhận lại hàng hoàn")
            );
            trangThaiHoaDonRepository.saveAll(danhSachTrangThai);
            System.out.println("✅ Đã khởi tạo 11 trạng thái hóa đơn cơ bản.");
        }

        // 4. KHỞI TẠO TÀI KHOẢN ADMIN VÀ HỒ SƠ NHÂN VIÊN MẶC ĐỊNH
        String adminEmail = "admin@shop.com";
        if (taiKhoanRepository.findByTenDangNhap(adminEmail).isEmpty()) {

            // 4.1 Tạo tài khoản xác thực
            TaiKhoan adminAccount = new TaiKhoan();
            adminAccount.setTenDangNhap(adminEmail);
            adminAccount.setMatKhau(passwordEncoder.encode("12345678")); // Mật khẩu mặc định
            adminAccount.setTrangThai(true);
            adminAccount.setVaiTro(roleAdmin);
            adminAccount.setDaDoiTenDangNhap(true); // Khóa quyền đổi tên đăng nhập
            TaiKhoan savedAdminAccount = taiKhoanRepository.save(adminAccount);

            // 4.2 Tạo giỏ hàng trống gắn liền với tài khoản
            GioHang gioHang = new GioHang();
            gioHang.setTaiKhoan(savedAdminAccount);
            gioHangRepository.save(gioHang);

            // 4.3 Tạo hồ sơ nhân viên
            NhanVien adminProfile = new NhanVien();
            adminProfile.setMa("ADMIN");
            adminProfile.setHoTen("Quản Trị Viên Tối Cao");
            adminProfile.setEmail(adminEmail);
            adminProfile.setSoDienThoai("0999999999");
            adminProfile.setNgaySinh(java.sql.Date.valueOf(LocalDate.of(2000, 1, 1)));
            adminProfile.setGioiTinh("Nam");
            adminProfile.setDiaChi("Trụ sở chính BEEMATE");
            adminProfile.setTrangThai(true);
            adminProfile.setChucVu(cvAdmin);
            adminProfile.setTaiKhoan(savedAdminAccount);

            nhanVienRepository.save(adminProfile);

            System.out.println("==================================================");
            System.out.println("🔥 KHỞI TẠO THÀNH CÔNG TÀI KHOẢN ADMIN MẶC ĐỊNH 🔥");
            System.out.println("Tên đăng nhập: " + adminEmail);
            System.out.println("Mật khẩu:      12345678");
            System.out.println("==================================================");
        } else {
            System.out.println("⚡ Dữ liệu mầm đã tồn tại, bỏ qua bước khởi tạo.");
        }
    }
}