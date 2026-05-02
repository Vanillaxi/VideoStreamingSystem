package com.video.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class AppProperties {
    private static final String CONFIG_FILE = "properties/application.properties";
    private static final Properties PROPERTIES = load();

    private AppProperties() {
    }

    public static Properties getProperties() {
        Properties copy = new Properties();
        copy.putAll(PROPERTIES);
        return copy;
    }

    public static String getProperty(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException(CONFIG_FILE + " not found");
            }
            properties.load(inputStream);
            log.info("应用配置加载成功，path={}", CONFIG_FILE);
        } catch (Exception e) {
            log.error("应用配置加载失败，path={}", CONFIG_FILE, e);
            throw new ExceptionInInitializerError(e);
        }
        return properties;
    }
}
