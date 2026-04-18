package com.video.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JSONUtil {

    /**
     * 对象转JSON字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            // 使用 Fastjson 的标准方法，它会自动跳过 AOP 代理的无关字段，防止栈溢出
            // SerializerFeature.WriteMapNullValue 让 null 字段也显示
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            log.error("JSON 序列化失败", e);
            return "{\"code\":500,\"msg\":\"JSON Error\"}";
        }
    }

    /**
     * JSON 字符串转对象
     */
    public static <T> T toBean(String json, Class<T> clazz) {
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.error("JSON 反序列化失败", e);
            return null;
        }
    }
}