package com.example.social;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Spring Boot!";
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/sum")
    public Map<String, Object> sum(@RequestParam int a, @RequestParam int b) {
        return Map.of(
                "a", a,
                "b", b,
                "sum", a + b
        );
    }
}

