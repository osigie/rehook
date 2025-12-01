package com.osigie.rehook.service.impl;

import com.osigie.rehook.domain.model.User;
import com.osigie.rehook.exception.TokenExpiredException;
import com.osigie.rehook.service.JWTService;
import com.osigie.rehook.utils.SignToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.function.Function;

@Service
public class JWTServiceImpl implements JWTService {

    private final SignToken signToken;

    public JWTServiceImpl(SignToken signToken) {
        this.signToken = signToken;
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public String extractTenant(String token) {
        return extractClaim(token, (c) -> c.get("tenant",  String.class));
    }


    public boolean isValid(String token, UserDetails user) {
        final String email = extractEmail(token);
        return (email.equals(user.getUsername()) && isTokenExpired(token));
    }


    public boolean isTokenExpired(String token) throws TokenExpiredException {
        return !extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(signToken.getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public String generateToken(User user) {

        return Jwts
                .builder()
                .subject(user.getEmail())
                .claim("tenant", user.getTenant().getName())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) //24 hrs
                .signWith(signToken.getSigningKey())
                .compact();
    }


    public String generateToken(User user, Integer expirationMillis) {

        return Jwts
                .builder()
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMillis)) //24 hrs
                .signWith(signToken.getSigningKey())
                .compact();
    }


}
