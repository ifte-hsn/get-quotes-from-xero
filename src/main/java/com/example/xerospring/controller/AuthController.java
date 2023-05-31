package com.example.xerospring.controller;

import com.example.xerospring.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@RestController
public class AuthController {

    @Autowired
    AuthService authService;

    @GetMapping("/login")
    public void authenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String url = authService.buildAuthUrl(response);
        response.sendRedirect(url);
    }


    @GetMapping("login/callback")
    public String loginCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean isAuthenticated = authService.callBack(request, response);
        if (isAuthenticated) {
            return "/home";
        }
        return "/";
    }
}
