package com.ServiceMarketplace.service_marketplace.controller;

import java.util.HashMap;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@CrossOrigin("localhost:5173")
public class TestController {
    
    @GetMapping("/Cors")
    public HashMap<String, String> testCors(){

        System.out.println("THE SYSTEM HAS READ");
        HashMap <String, String> response = new HashMap<>();

        response.put("message", "Cors is working!");

        return response;
    }
    
}
