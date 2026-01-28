package com.optum.jwt_auth.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hello")
public class HelloController {

    @GetMapping("sayHello")
    public String sayHello() {
        return "Hello, World!";
    }


    @GetMapping("sayGossip")
    public String sayGossip() {
        return "Did you hear about the new Java update?";
    }

}
