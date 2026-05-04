package com.video.mapper.impl;

import com.video.annotation.MyComponent;
import com.video.config.DBPool;
import com.video.exception.ErrorCode;
import com.video.exception.SystemException;
import com.video.mapper.CouponMapper;
import com.video.pojo.entity.Coupon;
import com.video.utils.JdbcUtils;
import com.video.utils.XmlSqlReaderUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

@MyComponent
public class CouponMapperImpl implements CouponMapper {
    @Override
    public Long create(Coupon coupon) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CouponMapper.create");
        Connection conn = DBPool.getConnection();
        try {
            conn.setAutoCommit(false);
            Long couponId;
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, coupon.getTitle());
                pstmt.setInt(2, coupon.getStock());
                pstmt.setObject(3, coupon.getStartTime());
                pstmt.setObject(4, coupon.getEndTime());
                pstmt.setInt(5, coupon.getStatus());
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SystemException(ErrorCode.SYSTEM_ERROR);
                    }
                    couponId = rs.getLong(1);
                }
            }
            conn.commit();
            return couponId;
        } catch (Exception e) {
            rollback(conn);
            if (e instanceof SystemException) {
                throw (SystemException) e;
            }
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        } finally {
            resetAutoCommit(conn);
            DBPool.releaseConnection(conn);
        }
    }

    @Override
    public List<Coupon> list(Integer status, int offset, int pageSize) {
        if (status == null) {
            String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CouponMapper.listAll");
            return JdbcUtils.executeQuery(Coupon.class, sql, offset, pageSize);
        }
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CouponMapper.listByStatus");
        return JdbcUtils.executeQuery(Coupon.class, sql, status, offset, pageSize);
    }

    @Override
    public Long count(Integer status) {
        if (status == null) {
            String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CouponMapper.countAll");
            return JdbcUtils.executeQueryCount(sql);
        }
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CouponMapper.countByStatus");
        return JdbcUtils.executeQueryCount(sql, status);
    }

    @Override
    public Coupon getById(Long id) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CouponMapper.getById");
        List<Coupon> coupons = JdbcUtils.executeQuery(Coupon.class, sql, id);
        return coupons == null || coupons.isEmpty() ? null : coupons.get(0);
    }

    @Override
    public Coupon getByIdForUpdate(Long id) {
        throw new UnsupportedOperationException("请在事务连接中调用 SELECT ... FOR UPDATE");
    }

    @Override
    public int decreaseStock(Long id) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CouponMapper.decreaseStock");
        return JdbcUtils.executeUpdate(sql, id);
    }

    @Override
    public int disable(Long id) {
        String sql = XmlSqlReaderUtil.getSql("com.video.mapper.CouponMapper.disable");
        return JdbcUtils.executeUpdate(sql, id);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception ignored) {
        }
    }

    private void resetAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (Exception ignored) {
        }
    }
}
