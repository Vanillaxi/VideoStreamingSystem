package com.video.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

@Slf4j
public class CouponCodeUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final String SECRET = loadSecret();

    private CouponCodeUtil() {
    }

    public static String generate(Long couponId, Long userId) {
        byte[] random = new byte[10];
        SECURE_RANDOM.nextBytes(random);
        byte[] signature = hmac(random, couponId, userId);
        byte[] payload = new byte[random.length + 5];
        System.arraycopy(random, 0, payload, 0, random.length);
        System.arraycopy(signature, 0, payload, random.length, 5);
        String encoded = base32(payload);
        String body = encoded.substring(0, 16);
        return "CPN-" + body.substring(0, 4)
                + "-" + body.substring(4, 8)
                + "-" + body.substring(8, 12)
                + "-" + body.substring(12, 16);
    }

    private static byte[] hmac(byte[] random, Long couponId, Long userId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(random);
            mac.update(ByteBuffer.allocate(Long.BYTES).putLong(couponId == null ? 0L : couponId).array());
            mac.update(ByteBuffer.allocate(Long.BYTES).putLong(userId == null ? 0L : userId).array());
            return mac.doFinal();
        } catch (Exception e) {
            throw new IllegalStateException("生成优惠券 HMAC 校验位失败", e);
        }
    }

    private static String base32(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                builder.append(BASE32[(buffer >> (bitsLeft - 5)) & 31]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            builder.append(BASE32[(buffer << (5 - bitsLeft)) & 31]);
        }
        return builder.toString();
    }

    private static String loadSecret() {
        try {
            return AppProperties.getProperty("jwt.secret", "coupon-code-default-secret");
        } catch (Exception e) {
            log.warn("读取券码 HMAC secret 失败，使用默认 secret", e);
            return "coupon-code-default-secret";
        }
    }
}
