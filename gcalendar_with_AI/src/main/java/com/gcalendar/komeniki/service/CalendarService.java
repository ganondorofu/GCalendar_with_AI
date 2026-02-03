package com.gcalendar.komeniki.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcalendar.komeniki.model.AiResponse;
import com.gcalendar.komeniki.model.Event;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class CalendarService {

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserService userService;

    public CalendarService(OAuth2AuthorizedClientManager authorizedClientManager,
                          OAuth2AuthorizedClientService authorizedClientService,
                          UserService userService) {
        this.authorizedClientManager = authorizedClientManager;
        this.authorizedClientService = authorizedClientService;
        this.userService = userService;
    }

    /**
     * 現在の認証情報からGoogle Calendarサービスを取得
     */
    private Calendar getCalendarService() throws GeneralSecurityException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            throw new IllegalStateException("OAuth2認証が必要です。再度ログインしてください。");
        }
        
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        
        System.out.println("OAuth Token - Registration ID: " + oauthToken.getAuthorizedClientRegistrationId());
        System.out.println("OAuth Token - Name: " + oauthToken.getName());
        
        // まず既存のクライアントをロード
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            oauthToken.getAuthorizedClientRegistrationId(),
            oauthToken.getName()
        );
        
        // クライアントが存在しない場合、またはトークンが期限切れの場合は新規取得/更新
        if (client == null || client.getAccessToken() == null || 
            (client.getAccessToken().getExpiresAt() != null && 
             client.getAccessToken().getExpiresAt().isBefore(java.time.Instant.now()))) {
            
            System.out.println("OAuth2クライアントを取得/更新します。");
            
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(
                oauthToken.getAuthorizedClientRegistrationId())
                .principal(oauthToken)
                .build();
            
            client = authorizedClientManager.authorize(authorizeRequest);
            
            if (client == null || client.getAccessToken() == null) {
                throw new IllegalStateException("OAuth2クライアントの取得/更新に失敗しました。再度ログインしてください。");
            }
            
            System.out.println("OAuth2クライアントを取得/更新しました。");
        }
        
        if (client.getAccessToken() == null) {
            throw new IllegalStateException("アクセストークンが取得できません。再度ログインしてください。");
        }
        
        String accessTokenValue = client.getAccessToken().getTokenValue();
        Date expiresAt = client.getAccessToken().getExpiresAt() != null 
            ? Date.from(client.getAccessToken().getExpiresAt()) 
            : new Date(System.currentTimeMillis() + 3600000); // デフォルト1時間
        
        AccessToken accessToken = new AccessToken(accessTokenValue, expiresAt);
        GoogleCredentials credentials = GoogleCredentials.create(accessToken);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
        
        return new Calendar.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            requestInitializer
        ).setApplicationName("GCalendar with AI").build();
    }

    /**
     * AIレスポンスを処理
     */
    public String handleAiResponse(AiResponse aiResponse, String originalQuestion) {
        if (!aiResponse.isAnswerable()) {
            return "処理できません: " + aiResponse.getMessage();
        }

        switch (aiResponse.getRequestType()) {
            case "register":
                return registerEvents(aiResponse.getEvents());
            case "search":
                return searchEventsWithAi(aiResponse.getStartDate(), aiResponse.getEndDate(), originalQuestion);
            default:
                return "不明なリクエストタイプ: " + aiResponse.getRequestType();
        }
    }

    /**
     * 複数イベントを登録
     */
    private String registerEvents(List<Event> events) {
        List<String> results = new ArrayList<>();
        for (Event event : events) {
            String result = addEvent(event);
            results.add(result);
        }
        return String.join("\n", results);
    }

    /**
     * Google Calendarにイベントを追加
     */
    public String addEvent(Event event) {
        try {
            Calendar calendarService = getCalendarService();
            
            com.google.api.services.calendar.model.Event googleEvent = 
                new com.google.api.services.calendar.model.Event();
            
            googleEvent.setSummary(event.getTitle());
            googleEvent.setDescription(event.getDescription());
            
            if (event.isAllDay()) {
                googleEvent.setStart(new EventDateTime()
                    .setDate(new DateTime(event.getStartDatetime().split("T")[0])));
                googleEvent.setEnd(new EventDateTime()
                    .setDate(new DateTime(event.getEndDatetime().split("T")[0])));
            } else {
                googleEvent.setStart(new EventDateTime()
                    .setDateTime(new DateTime(event.getStartDatetime() + ":00+09:00"))
                    .setTimeZone("Asia/Tokyo"));
                googleEvent.setEnd(new EventDateTime()
                    .setDateTime(new DateTime(event.getEndDatetime() + ":00+09:00"))
                    .setTimeZone("Asia/Tokyo"));
            }
            
            com.google.api.services.calendar.model.Event createdEvent = 
                calendarService.events().insert("primary", googleEvent).execute();
            
            return "予定を追加しました: " + createdEvent.getSummary();
            
        } catch (GeneralSecurityException | IOException e) {
            return "イベント追加エラー: " + e.getMessage();
        }
    }

    /**
     * Google Calendarからイベントを検索
     */
    public String searchEvents(String startDate, String endDate) {
        try {
            System.out.println("Calendar API検索開始: " + startDate + " から " + endDate);
            Calendar calendarService = getCalendarService();
            System.out.println("Calendarサービス取得成功");
            
            DateTime timeMin = new DateTime(startDate + "T00:00:00+09:00");
            DateTime timeMax = new DateTime(endDate + "T23:59:59+09:00");
            System.out.println("検索期間: " + timeMin + " から " + timeMax);
            
            Events events = calendarService.events().list("primary")
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
            System.out.println("Calendar API呼び出し成功、イベント数: " + events.getItems().size());
            
            if (events.getItems().isEmpty()) {
                return "予定はありません";
            }
            
            StringBuilder result = new StringBuilder("予定一覧:\n");
            for (var item : events.getItems()) {
                String start = item.getStart().getDateTime() != null 
                    ? item.getStart().getDateTime().toString() 
                    : item.getStart().getDate().toString();
                result.append("- ").append(item.getSummary()).append(" (").append(start).append(")\n");
            }
            return result.toString();
            
        } catch (GeneralSecurityException | IOException e) {
            System.out.println("Calendar API検索エラー: " + e.getMessage());
            e.printStackTrace();
            return "検索エラー: " + e.getMessage();
        }
    }

    /**
     * Google Calendarからイベントを検索し、結果をAIで要約
     */
    public String searchEventsWithAi(String startDate, String endDate, String originalQuestion) {
        // まず検索結果を取得
        String searchResults = searchEvents(startDate, endDate);
        
        // 検索結果と元の質問を合わせてAIに質問
        String summaryPrompt = AiService.buildSearchSummaryPrompt(originalQuestion, searchResults);
        String aiResponse = AiService.requestAi(summaryPrompt);
        
        return aiResponse;
    }

    /**
     * ユーザーIDを指定してイベントを検索（外部API用）
     */
    public String searchEventsForUser(String authId, String startDate, String endDate) {
        try {
            System.out.println("外部API Calendar検索開始: " + authId + ", " + startDate + " から " + endDate);
            Calendar calendarService = getCalendarServiceForUser(authId);
            System.out.println("Calendarサービス取得成功");

            DateTime timeMin = new DateTime(startDate + "T00:00:00+09:00");
            DateTime timeMax = new DateTime(endDate + "T23:59:59+09:00");

            Events events = calendarService.events().list("primary")
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
            System.out.println("Calendar API呼び出し成功、イベント数: " + events.getItems().size());

            if (events.getItems().isEmpty()) {
                return "予定はありません";
            }

            StringBuilder result = new StringBuilder("予定一覧:\n");
            for (var item : events.getItems()) {
                String start = item.getStart().getDateTime() != null
                    ? item.getStart().getDateTime().toString()
                    : item.getStart().getDate().toString();
                result.append("- ").append(item.getSummary()).append(" (").append(start).append(")\n");
            }
            return result.toString();

        } catch (GeneralSecurityException | IOException e) {
            System.out.println("外部API Calendar検索エラー: " + e.getMessage());
            e.printStackTrace();
            return "検索エラー: " + e.getMessage();
        }
    }

    /**
     * ユーザーIDを指定してAIレスポンスを処理（外部API用）
     */
    public String handleAiResponseForUser(String authId, String query) {
        try {
            // まずAIにクエリを解析させる
            String prompt = AiService.buildPrompt(query, java.time.LocalDateTime.now());
            String aiRawJsonString = AiService.requestAi(prompt);

            // JSONパース
            ObjectMapper objectMapper = new ObjectMapper();
            AiResponse aiResponse = objectMapper.readValue(aiRawJsonString, AiResponse.class);

            if (!aiResponse.isAnswerable()) {
                return "処理できません: " + aiResponse.getMessage();
            }

            switch (aiResponse.getRequestType()) {
                case "register":
                    return registerEventsForUser(authId, aiResponse.getEvents());
                case "search":
                    return searchEventsWithAiForUser(authId, aiResponse.getStartDate(), aiResponse.getEndDate(), query);
                default:
                    return "不明なリクエストタイプ: " + aiResponse.getRequestType();
            }

        } catch (Exception e) {
            System.out.println("外部API AI処理エラー: " + e.getMessage());
            e.printStackTrace();
            return "AI処理エラー: " + e.getMessage();
        }
    }

    /**
     * ユーザーIDを指定して複数イベントを登録（外部API用）
     */
    private String registerEventsForUser(String authId, List<Event> events) {
        List<String> results = new ArrayList<>();
        for (Event event : events) {
            String result = addEventForUser(authId, event);
            results.add(result);
        }
        return String.join("\n", results);
    }

    /**
     * ユーザーIDを指定してイベントを追加（外部API用）
     */
    public String addEventForUser(String authId, Event event) {
        try {
            Calendar calendarService = getCalendarServiceForUser(authId);

            com.google.api.services.calendar.model.Event googleEvent =
                new com.google.api.services.calendar.model.Event();

            googleEvent.setSummary(event.getTitle());
            googleEvent.setDescription(event.getDescription());

            if (event.isAllDay()) {
                googleEvent.setStart(new EventDateTime()
                    .setDate(new DateTime(event.getStartDatetime().split("T")[0])));
                googleEvent.setEnd(new EventDateTime()
                    .setDate(new DateTime(event.getEndDatetime().split("T")[0])));
            } else {
                googleEvent.setStart(new EventDateTime()
                    .setDateTime(new DateTime(event.getStartDatetime() + ":00+09:00"))
                    .setTimeZone("Asia/Tokyo"));
                googleEvent.setEnd(new EventDateTime()
                    .setDateTime(new DateTime(event.getEndDatetime() + ":00+09:00"))
                    .setTimeZone("Asia/Tokyo"));
            }

            com.google.api.services.calendar.model.Event createdEvent =
                calendarService.events().insert("primary", googleEvent).execute();

            return "予定を追加しました: " + createdEvent.getSummary();

        } catch (GeneralSecurityException | IOException e) {
            return "イベント追加エラー: " + e.getMessage();
        }
    }

    /**
     * ユーザーIDを指定してイベントを検索しAIで要約（外部API用）
     */
    public String searchEventsWithAiForUser(String authId, String startDate, String endDate, String originalQuestion) {
        // まず検索結果を取得
        String searchResults = searchEventsForUser(authId, startDate, endDate);

        // 検索結果と元の質問を合わせてAIに質問
        String summaryPrompt = AiService.buildSearchSummaryPrompt(originalQuestion, searchResults);
        String aiResponse = AiService.requestAi(summaryPrompt);

        return aiResponse;
    }

    /**
     * ユーザーIDを指定してCalendarサービスを取得（外部API用）
     */
    private Calendar getCalendarServiceForUser(String authId) throws GeneralSecurityException, IOException {
        return userService.getCalendarServiceForUser(authId);
    }
}
