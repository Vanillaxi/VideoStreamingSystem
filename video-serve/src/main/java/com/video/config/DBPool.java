package com.video.config;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedList;
import java.util.Properties;

@Slf4j
public class DBPool {
    private static LinkedList<Connection> pool = new LinkedList<>();

    static {
        try (InputStream is = DBPool.class.getClassLoader().getResourceAsStream("properties/DB.properties")) {
            Properties props = new Properties();
            props.load(is);

            // 加载驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 初始化创建 5 个连接
            for (int i = 0; i < 5; i++) {
                Connection conn = DriverManager.getConnection(
                        props.getProperty("db.url"),
                        props.getProperty("db.username"),
                        props.getProperty("db.password")
                );
                pool.add(conn);
            }
            log.info("MySQL 数据库连接池初始化成功，当前容量：{}", pool.size());
        } catch (Exception e) {
            log.error("数据库池初始化崩溃", e);
        }
    }

    // 获取连接
    public static synchronized Connection getConnection() {
        if (pool.isEmpty()) {
            log.warn("连接池空了！正在等待或扩容...");
        }
        return pool.removeFirst();
    }

    // 归还连接
    public static synchronized void releaseConnection(Connection conn) {
        pool.addLast(conn);
    }
}