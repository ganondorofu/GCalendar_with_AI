package com.gcalendar.komeniki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
    org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration.class
})
public class GcalendarWithAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GcalendarWithAiApplication.class, args);
	}

}
