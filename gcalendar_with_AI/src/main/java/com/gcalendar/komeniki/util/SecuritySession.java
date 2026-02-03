package com.gcalendar.komeniki.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class SecuritySession {

    public static String getUsername() {
        // SecurityContextHolderから
        // org.springframework.security.core.Authenticationオブジェクトを取得
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null) {
            // AuthenticationオブジェクトからUserDetailsオブジェクトを取得
            Object principal = authentication.getPrincipal();
            if (principal instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principal;
                // Googleの場合は通常 "email" 属性から取得
                return oauth2User.getAttribute("email");
                // または名前が必要なら
                // return oauth2User.getAttribute("name");
            }
            if (principal instanceof UserDetails) {
                // UserDetailsオブジェクトから、ユーザ名を取得
                return ((UserDetails) principal).getUsername();
            }
        }
        return null;
    }

}