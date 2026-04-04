package com.Video.Utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // 加密密码（加盐）
    public static String encrypt(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    // 验证密码（明文 vs 密文）
    public static boolean verify(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
