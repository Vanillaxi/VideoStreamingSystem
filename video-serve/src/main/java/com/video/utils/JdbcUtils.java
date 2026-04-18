package com.video.utils;

import com.video.config.DBPool;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JdbcUtils {

    /**
     * 修改
     * @param sql
     * @param params
     * @return
     */
    public static int executeUpdate(String sql, Object... params) {
        Connection conn = DBPool.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
                //System.out.println(params[i]);
            }
            log.info("开始执行更新SQL: {}", sql);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("SQL更新失败! 语句: {} | 错误: ", sql, e);
            return -1;
        } finally {
            DBPool.releaseConnection(conn);
        }
    }

    /**
     * 查询
     * @param clazz
     * @param sql
     * @param params
     * @return
     */
    public static <T> List<T> executeQuery(Class<T> clazz, String sql, Object... params) {
        Connection conn = DBPool.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<T> list = new ArrayList<>();

        try {
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }

            rs = pstmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                T t = clazz.getDeclaredConstructor().newInstance();
                for (int i = 1; i <= columnCount; i++) {
                    // 获取列名并寻找类中对应的属性
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);

                    try {
                        Field field = clazz.getDeclaredField(columnName);
                        field.setAccessible(true);
                        field.set(t, value);
                    } catch (NoSuchFieldException e) {
                        log.debug("类 {} 中未找到属性: {}", clazz.getSimpleName(), columnName);
                    } catch (Exception e) {
                        log.error("属性填充失败: 列名={}, 值={}", columnName, value, e);
                    }
                }
                list.add(t);
            }
            log.info("查询SQL执行成功，返回记录数: {}", list.size());
        } catch (Exception e) {
            log.error("查询失败! SQL: {}", sql, e);
        } finally {
            DBPool.releaseConnection(conn);
        }
        return list;
    }

    /**
     * 查询，返回cnt
     * @param sql
     * @param params
     * @return
     */
    public static Long executeQueryCount(String sql, Object... params) {
        Connection conn = DBPool.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            log.error("查询 Count 失败! SQL: {}", sql, e);
        } finally {
            DBPool.releaseConnection(conn);
        }
        return 0L;
    }

    /**
     * 查询是否存在
     * @param sql
     * @param params
     * @return
     */
    public static boolean executeExists(String sql, Object... params) {
        Connection conn = DBPool.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            rs = pstmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            log.error("查询 Exists 失败! SQL: {}", sql, e);
            return false;
        } finally {
            DBPool.releaseConnection(conn);
        }
    }
}