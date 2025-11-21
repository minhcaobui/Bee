package com.example.bee.controllers.product;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/products")
public class ProductsController {
    @GetMapping
    public String products() {
        return "admin/product/products";
    }
}
