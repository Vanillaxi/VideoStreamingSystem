package com.Video.config;

import com.Video.Utils.LoggerUtil;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedList;
import java.util.Properties;
import java.io.InputStream;

public class DBPool {
    private static LinkedList<Connection> pool = new LinkedList<>();

    static {
        try (InputStream is = DBPool.class.getClassLoader().getResourceAsStream("mapper/DB.properties")) {
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
            LoggerUtil.info(DBPool.class, "MySQL 数据库连接池初始化成功，容量：5");
        } catch (Exception e) {
            LoggerUtil.error(DBPool.class, "数据库池初始化崩溃", e);
        }
    }

    // 获取连接
    public static synchronized Connection getConnection() {
        if (pool.isEmpty()) {
            LoggerUtil.warn(DBPool.class, "连接池空了！正在等待或扩容...");
        }
        return pool.removeFirst();
    }

    // 归还连接
    public static synchronized void releaseConnection(Connection conn) {
        pool.addLast(conn);
    }
}