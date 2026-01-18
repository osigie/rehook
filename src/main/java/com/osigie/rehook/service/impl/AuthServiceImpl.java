package com.osigie.rehook.service.impl;

import com.osigie.rehook.domain.model.Tenant;
import com.osigie.rehook.domain.model.User;
import com.osigie.rehook.exception.ConflictException;
import com.osigie.rehook.exception.UserAlreadyExistException;
import com.osigie.rehook.repository.TenantRepository;
import com.osigie.rehook.repository.UserRepository;
import com.osigie.rehook.service.AuthService;
import com.osigie.rehook.service.JWTService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, TenantRepository tenantRepository, AuthenticationManager authenticationManager, JWTService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(String email, String password, String tenantName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistException();
        }

        if (tenantRepository.findByName(tenantName).isPresent()) {
            throw new ConflictException("Tenant already exists");
        }

        Tenant tenant = Tenant
                .builder()
                .name(tenantName)
                .build();

        User user = User
                .builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .tenant(tenant)
                .build();

        userRepository.save(user);

        return user;
    }


    @Override
    public Map<String, Object> authenticate(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email, password
                )
        );

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Invalid Credentials"));

        String jwt = jwtService.generateToken(user);

        log.info("User has successfully login...{}", user);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("user", user);
        userMap.put("token", jwt);
        return userMap;

    }
}
