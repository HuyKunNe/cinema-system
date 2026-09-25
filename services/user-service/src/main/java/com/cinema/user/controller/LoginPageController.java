package com.cinema.user.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginPageController {

    @GetMapping(path = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public String loginPage() {

        return "login";
    }
}
