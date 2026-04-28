-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS Video DEFAULT CHARACTER SET utf8mb4;
USE Video;

-- ----------------------------
-- 2. 创建用户表 (User)
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
                        `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键id',
                        `username`    VARCHAR(32) NOT NULL UNIQUE COMMENT '用户名',
                        `password`    VARCHAR(64) NOT NULL COMMENT 'BCrypt加密后的密码',
                        `nickname`    VARCHAR(50) DEFAULT '用户' COMMENT '昵称',
                        `avatar_url`  VARCHAR(500) COMMENT '头像访问地址',
                        `avatar_object_key` VARCHAR(255) COMMENT '头像OSS对象Key',
                        `role`        TINYINT DEFAULT 0 COMMENT '0-普通用户，1-管理员',
                        `create_user` VARCHAR(32) DEFAULT 'system' COMMENT '创建人',
                        `update_user` VARCHAR(32) COMMENT '更新人',
                        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 3. 创建视频分区表 (Category)
-- ----------------------------
DROP TABLE IF EXISTS `category`;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 4. 创建视频表 (Videos)
-- ----------------------------
DROP TABLE IF EXISTS `videos`;
CREATE TABLE `videos` (
                          `id`          BIGINT NOT NULL AUTO_INCREMENT,
                          `title`       VARCHAR(100) NOT NULL COMMENT '视频标题',
                          `description` TEXT COMMENT '视频描述',
                          `category_id` BIGINT COMMENT '分类ID',
                          `user_id`     BIGINT NOT NULL COMMENT '上传者ID',
                          `video_url`   VARCHAR(500) NOT NULL COMMENT 'OSS视频访问地址',
                          `object_key`  VARCHAR(255) NOT NULL COMMENT 'OSS对象Key',
                          `size`        BIGINT NOT NULL COMMENT '视频大小，单位字节',
                          `status`      VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '视频状态',
                          `like_count` BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数',
                          `comment_count` BIGINT NOT NULL DEFAULT 0 COMMENT '评论数',
                          `favorite_count` BIGINT NOT NULL DEFAULT 0 COMMENT '收藏数',
                          `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '播放数',
                          `hot_score` DOUBLE NOT NULL DEFAULT 0 COMMENT '热度分',
                          `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                          `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (`id`),
                          KEY `idx_videos_category_time` (`category_id`, `create_time`),
                          KEY `idx_videos_hot_score` (`hot_score`, `create_time`),
                          CONSTRAINT `fk_video_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 5. 创建粉丝关注表 (User Follow)
-- ----------------------------
DROP TABLE IF EXISTS `user_follow`;
CREATE TABLE `user_follow` (
                               `id`           BIGINT NOT NULL AUTO_INCREMENT,
                               `follower_id`  BIGINT NOT NULL COMMENT '关注者id',
                               `following_id` BIGINT NOT NULL COMMENT '被关注者id',
                               `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_follow` (`follower_id`, `following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 6. 创建点赞表 (Video Like)
-- ----------------------------
DROP TABLE IF EXISTS `video_like`;
CREATE TABLE `video_like` (
                              `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
                              `user_id`     BIGINT NOT NULL COMMENT '用户id',
                              `video_id`    BIGINT NOT NULL COMMENT '视频id',
                              `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE KEY `uk_user_video` (`user_id`, `video_id`) -- 防止重复点赞
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- ----------------------------
-- 7. 创建收藏表 (Video Favorite)
-- ----------------------------
DROP TABLE IF EXISTS `video_favorite`;
CREATE TABLE `video_favorite` (
                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `video_id` BIGINT NOT NULL COMMENT '视频ID',
                                  `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_video_favorite` (`video_id`, `user_id`),
                                  KEY `idx_video_favorite_user_time` (`user_id`, `create_time`),
                                  KEY `idx_video_favorite_video` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 8. 创建评论表 (Comments)
-- ----------------------------
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments` (
                            `id`               BIGINT PRIMARY KEY AUTO_INCREMENT,
                            `video_id`         BIGINT NOT NULL COMMENT '所属视频ID',
                            `user_id`          BIGINT NOT NULL COMMENT '发表者用户ID',
                            `parent_id`        BIGINT NULL COMMENT '直接回复的评论ID',
                            `root_id`          BIGINT NULL COMMENT '所属一级评论ID',
                            `reply_to_user_id` BIGINT NULL COMMENT '回复的用户ID',
                            `content`          TEXT NOT NULL COMMENT '评论内容',
                            `like_count`       BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数',
                            `reply_count`      BIGINT NOT NULL DEFAULT 0 COMMENT '回复数',
                            `hot_score`        DOUBLE NOT NULL DEFAULT 0 COMMENT '热度分',
                            `deleted`          TINYINT NOT NULL DEFAULT 0 COMMENT '0未删除，1已删除',
                            `create_time`      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            KEY `idx_comments_video_parent_time` (`video_id`, `parent_id`, `create_time`),
                            KEY `idx_comments_video_parent_hot` (`video_id`, `parent_id`, `hot_score`, `create_time`),
                            KEY `idx_comments_root_time` (`root_id`, `create_time`),
                            KEY `idx_comments_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 9. 插入初始测试数据
-- ----------------------------
-- 注意：插入数据前请确保对应的 ID 存在
INSERT INTO `user` (id, username, password, nickname, role)
VALUES (1, 'Vanilla_xi', '$2a$10$xxxx', '作者', 1);

INSERT INTO `category` (id, name, description, sort, status)
VALUES
    (1, '默认', '默认分区', 0, 1),
    (2, '生活', '生活记录', 10, 1),
    (3, '学习', '学习分享', 20, 1),
    (4, '游戏', '游戏视频', 30, 1),
    (5, '音乐', '音乐视频', 40, 1);

INSERT INTO `videos` (id, title, description, category_id, user_id, video_url, object_key, size, status, like_count, comment_count, favorite_count, view_count, hot_score)
VALUES (1, '示例视频', 'OSS 示例视频', 1, 1,
        'https://video-streaming-system.oss-cn-hangzhou.aliyuncs.com/videos/sample.mp4',
        'videos/sample.mp4', 0, 'PUBLISHED', 0, 1, 0, 0, 105);

INSERT INTO `comments` (id, video_id, user_id, parent_id, root_id, reply_to_user_id, content, hot_score)
VALUES (1, 1, 1, NULL, 1, NULL, '这视频拍得真不错，点赞！', 100);
