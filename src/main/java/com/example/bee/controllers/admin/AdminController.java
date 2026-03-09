package com.example.bee.controllers.admin;

import com.example.bee.entities.staff.NV;
import com.example.bee.repositories.staff.NVRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminController {
    @Autowired
    private NVRepository nvRepo;
    @GetMapping("/admin")
    public String showAdminLayout() {
        return "admin/admin-layout";
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

    @GetMapping("/staff")
    public String staffPage(Model model) {
        List<NV> list = nvRepo.getAllNhanVienCustom();
        model.addAttribute("list", list);
        return "admin/staff/staff";
    }

}
