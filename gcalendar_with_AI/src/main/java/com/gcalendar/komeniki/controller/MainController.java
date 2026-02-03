package com.gcalendar.komeniki.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.gcalendar.komeniki.util.SecuritySession;

@Controller
public class MainController {

    @GetMapping("/")
    public String index() {
    	System.out.println(SecuritySession.getUsername());
        return "index";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }
}
