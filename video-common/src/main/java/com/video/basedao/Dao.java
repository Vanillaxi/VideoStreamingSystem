package com.video.basedao;

import com.video.config.DBPool;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Dao {

    /**
     * 增删改 (Insert, Update, Delete)
     * @param sql
     * @param params
     * @return
     */
    public static int executeUpdate(String sql, Object... params) {
        Connection conn = DBPool.getConnection();
        PreparedStatement pstmt = null;
        try {
            //预编译
            pstmt = conn.prepareStatement(sql);
            // 动态填充参数
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            log.info("开始执行更新SQL: {}", sql);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("SQL更新失败! 语句: {} | 错误: ", sql, e);
            return -1;
        } finally {
            // 归还连接
            DBPool.releaseConnection(conn);
        }
    }

    /**
     * 查询(利用反射自动映射到 Pojo)
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
            //  获取结果集的元数据
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                // 创建实体类实例 （反射调用无参构造方法）
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
                        // 如类型转换失败
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
}