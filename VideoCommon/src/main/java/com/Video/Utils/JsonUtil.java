// VideoCommon/src/main/java/com/video/Utils/JsonUtil.java
package com.Video.Utils;

import java.lang.reflect.Field;
import java.util.List;

public class JsonUtil {

    // 对象转JSON字符串（简化版，够用就行）
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }

        if (obj instanceof Number || obj instanceof Boolean) {
            return String.valueOf(obj);
        }

        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }

        // 普通对象，用反射获取字段
        StringBuilder sb = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value != null) {
                    if (!first) {
                        sb.append(",");
                    }
                    sb.append("\"").append(field.getName()).append("\":");
                    sb.append(toJson(value));
                    first = false;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        sb.append("}");
        return sb.toString();
    }

    // 简单转义
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // 统一返回格式（最常用）
    public static String success(Object data) {
        return "{\"code\":200,\"msg\":\"success\",\"data\":" + toJson(data) + "}";
    }

    public static String error(int code, String msg) {
        return "{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}";
    }
}