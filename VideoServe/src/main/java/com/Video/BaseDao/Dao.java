package com.Video.BaseDao;

import com.Video.Utils.LoggerUtil;
import com.Video.config.DBPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Dao {
    //通用的增删改方法
    public static int executeUpdate(String sql, Object... params) {
        Connection conn = DBPool.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            LoggerUtil.error(Dao.class, "SQL执行失败：" + sql, e);
            return -1;
        } finally {
            DBPool.releaseConnection(conn);
        }
    }
}
