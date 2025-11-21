package com.example.bee.controllers.catalog;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/catalogs")
public class CatalogsController {
    @GetMapping
    public String catalogs() {
        return "admin/catalog/catalogs";
    }
}
