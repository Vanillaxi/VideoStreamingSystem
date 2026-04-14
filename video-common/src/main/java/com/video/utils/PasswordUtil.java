package com.video.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    /**
     * 加密：自动生成盐并哈希
     * 返回值格式如：$2a$10$R9h/lIPz... (已包含盐值)
     */
    public static String hashPassword(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt());
    }

    /**
     * 校验：自动从 hashed 中提取盐并对比
     */
    public static boolean checkPassword(String plainText, String hashed) {
        try {
            return BCrypt.checkpw(plainText, hashed);
        } catch (Exception e) {
            return false;
        }
    }
}