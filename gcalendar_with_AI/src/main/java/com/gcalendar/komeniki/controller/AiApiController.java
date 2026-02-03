package com.gcalendar.komeniki.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcalendar.komeniki.model.AiResponse;
import com.gcalendar.komeniki.service.AiService;
import com.gcalendar.komeniki.service.CalendarService;

@RestController
@RequestMapping("/api")
public class AiApiController {

    private final CalendarService calendarService;
    private final ObjectMapper objectMapper;

    public AiApiController(CalendarService calendarService) {
        this.calendarService = calendarService;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/ai")
    public ResponseEntity<String> submit(@RequestParam("value") String value) {
        LocalDateTime nowDate = LocalDateTime.now();
        String prompt = AiService.buildPrompt(value, nowDate);
        String aiRawJsonString = AiService.requestAi(prompt);

        AiResponse ai = null;
        try {
            ai = objectMapper.readValue(aiRawJsonString, AiResponse.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error processing AI response: " + e.getMessage());
        }

        // 元の質問を渡してAIレスポンスを処理
        String result = calendarService.handleAiResponse(ai, value);
        return ResponseEntity.ok(result);
    }
}
