package com.osigie.rehook.service;

import com.osigie.rehook.config.AbstractContainerBaseTest;
import com.osigie.rehook.domain.model.User;
import com.osigie.rehook.exception.UserAlreadyExistException;
import com.osigie.rehook.repository.TenantRepository;
import com.osigie.rehook.repository.UserRepository;
import com.osigie.rehook.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class AuthServiceTest extends AbstractContainerBaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = com.osigie.rehook.util.TestDataFactory.createUser("test@example.com");
    }

    @Test
    public void givenNewUser_whenRegister_thenReturnUser() {
        //given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(tenantRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        //when
        User result = authService.register("test@example.com", "password", "tenant1");

        //then
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void givenExistingUser_whenRegister_thenThrowUserAlreadyExistException() {
        //given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        //when & then
        assertThrows(UserAlreadyExistException.class, () -> {
            authService.register("test@example.com", "password", "tenant1");
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void givenValidCredentials_whenAuthenticate_thenReturnUserData() {
        //given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // Return null to indicate successful authentication
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        //when
        Map<String, Object> result = authService.authenticate("test@example.com", "password");

        //then
        assertNotNull(result);
        assertEquals(testUser, result.get("user"));
        assertEquals("jwt-token", result.get("token"));
    }

    @Test
    public void givenInvalidEmail_whenAuthenticate_thenThrowException() {
        //given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        //when & then
        assertThrows(AuthenticationException.class, () -> {
            authService.authenticate("nonexistent@example.com", "password");
        });
    }

    @Test
    public void givenInvalidPassword_whenAuthenticate_thenThrowException() {
        //given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        //when & then
        assertThrows(AuthenticationException.class, () -> {
            authService.authenticate("test@example.com", "wrong-password");
        });
    }
}
