package com.example.bee.controllers.customer;

import com.example.bee.repositories.customer.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer")
public class CustomerController {
    @Autowired
    private KhachHangRepository khachHangRepository;

    @GetMapping({"", "/"})
    public String layout() {
        return "layout/customer-layout";
    }

    @GetMapping("/home")
    public String home() {
        return "customer/home/home";
    }

    @GetMapping("/shop")
    public String shop() {
        return "customer/shop/shop";
    }

    @GetMapping({"/detail", "/detail/{id}"})
    public String detail() {
        return "customer/detail/detail";
    }

    @GetMapping("/cart")
    public String cart() {
        return "customer/cart/cart";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "customer/cart/checkout";
    }

    @GetMapping("/order")
    public String order() {
        return "customer/order/order";
    }

    @GetMapping("/account")
    public String account(java.security.Principal principal, org.springframework.ui.Model model) {
        if (principal != null) {
            String username = principal.getName();
            model.addAttribute("user", khachHangRepository.findByTaiKhoan_TenDangNhap(username).orElse(null));
        }
        return "customer/account/account";
    }

    @GetMapping("/about")
    public String about() {
        return "customer/about/about";
    }

    @GetMapping("/collection")
    public String collection() {
        return "customer/collection/collection";
    }

    @GetMapping("/sale")
    public String sale() {
        return "customer/sale/sale";
    }

}