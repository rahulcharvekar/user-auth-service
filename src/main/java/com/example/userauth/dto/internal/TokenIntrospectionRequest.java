package com.example.userauth.dto.internal;

import jakarta.validation.constraints.NotBlank;

public class TokenIntrospectionRequest {

    @NotBlank
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
