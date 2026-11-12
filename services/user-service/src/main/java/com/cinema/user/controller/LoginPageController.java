package com.cinema.user.controller;

import com.cinema.user.config.GoogleLoginProperties;
import com.cinema.user.config.UserRegistrationProperties;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginPageController {

    private final GoogleLoginProperties googleLoginProperties;
    private final UserRegistrationProperties userRegistrationProperties;

    public LoginPageController(
            GoogleLoginProperties googleLoginProperties,
            UserRegistrationProperties userRegistrationProperties) {
        this.googleLoginProperties = googleLoginProperties;
        this.userRegistrationProperties = userRegistrationProperties;
    }

    @GetMapping(path = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public String loginPage(Model model) {
        model.addAttribute("googleLoginEnabled", googleLoginProperties.enabled());
        model.addAttribute("registrationPageUrl", userRegistrationProperties.pageUrl());

        return "login";
    }
}
