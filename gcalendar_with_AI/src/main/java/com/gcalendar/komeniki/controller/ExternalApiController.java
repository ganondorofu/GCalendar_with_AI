package com.gcalendar.komeniki.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gcalendar.komeniki.model.User;
import com.gcalendar.komeniki.service.CalendarService;
import com.gcalendar.komeniki.service.UserService;

@RestController
@RequestMapping("/api/external")
public class ExternalApiController {

    private final UserService userService;
    private final CalendarService calendarService;

    public ExternalApiController(UserService userService, CalendarService calendarService) {
        this.userService = userService;
        this.calendarService = calendarService;
    }

    /**
     * APIキーと日本語クエリでAIカレンダー操作を行う
     * 使用例: POST /api/external/query
     * Body: query=明日の予定を教えて&api_key=YOUR_API_KEY
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> processQuery(
            @RequestParam("query") String query,
            @RequestHeader("X-API-Key") String apiKey) {

        Map<String, Object> response = new HashMap<>();

        try {
            // APIキーでユーザーを認証
            User user = authenticateByApiKey(apiKey);
            if (user == null) {
                response.put("success", false);
                response.put("message", "無効なAPIキーです");
                return ResponseEntity.status(401).body(response);
            }

            // AIレスポンスを処理（外部API用）
            String result = calendarService.handleAiResponseForUser(user.getAuthId(), query);

            response.put("success", true);
            response.put("result", result);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "処理エラー: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }   

        return ResponseEntity.ok(response);
    }

    private User authenticateByApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }
        return userService.findByApiKey(apiKey).orElse(null);
    }
}
