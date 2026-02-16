package com.osigie.rehook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osigie.rehook.config.AbstractContainerBaseTest;
import com.osigie.rehook.domain.model.User;
import com.osigie.rehook.dto.request.LoginRequestDto;
import com.osigie.rehook.dto.request.RegisterDto;
import com.osigie.rehook.dto.response.LoginResponseDto;
import com.osigie.rehook.dto.response.UserResponseDto;
import com.osigie.rehook.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AuthControllerTest extends AbstractContainerBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    public void givenValidRegisterRequest_whenRegister_thenReturnCreated() throws Exception {
        //given
        RegisterDto registerDto = new RegisterDto("test@example.com", "password", "tenant1");
        User user = com.osigie.rehook.util.TestDataFactory.createUser("test@example.com");
        
        when(authService.register(anyString(), anyString(), anyString())).thenReturn(user);

        //when & then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    public void givenDuplicateEmail_whenRegister_thenReturnConflict() throws Exception {
        //given
        RegisterDto registerDto = new RegisterDto("existing@example.com", "password", "tenant1");
        
        when(authService.register(anyString(), anyString(), anyString()))
                .thenThrow(new com.osigie.rehook.exception.UserAlreadyExistException());

        //when & then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isConflict());
    }

    @Test
    public void givenValidLoginRequest_whenLogin_thenReturnToken() throws Exception {
        //given
        LoginRequestDto loginDto = new LoginRequestDto("test@example.com", "password");
        User user = com.osigie.rehook.util.TestDataFactory.createUser("test@example.com");
        String token = "jwt-token";
        
        when(authService.authenticate(anyString(), anyString()))
                .thenReturn(Map.of("user", user, "token", token));

        //when & then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(token))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    public void givenInvalidCredentials_whenLogin_thenReturnUnauthorized() throws Exception {
        //given
        LoginRequestDto loginDto = new LoginRequestDto("test@example.com", "wrong-password");
        
        when(authService.authenticate(anyString(), anyString()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid credentials"));

        //when & then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized());
    }
}