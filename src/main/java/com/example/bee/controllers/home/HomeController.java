package com.example.bee.controllers.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Tự động chuyển hướng khách vào trang đăng nhập khi gõ domain gốc
        return "redirect:/login";

        // (Hoặc nếu bạn có trang chủ cho khách mua hàng, bạn đổi thành: return "redirect:/trang-chu";)
    }
}