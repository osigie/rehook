package com.osigie.rehook.service;


import com.osigie.rehook.domain.model.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

public interface JWTService {

    String extractEmail(String token);

    String extractTenant(String token);

    boolean isValid(String token, UserDetails user);

    String generateToken(User user);

    String generateToken(User user, Integer expirationMillis);

    boolean isTokenExpired(String mockToken);

    Date extractExpiration(String mockToken);


}

