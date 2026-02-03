package com.gcalendar.komeniki.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
	@GetMapping("/login")
	public String login() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		// 既に認証済みの場合はトップページへリダイレクト
		if (auth != null && auth.isAuthenticated() && 
			!auth.getPrincipal().equals("anonymousUser")) {
			return "redirect:/";
		}
		return "login";
	}
}
