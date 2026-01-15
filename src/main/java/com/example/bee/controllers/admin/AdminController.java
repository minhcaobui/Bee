package com.example.bee.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // Dùng @Controller để trả về View (HTML)
//@RequestMapping("/admin")
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
}
