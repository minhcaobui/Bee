package com.example.bee.controllers.admin;

import com.example.bee.entities.staff.NhanVien;
import com.example.bee.repositories.staff.NhanVienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final NhanVienRepository nhanVienRepository;

    @GetMapping("/admin")
    public String showAdminLayout() {
        return "layout/admin-layout";
    }

    @GetMapping("/catalogs")
    public String catalogs() {
        return "admin/catalog/catalogs";
    }

    @GetMapping("/products")
    public String products() {
        return "admin/product/products";
    }

    @GetMapping("/pos")
    public String pos() {
        return "admin/sale/pos";
    }

    @GetMapping("/orders")
    public String orders() {
        return "admin/order/orders";
    }

    @GetMapping("/promotions")
    public String promotions() {
        return "admin/promotion/promotions";
    }

    @GetMapping("/dashboards")
    public String dashboards() {
        return "admin/dashboard/dashboards";
    }

    @GetMapping("/customers")
    public String customers() {
        return "admin/customer/customers";
    }

    @GetMapping("/profiles")
    public String profiles() {
        return "admin/profile/profiles";
    }

    @GetMapping("/reviews")
    public String reviews() {
        return "admin/reviews";
    }

    @GetMapping("/returns")
    public String returns() {
        return "admin/order/doi-tra";
    }

    @GetMapping("/staff")
    public String staffPage(Model model) {
        List<NhanVien> list = nhanVienRepository.getAllNhanVienCustom();
        model.addAttribute("list", list);
        return "admin/staff/staff";
    }

}
