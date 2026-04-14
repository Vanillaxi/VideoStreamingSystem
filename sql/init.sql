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
                        `role`        TINYINT DEFAULT 0 COMMENT '0-普通用户，1-管理员',
                        `create_user` VARCHAR(32) DEFAULT 'system' COMMENT '创建人',
                        `update_user` VARCHAR(32) COMMENT '更新人',
                        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 3. 创建视频表 (Videos)
-- ----------------------------
DROP TABLE IF EXISTS `videos`;
CREATE TABLE `videos` (
                          `id`          BIGINT NOT NULL AUTO_INCREMENT,
                          `title`       VARCHAR(100) NOT NULL COMMENT '视频标题',
                          `url`         VARCHAR(255) NOT NULL COMMENT '视频路径/文件路径',
                          `user_id`     BIGINT NOT NULL COMMENT '上传者ID',
                          `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (`id`),
                          CONSTRAINT `fk_video_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 4. 创建粉丝关注表 (User Follow)
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
-- 5. 创建点赞表 (Video Like)
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
-- 6. 创建评论表 (Comments)
-- ----------------------------
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments` (
                            `id`          INT PRIMARY KEY AUTO_INCREMENT,
                            `video_id`    INT NOT NULL COMMENT '所属视频ID',
                            `user_id`     INT NOT NULL COMMENT '发表者用户ID',
                            `content`     TEXT NOT NULL COMMENT '评论内容',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 7. 插入初始测试数据
-- ----------------------------
-- 注意：插入数据前请确保对应的 ID 存在
INSERT INTO `user` (id, username, password, nickname, role)
VALUES (1, 'Vanilla_xi', '$2a$10$xxxx', '作者', 1);

INSERT INTO `videos` (id, title, url, user_id)
VALUES (1, '动物', 'https://www.w3schools.com/html/mov_bbb.mp4', 1);

INSERT INTO `comments` (video_id, user_id, content)
VALUES (1, 1, '这视频拍得真不错，点赞！');