package com.mp3.demo.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

@Profile("api")
@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "OK";
    }
}