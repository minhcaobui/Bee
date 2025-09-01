package com.example.bee.controllers.catalog;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/meta")
public class MetaController {
    @GetMapping
    public String index() {
        return "catalog/meta";
    }
}
