package com.Video.Utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.io.InputStream;
import java.security.Key;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JwtUtil {

    private static final Logger logger = Logger.getLogger(JwtUtil.class.getName());

    private static String SECRET;
    private static long EXPIRE;
    private static String TOKEN_NAME;

    static {
        //获取配置信息
        try (InputStream input = JwtUtil.class.getClassLoader()
                .getResourceAsStream("JWT.properties")) {
            Properties props = new Properties();
            if (input != null) {
                //获取成功
                props.load(input);
                SECRET = props.getProperty("jwt.secret");
                EXPIRE = Long.parseLong(props.getProperty("jwt.ttl"));
                TOKEN_NAME = props.getProperty("jwt.tokenName");
                logger.info("JWT配置加载成功");
            } else {
                //获取失败
                throw new RuntimeException("找不到JWT配置文件!");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "JWT获取失败", e);
        }
    }

    private static Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    /**
     * 生成令牌：加入 role 满足 RBAC 需求
     * 把用户信息整合成加密字符串
     * 载荷claims 里面只放非敏感信息
     * sign 签名：使用HS256算法 和 secret密钥进行签名
     */
    public static String generate(Integer userId, String username, Integer role) {
        logger.info("生成Token，用户ID: " + userId + "，角色: " + role);
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .claim("role", role)  // 存入角色，用于防止垂直越权
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
            logger.warning("Token已过期");
            return false;
        } catch (JwtException e) {
            logger.warning("Token无效: " + e.getMessage());
            return false;
        }
    }

    public static Integer getUserId(String token) {
        return Integer.parseInt(parse(token).getSubject());
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