package com.osigie.rehook.service;

import com.osigie.rehook.domain.model.User;

import java.util.Map;

public interface AuthService {

    User register(String email, String password, String tenantName);

    Map<String, Object> authenticate(String email, String password);
}
