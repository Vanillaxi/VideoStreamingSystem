package com.video.utils;

//全局请求头，测试的时候要携带
public class AuthHeaderUtil {
    private static final String BEARER_PREFIX = "Bearer ";

    private AuthHeaderUtil() {
    }

    public static String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String value = authorization.trim();
        if (!value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = value.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    public static String buildBearerValue(String token) {
        return BEARER_PREFIX + token;
    }
}
