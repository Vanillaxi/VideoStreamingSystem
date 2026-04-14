package com.video.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.security.Key;
import java.util.Date;
import java.util.Properties;

@Slf4j
public class JWTUtil {

    private static String SECRET;
    private static long EXPIRE;
    private static String TOKEN_NAME;

    static {
        //获取配置信息
        try (InputStream input = JWTUtil.class.getClassLoader()
                .getResourceAsStream("mapper/JWT.properties")) {
            Properties props = new Properties();
            if (input != null) {
                //获取成功
                props.load(input);
                SECRET = props.getProperty("jwt.secret");
                EXPIRE = Long.parseLong(props.getProperty("jwt.ttl"));
                TOKEN_NAME = props.getProperty("jwt.tokenName");
                log.info("JWT配置加载成功");
            } else {
                //获取失败
                throw new RuntimeException("找不到JWT配置文件!");
            }
        } catch (Exception e) {
            log.error("JWT配置初始化失败！请检查 JWT.properties", e);//打印堆栈
        }
    }

    private static Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }


    /**
     * 生成令牌：加入 role 满足 RBAC 需求
     */
    public static String generate(Long userId, String username, Integer role) {
        // 1. 防御性判断
        if (userId == null) {
            log.error("生成 Token 失败：userId 为空");
            return null;
        }

        log.info("生成Token，用户ID: " + userId);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析令牌：带异常分类，方便统一异常处理
     * 当字符串被篡改或者 Token 过期了，会抛出异常
     */
    public static Claims parse(String token) throws ExpiredJwtException, JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 使用JDK 生的 java.util.logging，
     * 当有非法 Token 尝试访问时，后台会记录警告日志
     */
    public static boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
            return false;
        } catch (JwtException e) {
            log.warn("Token无效: " + e.getMessage());
            return false;
        }
    }

    /**
     * 刷新令牌：如果旧 Token 还没过期，且合法，则生成一个有效期完整的新 Token
     */
    public static String refreshToken(String oldToken) {
        try {
            Claims claims = parse(oldToken); // 解析出原有的 payload
            Long userId = Long.parseLong(claims.getSubject());
            String username = claims.get("username", String.class);
            Integer role = claims.get("role", Integer.class);

            // 重新生成一个全新的令牌返回
            return generate(userId, username, role);
        } catch (Exception e) {
            log.error("刷新令牌失败: " + e.getMessage());
            return null;
        }
    }

    public static Long getUserId(String token) {
        return Long.parseLong(parse(token).getSubject());
    }

    public static String getUsername(String token) {
        return parse(token).get("username", String.class);
    }

    /**
     * 获取角色信息，用于 RBAC 鉴权
     */
    public static Integer getUserRole(String token) {
        return parse(token).get("role", Integer.class);
    }

    public static String getTokenName() {
        return TOKEN_NAME;
    }
}