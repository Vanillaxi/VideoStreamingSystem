package com.video.config;

import com.video.utils.AppProperties;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Properties;

@Slf4j
public class DBPool {
    private static final LinkedList<Connection> pool = new LinkedList<>();
    private static final Properties props = new Properties();
    private static final int INITIAL_SIZE = 5;
    private static volatile boolean initialized = false;
    private static volatile boolean shutdown = false;

    static {
        try {
            props.putAll(AppProperties.getProperties());

            Class.forName("com.mysql.cj.jdbc.Driver");

            for (int i = 0; i < INITIAL_SIZE; i++) {
                pool.add(createConnection());
            }
            initialized = true;
            log.info("MySQL 数据库连接池初始化成功，当前容量：{}", pool.size());
        } catch (Exception e) {
            for (Connection conn : pool) {
                closeSilently(conn);
            }
            pool.clear();
            initialized = false;
            log.error("数据库连接池初始化失败", e);
        }
    }

    public static synchronized Connection getConnection() {
        if (!initialized) {
            throw new IllegalStateException("DBPool is not initialized");
        }
        while (pool.isEmpty()) {
            if (shutdown) {
                throw new IllegalStateException("DBPool has been closed");
            }
            try {
                log.warn("连接池为空，等待可用连接...");
                DBPool.class.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for DB connection", e);
            }
        }

        Connection conn = pool.removeFirst();
        if (!isConnectionUsable(conn)) {
            closeSilently(conn);
            return createConnection();
        }
        return conn;
    }

    public static synchronized void releaseConnection(Connection conn) {
        if (conn == null) {
            return;
        }
        if (shutdown) {
            closeSilently(conn);
            return;
        }
        if (!isConnectionUsable(conn)) {
            closeSilently(conn);
            try {
                pool.addLast(createConnection());
            } catch (RuntimeException e) {
                log.error("连接失效后重建失败", e);
            } finally {
                DBPool.class.notifyAll();
            }
            return;
        }
        pool.addLast(conn);
        DBPool.class.notifyAll();
    }

    public static synchronized void close() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        for (Connection conn : pool) {
            closeSilently(conn);
        }
        int size = pool.size();
        pool.clear();
        DBPool.class.notifyAll();
        log.info("MySQL 连接池已关闭，共释放 {} 个空闲连接", size);
    }

    private static Connection createConnection() {
        try {
            return DriverManager.getConnection(
                    props.getProperty("db.url"),
                    props.getProperty("db.username"),
                    props.getProperty("db.password")
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create DB connection", e);
        }
    }

    private static boolean isConnectionUsable(Connection conn) {
        if (conn == null) {
            return false;
        }
        try {
            return !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            log.warn("数据库连接校验失败", e);
            return false;
        }
    }

    private static void closeSilently(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            if (!conn.isClosed()) {
                conn.close();
            }
        } catch (Exception e) {
            log.error("关闭数据库连接失败", e);
        }
    }
}
