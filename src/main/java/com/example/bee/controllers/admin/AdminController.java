package com.example.bee.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // Dùng @Controller để trả về View (HTML)
@RequestMapping("/admin")
public class AdminController {
    @GetMapping
    public String showAdminLayout() {
        return "admin/admin-layout";
    }
}
