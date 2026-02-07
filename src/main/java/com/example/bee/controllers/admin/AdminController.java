package com.example.bee.controllers.admin;

import com.example.bee.entities.staff.NV;
import com.example.bee.repositories.staff.NVRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller // Dùng @Controller để trả về View (HTML)
//@RequestMapping("/admin")
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
    @GetMapping("/staff")
    public String staffPage(Model model) {
        List<NV> list = nvRepo.getAllNhanVienCustom();
        model.addAttribute("list", list);
        return "admin/staff/staff";
    }

//    @GetMapping("/products")
//    public String products() {
//        return "admin/product/products";
//    }
}
