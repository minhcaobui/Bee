package com.example.bee.controllers.api;

import com.example.bee.services.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tro-ly-ao")
@RequiredArgsConstructor
public class ChatbotApi {

    private final ChatbotService chatbotService;

    @PostMapping("/hoi-dap")
    public ResponseEntity<Map<String, String>> hoiDapBot(@RequestBody Map<String, String> payload) {
        return chatbotService.hoiDap(payload);
    }
}