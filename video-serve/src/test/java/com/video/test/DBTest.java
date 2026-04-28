package com.video.test;

import com.video.config.DBPool;
import org.junit.Test;
import java.sql.Connection;
public class DBTest {

    @Test
    public void testSQLConnection() {
        System.out.println("开始测试 MySQL 连接...");
        try (Connection conn = DBPool.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("---------------------------------");
                System.out.println("MySQL 连接成功！");
                System.out.println("当前连接对象: " + conn);
                System.out.println("---------------------------------");

                // 归还
                DBPool.releaseConnection(conn);
            }
        } catch (Exception e) {
            System.err.println("MySQL 连接失败！请检查 DB.properties 配置或数据库是否启动。");
            e.printStackTrace();
        }
    }
}