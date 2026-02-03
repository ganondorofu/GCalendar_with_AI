package com.gcalendar.komeniki.controller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gcalendar.komeniki.service.AiService;

@Controller
public class CalendarController {

	@PostMapping("/add")
    public String submit(@RequestParam("value") String value,Model model) {
        return "test";
    }
}
