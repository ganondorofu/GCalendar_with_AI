package com.gcalendar.komeniki.service;

import com.gcalendar.komeniki.model.User;
import com.gcalendar.komeniki.repository.UserRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final StringEncryptor encryptor;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public UserService(UserRepository userRepository, StringEncryptor encryptor) {
        this.userRepository = userRepository;
        this.encryptor = encryptor;
    }

    /**
     * ユーザー保存（apiKey自動生成、refresh_token暗号化）
     */
    public User save(User user) {
        if (user.getRefreshToken() != null && !user.getRefreshToken().isEmpty()) {
            user.setRefreshToken(encryptor.encrypt(user.getRefreshToken()));
        }
        return userRepository.save(user);
    }

    public Optional<User> findByAuthId(String authId) {
        return userRepository.findById(authId);
    }

    public Optional<User> findByApiKey(String apiKey) {
        return userRepository.findByApiKey(apiKey);
    }

    /**
     * refresh_token から access_token を取得（復号化してから使用）
     */
    public String getAccessTokenFromRefresh(String encryptedRefreshToken) {
        String refreshToken = encryptor.decrypt(encryptedRefreshToken);
        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }
        throw new RuntimeException("Failed to refresh access token: " + response.getBody());
    }

    /**
     * authId（Google UID）からカレンダーサービスを取得
     */
    public Calendar getCalendarServiceForUser(String authId) throws GeneralSecurityException, IOException {
        User user = userRepository.findById(authId)
                .orElseThrow(() -> new RuntimeException("User not found: " + authId));

        String accessToken = getAccessTokenFromRefresh(user.getRefreshToken());

        AccessToken token = new AccessToken(accessToken, null);
        GoogleCredentials credentials = GoogleCredentials.create(token);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer
        ).setApplicationName("GCalendar with AI").build();
    }

    private String generateUniqueApiKey() {
        for (int i = 0; i < 5; i++) {
            String key = UUID.randomUUID().toString().replace("-", "");
            if (userRepository.findByApiKey(key).isEmpty()) return key;
        }
        throw new IllegalStateException("apiKey generation failed after retries");
    }
}
