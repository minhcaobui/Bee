package com.example.bee.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {
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

}
