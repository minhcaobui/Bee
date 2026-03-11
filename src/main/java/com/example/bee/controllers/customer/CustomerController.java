package com.example.bee.controllers.customer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    // =============================================
    // SHELL LAYOUT — trang gốc, chứa navbar + footer
    // Truy cập: http://localhost:8080/
    // =============================================
    @GetMapping({"", "/"})
    public String layout() {
        return "customer/customer-layout";
    }

    // =============================================
    // FRAGMENTS — router fetch vào #content-area
    // Giống pattern /orders, /products bên admin
    // =============================================

    @GetMapping("/home")
    public String home() {
        return "customer/home/home";
    }

    @GetMapping("/shop")
    public String shop() {
        return "customer/shop/shop";
    }

    @GetMapping("/detail")
    public String detail() {
        return "customer/detail/detail";
    }

    @GetMapping("/cart")
    public String cart() {
        return "customer/cart/cart";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "customer/checkout/checkout";
    }

    @GetMapping("/order")
    public String order() {
        return "customer/order/order";
    }

    @GetMapping("/account")
    public String account() { return "customer/account/account"; }

    @GetMapping("/about")
    public String about() { return "customer/about/about"; }

    @GetMapping("/collection")
    public String collection() { return "customer/collection/collection"; }

    @GetMapping("/sale")
    public String sale() { return "customer/sale/sale"; }
}