package com.video.websocket;

import com.video.utils.AuthHeaderUtil;
import com.video.utils.JWTUtil;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpointConfig;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@ServerEndpoint(value = "/ws/{userId}", configurator = NotificationWebSocketServer.AuthorizationConfigurator.class)
public class NotificationWebSocketServer {
    private static final String WS_USER_ID_KEY = "wsUserId";
    private static final ConcurrentMap<Long, Session> SESSION_MAP = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, EndpointConfig config, @PathParam("userId") Long userId) {
        Long tokenUserId = (Long) config.getUserProperties().get(WS_USER_ID_KEY);
        if (tokenUserId == null || !tokenUserId.equals(userId)) {
            closeUnauthenticated(session, userId);
            return;
        }
        SESSION_MAP.put(userId, session);
        log.info("WebSocket 用户连接成功，userId={}, sessionId={}", userId, session.getId());
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") Long userId) {
        SESSION_MAP.remove(userId, session);
        log.info("WebSocket 用户断开连接，userId={}, sessionId={}", userId, session.getId());
    }

    @OnError
    public void onError(Session session, @PathParam("userId") Long userId, Throwable throwable) {
        if (session != null) {
            SESSION_MAP.remove(userId, session);
        } else {
            SESSION_MAP.remove(userId);
        }
        log.warn("WebSocket 连接异常，userId={}", userId, throwable);
    }

    public static boolean isOnline(Long userId) {
        Session session = SESSION_MAP.get(userId);
        return session != null && session.isOpen();
    }

    public static boolean sendToUser(Long userId, String message) {
        Session session = SESSION_MAP.get(userId);
        if (session == null || !session.isOpen()) {
            return false;
        }
        synchronized (session) {
            try {
                session.getBasicRemote().sendText(message);
                return true;
            } catch (IOException e) {
                SESSION_MAP.remove(userId, session);
                log.warn("WebSocket 消息发送失败，userId={}", userId, e);
                return false;
            }
        }
    }

    private void closeUnauthenticated(Session session, Long userId) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized"));
        } catch (IOException e) {
            log.warn("WebSocket 未授权连接关闭失败，userId={}", userId, e);
        }
        log.warn("WebSocket 认证失败，拒绝连接，pathUserId={}, sessionId={}", userId, session.getId());
    }

    public static class AuthorizationConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request,
                                    jakarta.websocket.HandshakeResponse response) {
            Long userId = parseUserId(request);
            if (userId != null) {
                config.getUserProperties().put(WS_USER_ID_KEY, userId);
            }
        }

        private Long parseUserId(HandshakeRequest request) {
            String authorization = firstHeader(request.getHeaders(), "Authorization");
            String queryString = request.getQueryString();
            log.info("WebSocket 握手参数，Authorization={}, queryString={}", authorization, queryString);

            String token = extractToken(authorization, queryString);
            if (token == null) {
                log.warn("WebSocket 握手未获取到 token");
                return null;
            }
            try {
                if (!JWTUtil.validate(token)) {
                    log.warn("WebSocket Token 校验失败");
                    return null;
                }
                Long userId = JWTUtil.getUserId(token);
                log.info("WebSocket Token 解析成功，tokenUserId={}", userId);
                return userId;
            } catch (Exception e) {
                log.warn("WebSocket Token 解析失败", e);
                return null;
            }
        }

        private String extractToken(String authorization, String queryString) {
            String token = AuthHeaderUtil.extractBearerToken(authorization);
            if (token != null) {
                return token;
            }
            token = getQueryParam(queryString, "token");
            if (token == null || token.isBlank()) {
                return null;
            }
            token = token.trim();
            String bearerToken = AuthHeaderUtil.extractBearerToken(token);
            return bearerToken == null ? token : bearerToken;
        }

        private String getQueryParam(String queryString, String name) {
            if (queryString == null || queryString.isBlank()) {
                return null;
            }
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                int index = pair.indexOf('=');
                String key = index >= 0 ? pair.substring(0, index) : pair;
                if (!name.equals(URLDecoder.decode(key, StandardCharsets.UTF_8))) {
                    continue;
                }
                String value = index >= 0 ? pair.substring(index + 1) : "";
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
            return null;
        }

        private String firstHeader(Map<String, List<String>> headers, String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                    List<String> values = entry.getValue();
                    return values == null || values.isEmpty() ? null : values.get(0);
                }
            }
            return null;
        }
    }
}
