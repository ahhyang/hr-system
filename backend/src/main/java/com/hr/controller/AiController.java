package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public Dtos.AiChatResponse chat(@Valid @RequestBody Dtos.AiChatRequest request) {
        return aiService.chat(request);
    }
}
