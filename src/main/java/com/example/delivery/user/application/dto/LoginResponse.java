package com.example.delivery.user.application.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    private static final String BEARER = "Bearer";

    public static LoginResponse bearer(String accessToken, long expiresIn) {
        return new LoginResponse(accessToken, BEARER, expiresIn);
    }
}
