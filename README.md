# VideoStreamingSystem

一个不依赖 Spring / Spring Boot 的视频系统后端项目。项目使用 Servlet + 自定义 IOC / MVC / XML SQL 映射实现后端能力，支持视频上传到阿里云 OSS、评论、收藏、关注、热度排序、Redis 缓存和播放量异步累计。

## 项目特点

- 自定义 IOC：`@MyComponent`、`@MyAutowired`、`BeanFactory`
- 自定义 MVC：`@MyMapping`、`BaseController`、参数绑定、统一 `Result` 响应
- 自定义权限：`@RequireRole` + JWT + `LoginFilter`
- XML SQL 映射：类似 MyBatis 的 SQL 与 Java 分离
- 非 Spring Boot：运行在 Tomcat 10，使用 `jakarta.servlet`
- MySQL 为最终数据源，Redis 作为缓存 / 索引 / 异步计数层

## 技术栈

| 技术 | 说明 |
| --- | --- |
| JDK 17 | 项目编译运行环境 |
| Maven | 构建与依赖管理 |
| Tomcat 10.1 | Servlet 容器 |
| MySQL 8 | 主数据源 |
| Redis | 缓存、ZSet 索引、播放量累计 |
| 阿里云 OSS | 视频文件和头像文件存储 |
| JWT | 登录认证，使用 `Authorization: Bearer {token}` |
| BCrypt | 用户密码加密 |
| Logback | 日志 |

## 当前功能

| 模块 | 功能 |
| --- | --- |
| 用户 | 注册、登录、登出、修改资料、上传头像、查询用户、封号、提权 |
| 视频 | 本地视频文件上传、OSS 存储、查询详情、标题搜索、分区查询、热门 Top50、最新列表、删除视频 |
| 分类 | 查询启用分类列表 |
| 评论 | 一级评论、子评论/楼中楼、两级结构返回、逻辑删除、点赞、按时间/热度排序 |
| 收藏 | 收藏、取消收藏、是否收藏、收藏列表，MySQL + Redis ZSet |
| 关注 | 关注、取关、关注列表、粉丝列表、互关列表、是否关注，MySQL + Redis ZSet |
| 热度 | 视频和评论 `hot_score`，支持 `sort=time` / `sort=hot` |
| 播放量 | 详情页 Redis `INCR`，定时 / 手动异步刷入 MySQL |
| 缓存 | 视频详情、热门列表、最新第一页、分类第一页、评论第一页 |

## 核心目录

```text
video-serve/src/main/java/com/video
├── annotation      # 自定义注解
├── config          # DB/Redis/Servlet 容器启动监听
├── controller      # Servlet Controller
├── filter          # 登录过滤器
├── mapper          # Mapper 接口与实现
├── pojo            # entity / dto
├── proxy           # BeanFactory / AOP 代理
├── service         # Service 接口与实现
├── task            # 定时任务
└── utils           # JWT、Redis、OSS、XML SQL、Lua 等工具
```

## 配置

本地敏感配置文件位于：

```text
video-serve/src/main/resources/properties/
```

请参考 `.example` 文件创建本地配置。

### DB.properties

```properties
db.url=jdbc:mysql://localhost:3306/Video?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
db.username=root
db.password=your_password
```

### Redis.properties

```properties
redis.host=localhost
redis.port=6379
redis.password=
redis.database=0
redis.maxTotal=20
redis.maxIdle=10
```

### JWT.properties

当前 token 过期时间为 1 小时：

```properties
jwt.secret=your_very_long_secret
jwt.ttl=3600000
jwt.tokenName=token
```

### OSS.properties

AccessKey 从环境变量读取，不要写进配置文件：

```properties
oss.endpoint=oss-cn-hangzhou.aliyuncs.com
oss.bucket=video-streaming-system
oss.domain=https://video-streaming-system.oss-cn-hangzhou.aliyuncs.com
```

本地环境变量：

```bash
export OSS_ACCESS_KEY_ID=your_access_key_id
export OSS_ACCESS_KEY_SECRET=your_access_key_secret
```

## 数据库

从 0 建库使用：

```text
video-serve/src/main/resources/db/init.sql
```

已有数据库升级使用 migration，不要删库重建：

```text
sql/migration/20260427_add_favorite_category.sql
sql/migration/20260427_add_hot_score.sql
sql/migration/20260428_add_unique_indexes_for_actions.sql
```

建议按时间顺序执行。

## Redis 设计

### 视频缓存

| key | 说明 | TTL |
| --- | --- | --- |
| `video:list:hot` | 首页热门 Top50 | 逻辑过期 60 秒 |
| `video:list:new` | 最新视频第一页 | 逻辑过期 60 秒 |
| `video:list:category:{categoryId}` | 分类视频第一页 | 逻辑过期 60 秒 |
| `video:detail:{videoId}` | 视频详情 | 逻辑过期 300 秒 |

### 评论缓存

只缓存第一页：

```text
comment:list:video:{videoId}:page1:time
comment:list:video:{videoId}:page1:hot
```

TTL：逻辑过期 60 秒。

### 收藏 ZSet

```text
favorite:video:user:{userId}
```

- member：`videoId`
- score：收藏时间戳
- 不设置过期时间
- key 不存在时从 MySQL 重建

### 关注 ZSet

```text
user:following:{userId}
user:followers:{userId}
user:friends:{userId}
```

- member：用户 ID
- score：关注时间戳
- 不设置过期时间
- key 不存在时从 MySQL 重建

### 播放量累计

详情页访问时只写 Redis：

```text
video:view:count:{videoId}
```

每 1 分钟定时刷入 MySQL，也可以手动调用管理接口刷库。

Lua 脚本独立存放：

```text
video-serve/src/main/resources/lua/get_and_del.lua
```

用于原子读取并删除播放量增量 key，避免 `GET` 后 `DEL` 之间新增播放量被误删。

## 热度计算

视频热度：

```text
hot_score = like_count * 3
          + comment_count * 5
          + favorite_count * 4
          + view_count * 1
          + 时间衰减
```

评论热度：

```text
hot_score = like_count * 3
          + reply_count * 5
          + 时间衰减
```

当前时间衰减在 SQL 中计算：

```sql
GREATEST(0, 100 - TIMESTAMPDIFF(HOUR, create_time, NOW()))
```

## 主要接口

完整 OpenAPI 文档：

```text
openapi.yaml
```

可直接导入 Apifox。

认证请求头：

```http
Authorization: Bearer {token}
```

### 用户

```text
POST   /user/register
POST   /user/login
POST   /user/logout
POST   /user/update
POST   /user/avatar
GET    /user/info
DELETE /user/delete
DELETE /user/remove
POST   /user/promote
```

### 视频

```text
GET    /video/get/id
GET    /video/get/title?title=&page=&pageSize=&sort=time|hot
GET    /video/list/hot
GET    /video/list/new?page=&pageSize=
GET    /video/get/category?categoryId=&page=&pageSize=&sort=time|hot
POST   /video/post
DELETE /video/delete
POST   /video/changeLikes
```

视频上传使用 `multipart/form-data`，字段包括：

```text
title
description
categoryId
file
```

### 评论

```text
GET    /comment/list?videoId=&page=&pageSize=&sort=time|hot
POST   /comment/add
DELETE /comment/delete
POST   /comment/update
```

### 收藏

```text
POST   /favorite/add
DELETE /favorite/cancel
GET    /favorite/check
GET    /favorite/list?page=&pageSize=
```

### 分类

```text
GET /category/list
```

### 关注

```text
GET  /follow/followings
GET  /follow/followers
GET  /follow/friends
GET  /follow/isFollow
POST /follow/changeFollow
```

### 管理

```text
POST /admin/video/view-count/flush
```

用于手动触发播放量 Redis 增量刷库，需要管理员角色。

## 并发安全

- MySQL 是最终数据源
- Redis 只作为缓存 / 索引
- 点赞、收藏、关注均依赖唯一索引防止重复操作
- 写操作先提交 MySQL，再更新 Redis
- Redis 更新失败不回滚 MySQL，后续可从 MySQL 重建
- 播放量刷库使用 Lua 原子 get-and-delete；MySQL 更新失败会 `INCRBY` 补回 Redis

## 构建与运行

```bash
mvn clean package
```

生成 WAR 后部署到 Tomcat 10：

```text
video-serve/target/video-serve.war
```

本地访问示例：

```text
http://localhost:8080/video-serve
```

## 测试

```bash
mvn test
```

当前测试覆盖：

- IOC 容器基础加载
- XML SQL 映射加载
- OSS 文件校验与 Content-Type 推断
- Live OSS 测试按条件跳过

## 注意事项

- 当前项目不是 Spring Boot，不使用 Spring 注解。
- 运行上传视频 / 头像功能前，需要配置阿里云 OSS 环境变量。
- 修改已有数据库请执行 `sql/migration/`，不要直接删库重建。
- `openapi.yaml` 是接口调试入口，建议导入 Apifox 使用。
