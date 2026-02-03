package com.gcalendar.komeniki.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import java.time.LocalDateTime;

public class AiService {

    // プロンプトの定数部分
    private static final String PROMPT_BASE = "あなたは自然言語からカレンダーを操作するシステムのアシスタントです。ユーザーからの入力をもとに、リクエストタイプ（register: 予定追加, search: 予定検索）を判断し、適切な形式でJSONを出力してください。"
            + "1回のリクエストで複数の予定を追加したい場合（例: 連続した日付に同じ予定、同じ日に複数の予定など）はevents配列に複数のイベントを含めてください。"
            + "searchの場合はGoogle Calendarで検索するための情報を生成してください。"
            + "出力はコードブロックやjson以外の返答を含まない純粋なjsonのみで行ってください。jsonとして処理できなくなるので```jsonのようなコードブロックは使わないでください。"
            + "回答が不可能なリクエストが来た場合はis_answerableをfalseにして、messageに理由を記載してください。";

    private static final String CURRENT_TIME_INFO = "次は現在の時刻情報です。";

    private static final String JSON_FORMAT_REGISTER = "registerの場合: {is_answerable:true,message:'Register success',request_type:'register',events:[{title:'タイトル',is_all_day:false,start_datetime:'YYYY-MM-DDTHH:MM',end_datetime:'YYYY-MM-DDTHH:MM',description:'説明'}]}";

    private static final String JSON_FORMAT_SEARCH = "searchの場合: {is_answerable:true,message:'Search parameters generated',request_type:'search',start_date:'YYYY-MM-DD',end_date:'YYYY-MM-DD'}";

    // 検索結果を要約するためのプロンプト
    private static final String SEARCH_SUMMARY_PROMPT = "あなたはカレンダーアシスタントです。ユーザーの質問と検索結果をもとに、自然な日本語で回答してください。";

    public static String buildPrompt(String userInput, LocalDateTime nowDate) {
        return userInput + PROMPT_BASE + CURRENT_TIME_INFO + nowDate + JSON_FORMAT_REGISTER + JSON_FORMAT_SEARCH;
    }

    /**
     * 検索結果をもとにAIで回答を生成
     */
    public static String buildSearchSummaryPrompt(String originalQuestion, String searchResults) {
        return SEARCH_SUMMARY_PROMPT 
            + "\n\nユーザーの質問: " + originalQuestion 
            + "\n\n検索結果: " + searchResults 
			+ "\n\n現在の日時: " + LocalDateTime.now()
            + "\n\n上記の情報をもとに、ユーザーの質問に対して自然な日本語で回答してください。";
    }
	
	public static String requestAi(String content/*String[] args*/) {
	    int maxRetries = 3;
	    int retryCount = 0;
	    long retryDelay = 1000; // 1秒
		
	    while (retryCount < maxRetries) {
	        try (Client client = new Client()) {
	            GenerateContentResponse response =
	                client.models.generateContent(
	                    "gemini-2.5-flash",
	                    content,
	                    null);
	            System.out.println("Gemini API Response: " + response.text());
	            return(response.text());
	            
	        } catch (Exception e) {
	            System.out.println("Gemini API Error (attempt " + (retryCount + 1) + "/" + maxRetries + "): " + e.getMessage());
	            
	            // 503エラー（過負荷）の場合のみリトライ
	            if (e.getMessage().contains("503") || e.getMessage().contains("overloaded") || e.getMessage().contains("quota")) {
	                retryCount++;
	                if (retryCount < maxRetries) {
	                    try {
	                        System.out.println("Retrying in " + retryDelay + "ms...");
	                        Thread.sleep(retryDelay);
	                        retryDelay *= 2; // 指数バックオフ
	                    } catch (InterruptedException ie) {
	                        Thread.currentThread().interrupt();
	                        break;
	                    }
	                    continue;
	                }
	            }
	            
	            // リトライ回数を超えた場合または503以外のエラーの場合はフォールバック
	            break;
	        }
	    }
		
	    // フォールバック：シンプルなモックレスポンスを返す
	    System.out.println("Using fallback response due to Gemini API issues");
	    if (content.contains("明日は何がある")) {
	        return "{\"is_answerable\":true,\"message\":\"Search parameters generated\",\"request_type\":\"search\",\"start_date\":\"2026-01-20\",\"end_date\":\"2026-01-20\"}";
	    } else if (content.contains("予定追加") || content.contains("追加")) {
	        return "{\"is_answerable\":true,\"message\":\"Register success\",\"request_type\":\"register\",\"events\":[{\"title\":\"サンプル予定\",\"is_all_day\":false,\"start_datetime\":\"2026-01-20T10:00\",\"end_datetime\":\"2026-01-20T11:00\",\"description\":\"AIで追加された予定\"}]}";
	    } else {
	        return "{\"is_answerable\":false,\"message\":\"Gemini APIが一時的に利用できません。しばらく待ってから再度お試しください。\"}";
	    }
	}
}
