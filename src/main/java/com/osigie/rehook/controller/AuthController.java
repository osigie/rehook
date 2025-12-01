package com.osigie.rehook.controller;

import com.osigie.rehook.domain.model.User;
import com.osigie.rehook.dto.request.LoginRequestDto;
import com.osigie.rehook.dto.request.RegisterDto;
import com.osigie.rehook.dto.response.LoginResponseDto;
import com.osigie.rehook.dto.response.UserResponseDto;
import com.osigie.rehook.mapper.UserMapper;
import com.osigie.rehook.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, UserMapper userMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
    }

    @PostMapping("register")
    public ResponseEntity<UserResponseDto> register(
            @RequestBody @Valid RegisterDto request
    ) {
        User user = authService.register(request.email(), request.password(), request.tenant());
        return new ResponseEntity<>(userMapper.mapDto(user), HttpStatus.CREATED);
    }


    @PostMapping("login")
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody @Valid LoginRequestDto request
    ) {
        Map<String, Object> userData = authService
                .authenticate(request.email(), request.password());

        UserResponseDto user = userMapper.mapDto((User) userData.get("user"));
        String token = (String) userData.get("token");

        LoginResponseDto response = new LoginResponseDto(token, user);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
