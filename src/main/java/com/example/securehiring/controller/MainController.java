package com.example.securehiring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/result")
    public String result() {
        return "result";
    }

    @GetMapping("/search")
    public String search() {
        return "search";
    }
}
