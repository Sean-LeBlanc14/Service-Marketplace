package com.ServiceMarketplace.service_marketplace.service;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JWTService {
    
    //We need to generate a secret key
    private String secretkey;

    public JWTService(){
        try {
            SecretKey key = Jwts.SIG.HS256.key().build();

            secretkey = Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
        }
    }

    public String generateToken(String email){

        Map<String, Object> claims = new HashMap<>();
        
        return Jwts.builder()
            .claims()
            .add(claims)
            .subject(email)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + 60 * 60 * 30))
            .and()
            .signWith(getKey())
            .compact();
    }

    public Key getKey(){
        return Keys.hmacShaKeyFor(secretkey.getBytes());
    }

    public String extractEmail(String token){
        return "";
    }

    public boolean validateToken(String token, UserDetails userDetails){
        return true;
    }
}
