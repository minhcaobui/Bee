//package com.example.bee.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebMvcConfig implements WebMvcConfigurer {
//
//    @Value("${file.upload-dir}")
//    private String uploadDir;
//
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        // Cấu hình: Đường dẫn URL /uploads/** sẽ trỏ về thư mục vật lý
//        registry.addResourceHandler("/uploads/**")
//                .addResourceLocations("file:" + uploadDir + "/");
//    }
//}