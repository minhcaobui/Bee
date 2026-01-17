package com.example.bee.controllers.account;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DangNhapController {
    @GetMapping("/login")
    public String login() {
        return "login/dang-nhap";
    }
}
