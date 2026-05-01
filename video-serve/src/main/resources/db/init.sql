-- VideoStreamingSystem full initialization SQL.
-- Use this file only when creating a database from zero.

CREATE DATABASE IF NOT EXISTS `video` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `video`;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `message_outbox`;
DROP TABLE IF EXISTS `coupon_seckill_fail`;
DROP TABLE IF EXISTS `coupon_order_fail`;
DROP TABLE IF EXISTS `coupon_order`;
DROP TABLE IF EXISTS `coupon`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `comment_mention`;
DROP TABLE IF EXISTS `comments`;
DROP TABLE IF EXISTS `video_favorite`;
DROP TABLE IF EXISTS `video_like`;
DROP TABLE IF EXISTS `user_follow`;
DROP TABLE IF EXISTS `videos`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- User
-- ----------------------------
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `username` VARCHAR(32) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(64) NOT NULL COMMENT 'BCrypt加密后的密码',
    `nickname` VARCHAR(50) DEFAULT '用户' COMMENT '昵称',
    `role` TINYINT DEFAULT 0 COMMENT '0-普通用户，1-管理员',
    `create_user` VARCHAR(32) DEFAULT 'system' COMMENT '创建人',
    `update_user` VARCHAR(32) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `avatar_url` VARCHAR(500) COMMENT '头像访问地址',
    `avatar_object_key` VARCHAR(255) NULL COMMENT '头像OSS对象Key',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- Category
-- ----------------------------
CREATE TABLE `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(64) NOT NULL COMMENT '分类名称',
    `description` VARCHAR(255) COMMENT '分类描述',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频分区表';

-- ----------------------------
-- Videos
-- ----------------------------
CREATE TABLE `videos` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '视频ID',
    `title` VARCHAR(100) NOT NULL COMMENT '视频标题',
    `description` TEXT COMMENT '视频描述',
    `category_id` BIGINT COMMENT '分类ID',
    `user_id` BIGINT NOT NULL COMMENT '上传者ID',
    `video_url` VARCHAR(500) NOT NULL COMMENT 'OSS视频访问地址',
    `object_key` VARCHAR(255) NOT NULL COMMENT 'OSS对象Key',
    `size` BIGINT NOT NULL COMMENT '视频大小，单位字节',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '视频状态',
    `like_count` BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` BIGINT NOT NULL DEFAULT 0 COMMENT '评论数',
    `favorite_count` BIGINT NOT NULL DEFAULT 0 COMMENT '收藏数',
    `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '播放数',
    `hot_score` DOUBLE NOT NULL DEFAULT 0 COMMENT '热度分',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_videos_category_time` (`category_id`, `create_time`),
    KEY `idx_videos_hot_score` (`hot_score`, `create_time`),
    KEY `idx_videos_hot_cursor` (`hot_score` DESC, `create_time` DESC, `id` DESC),
    KEY `idx_videos_time_cursor` (`create_time` DESC, `id` DESC),
    KEY `idx_videos_user` (`user_id`),
    CONSTRAINT `fk_video_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频表';

-- ----------------------------
-- Follow
-- ----------------------------
CREATE TABLE `user_follow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `follower_id` BIGINT NOT NULL COMMENT '关注者id',
    `following_id` BIGINT NOT NULL COMMENT '被关注者id',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follow` (`follower_id`, `following_id`),
    KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

-- ----------------------------
-- Video Like
-- ----------------------------
CREATE TABLE `video_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户id',
    `video_id` BIGINT NOT NULL COMMENT '视频id',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_video` (`user_id`, `video_id`),
    KEY `idx_video_like_video` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频点赞表';

-- ----------------------------
-- Video Favorite
-- ----------------------------
CREATE TABLE `video_favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `video_id` BIGINT NOT NULL COMMENT '视频ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_video_favorite` (`video_id`, `user_id`),
    KEY `idx_video_favorite_user_time` (`user_id`, `create_time`),
    KEY `idx_video_favorite_video` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频收藏表';

-- ----------------------------
-- Comments
-- ----------------------------
CREATE TABLE `comments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `video_id` BIGINT NOT NULL COMMENT '所属视频ID',
    `user_id` BIGINT NOT NULL COMMENT '发表者用户ID',
    `parent_id` BIGINT NULL COMMENT '直接回复的评论ID',
    `root_id` BIGINT NULL COMMENT '所属一级评论ID',
    `reply_to_user_id` BIGINT NULL COMMENT '回复的用户ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `reply_count` BIGINT NOT NULL DEFAULT 0 COMMENT '回复数',
    `hot_score` DOUBLE NOT NULL DEFAULT 0 COMMENT '热度分',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0未删除，1已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_comments_video_parent_time` (`video_id`, `parent_id`, `create_time`),
    KEY `idx_comments_video_parent_hot` (`video_id`, `parent_id`, `hot_score`, `create_time`),
    KEY `idx_comments_video_time_cursor` (`video_id`, `parent_id`, `create_time` DESC, `id` DESC),
    KEY `idx_comments_video_hot_cursor` (`video_id`, `parent_id`, `hot_score` DESC, `create_time` DESC, `id` DESC),
    KEY `idx_comments_root_time` (`root_id`, `create_time`),
    KEY `idx_comments_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ----------------------------
-- Comment Mention
-- ----------------------------
CREATE TABLE `comment_mention` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `from_user_id` BIGINT NOT NULL COMMENT '发起@的用户ID',
    `to_user_id` BIGINT NOT NULL COMMENT '被@的用户ID',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0未读，1已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_comment_mention_to_read_time` (`to_user_id`, `is_read`, `create_time`),
    KEY `idx_comment_mention_comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论@表';

-- ----------------------------
-- Notification
-- ----------------------------
CREATE TABLE `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '接收通知的用户ID',
    `type` VARCHAR(32) NOT NULL COMMENT '通知类型',
    `content` VARCHAR(500) NOT NULL COMMENT '通知内容',
    `related_id` BIGINT COMMENT '关联业务ID',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0未读，1已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_notification_user_read_time` (`user_id`, `is_read`, `create_time`),
    KEY `idx_notification_related` (`related_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

-- ----------------------------
-- Coupon
-- ----------------------------
CREATE TABLE `coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    `title` VARCHAR(100) NOT NULL COMMENT '优惠券标题',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
    `start_time` DATETIME NOT NULL COMMENT '秒杀开始时间',
    `end_time` DATETIME NOT NULL COMMENT '秒杀结束时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用/正常',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_coupon_time_status` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

-- ----------------------------
-- Coupon Order
-- ----------------------------
CREATE TABLE `coupon_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `coupon_code` VARCHAR(64) NOT NULL COMMENT '券码',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0待发放，1已发放，2失败，3已使用，4已取消',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_coupon_user` (`coupon_id`, `user_id`),
    UNIQUE KEY `uk_coupon_code` (`coupon_code`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_coupon_id` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢券订单表';

-- ----------------------------
-- Coupon Order Failure Compensation
-- ----------------------------
CREATE TABLE `coupon_order_fail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '失败记录ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_time` DATETIME NOT NULL COMMENT '下次重试时间',
    `status` VARCHAR(16) NOT NULL DEFAULT 'INIT' COMMENT 'INIT/RETRYING/SUCCESS/DEAD',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_coupon_order_fail_order_id` (`order_id`),
    KEY `idx_coupon_order_fail_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券订单失败补偿表';

-- ----------------------------
-- Coupon Seckill Failure
-- ----------------------------
CREATE TABLE `coupon_seckill_fail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '失败记录ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `reason` VARCHAR(500) NOT NULL COMMENT '失败原因',
    `status` VARCHAR(16) NOT NULL DEFAULT 'INIT' COMMENT 'INIT/PROCESSED/DEAD',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_coupon_seckill_fail_status_time` (`status`, `create_time`),
    KEY `idx_coupon_seckill_fail_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券秒杀补偿失败记录表';

-- ----------------------------
-- Message Outbox
-- ----------------------------
CREATE TABLE `message_outbox` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `topic` VARCHAR(128) NOT NULL COMMENT 'Kafka topic',
    `payload` TEXT NOT NULL COMMENT '消息内容',
    `status` VARCHAR(16) NOT NULL DEFAULT 'INIT' COMMENT 'INIT/SENT',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_message_outbox_status_create_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表';

-- ----------------------------
-- Seed Data
-- ----------------------------
INSERT INTO `user` (id, username, password, nickname, role, create_user, update_user)
VALUES (1, 'Vanilla_xi', '$2a$10$xxxx', '作者', 1, 'system', 'system');

INSERT INTO `category` (id, name, description, sort, status)
VALUES
    (1, '默认', '默认分区', 0, 1),
    (2, '生活', '生活记录', 10, 1),
    (3, '学习', '学习分享', 20, 1),
    (4, '游戏', '游戏视频', 30, 1),
    (5, '音乐', '音乐视频', 40, 1);

INSERT INTO `videos` (id, title, description, category_id, user_id, video_url, object_key, size, status,
                      like_count, comment_count, favorite_count, view_count, hot_score)
VALUES (1, '示例视频', 'OSS 示例视频', 1, 1,
        'https://video-streaming-system.oss-cn-hangzhou.aliyuncs.com/videos/sample.mp4',
        'videos/sample.mp4', 0, 'PUBLISHED', 0, 1, 0, 0, 105);

INSERT INTO `comments` (id, video_id, user_id, parent_id, root_id, reply_to_user_id, content, hot_score)
VALUES (1, 1, 1, NULL, 1, NULL, '这视频拍得真不错，点赞！', 100);

INSERT INTO `coupon` (id, title, stock, start_time, end_time, status)
VALUES (1, '测试秒杀优惠券', 100, '2026-01-01 00:00:00', '2030-01-01 00:00:00', 1);
