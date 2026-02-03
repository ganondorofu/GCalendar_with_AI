package com.gcalendar.komeniki.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gcalendar.komeniki.model.User;
import com.gcalendar.komeniki.service.UserService;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final UserService userService;

    public SettingsController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api-status")
    public ResponseEntity<Map<String, Object>> getApiStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser != null && currentUser.getApiKey() != null && !currentUser.getApiKey().isEmpty()) {
                response.put("enabled", true);
                response.put("apiKey", currentUser.getApiKey());
            } else {
                response.put("enabled", false);
                response.put("apiKey", null);
            }
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/enable-api")
    public ResponseEntity<Map<String, Object>> enableApi() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません。再度ログインしてください。");
                return ResponseEntity.badRequest().body(response);
            }
            
            // APIキーがない場合は新規生成
            if (currentUser.getApiKey() == null || currentUser.getApiKey().isEmpty()) {
                String apiKey = UUID.randomUUID().toString();
                currentUser.setApiKey(apiKey);
                userService.save(currentUser);
            }
            
            response.put("success", true);
            response.put("apiKey", currentUser.getApiKey());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/disable-api")
    public ResponseEntity<Map<String, Object>> disableApi() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "ユーザーが見つかりません。再度ログインしてください。");
                return ResponseEntity.badRequest().body(response);
            }
            
            // APIキーをクリア
            currentUser.setApiKey(null);
            userService.save(currentUser);
            
            response.put("success", true);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            return null;
        }
        
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String authId = oauthToken.getName();
        
        return userService.findByAuthId(authId).orElse(null);
    }
}
