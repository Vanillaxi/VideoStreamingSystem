package com.video.utils;

import com.video.config.DBPool;
import com.video.exception.ErrorCode;
import com.video.exception.SystemException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JdbcUtils {

    public static int executeUpdate(String sql, Object... params) {
        Connection conn = DBPool.getConnection();
        try {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setParameters(pstmt, params);
                log.info("开始执行更新SQL: {}", sql);
                return pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("SQL更新失败! 语句: {}", sql, e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        } finally {
            DBPool.releaseConnection(conn);
        }
    }

    public static <T> List<T> executeQuery(Class<T> clazz, String sql, Object... params) {
        Connection conn = DBPool.getConnection();
        List<T> list = new ArrayList<>();

        try {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setParameters(pstmt, params);
                try (ResultSet rs = pstmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        T t = clazz.getDeclaredConstructor().newInstance();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnLabel(i);
                            Object value = rs.getObject(i);

                            try {
                                Field field = clazz.getDeclaredField(columnName);
                                field.setAccessible(true);
                                field.set(t, value);
                            } catch (NoSuchFieldException e) {
                                log.debug("类 {} 中未找到属性 {}", clazz.getSimpleName(), columnName);
                            } catch (Exception e) {
                                log.error("属性填充失败, 列名={}, 值={}", columnName, value, e);
                            }
                        }
                        list.add(t);
                    }
                }
            }
            log.info("查询SQL执行成功，返回记录数: {}", list.size());
        } catch (Exception e) {
            log.error("查询失败! SQL: {}", sql, e);
        } finally {
            DBPool.releaseConnection(conn);
        }
        return list;
    }

    public static Long executeQueryCount(String sql, Object... params) {
        Connection conn = DBPool.getConnection();
        try {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setParameters(pstmt, params);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询 Count 失败! SQL: {}", sql, e);
        } finally {
            DBPool.releaseConnection(conn);
        }
        return 0L;
    }

    public static boolean executeExists(String sql, Object... params) {
        Connection conn = DBPool.getConnection();
        try {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setParameters(pstmt, params);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    Object value = rs.getObject(1);
                    if (value instanceof Number) {
                        return ((Number) value).longValue() > 0;
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("查询 Exists 失败! SQL: {}", sql, e);
            return false;
        } finally {
            DBPool.releaseConnection(conn);
        }
    }

    private static void setParameters(PreparedStatement pstmt, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }
    }
}
