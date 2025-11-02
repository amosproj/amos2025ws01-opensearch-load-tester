package com.opensearchloadtester.testdatagenerator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/scenario")
public class TestScenarioController {

    @GetMapping("/")
    public String getGreeting() {
        return "Hello, let's start a load test!";
    }
}
