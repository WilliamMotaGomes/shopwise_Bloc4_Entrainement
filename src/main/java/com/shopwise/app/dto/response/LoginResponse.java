package com.shopwise.app.dto.response;

import java.util.List;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, List<String> roles) {
}
