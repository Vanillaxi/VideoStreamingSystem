# VideoStreamingSystem

一个不依赖 Spring / Spring Boot 的视频系统后端项目。项目使用 Servlet + 自定义 IOC / MVC / XML SQL 映射实现后端能力，支持视频上传到阿里云 OSS、评论、收藏、关注、Feed 推送、WebSocket 通知、优惠券秒杀、热度排序、Redis 缓存和播放量异步累计。


## 项目特点

- 自定义 IOC：`@MyComponent`、`@MyAutowired`、`BeanFactory`
- 自定义 MVC：`@MyMapping`、`BaseController`、参数绑定、统一 `Result` 响应
- 自定义权限：`@RequireRole` + JWT + `LoginFilter`
- XML SQL 映射：类似 MyBatis 的 SQL 与 Java 分离
- 非 Spring Boot：运行在 Tomcat 10，使用 `jakarta.servlet`
- MySQL 为最终数据源，Redis 作为缓存 / 索引 / 异步计数层
- Kafka 用于视频发布 Feed
- RocketMQ 用于优惠券秒杀事务消息

## 技术栈

| 技术 | 说明 |
| --- | --- |
| JDK 17 | 项目编译运行环境 |
| Maven | 构建与依赖管理 |
| Tomcat 10.1 | Servlet 容器 |
| MySQL 8 | 主数据源 |
| Redis | 缓存、ZSet 索引、播放量累计 |
| Kafka | 视频发布 Feed |
| RocketMQ | 优惠券秒杀事务消息 |
| 阿里云 OSS | 视频文件和头像文件存储 |
| WebSocket | 在线 Feed / 秒杀结果通知 |
| JWT | 登录认证，使用 `Authorization: Bearer {{bearerToken}}` |
| BCrypt | 用户密码加密 |
| Logback | 日志 |

## 当前功能

| 模块 | 功能 |
| --- | --- |
| 用户 | 注册、登录、登出、修改资料、上传头像、查询用户、封号、提权 |
| 用户安全 | 修改密码独立接口，必须校验旧密码；资料更新接口不允许修改 username/password |
| 管理员 | 管理员用户列表、用户搜索、修改角色、封禁用户、优惠券创建/列表/停用 |
| 视频 | 本地视频文件上传、OSS 存储、查询详情、标题搜索、分区查询、热门 Top50、最新列表、Feed 游标分页、删除视频 |
| 分类 | 查询启用分类列表 |
| 评论 | 一级评论、子评论/楼中楼、两级结构返回、逻辑删除、点赞、按时间/热度排序 |
| 收藏 | 收藏、取消收藏、是否收藏、收藏列表，MySQL + Redis ZSet |
| 关注 | 关注、取关、关注列表、粉丝列表、互关列表、是否关注，MySQL + Redis ZSet |
| 热度 | 视频和评论 `hot_score`，支持 `sort=time` / `sort=hot` |
| 播放量 | 详情页 Redis `INCR`，定时 / 手动异步刷入 MySQL |
| 缓存 | 视频详情、热门列表、最新第一页、分类第一页、评论第一页 |
| Feed | 视频发布后 Kafka Consumer 写入粉丝 Redis Feed，并向在线粉丝推送 WebSocket |
| WebSocket | `/ws/{userId}`，支持 `Authorization: Bearer {{bearerToken}}` 或 query `token` 鉴权 |
| 优惠券秒杀 | Redis Lua 预扣、RocketMQ 事务消息、失败补偿、WebSocket 结果推送 |

## 核心目录

```text
video-serve/src/main/java/com/video
├── annotation      # 自定义注解
├── config          # DB/Redis/Servlet 容器启动监听
├── controller      # Servlet Controller
├── filter          # 登录过滤器
├── mapper          # Mapper 接口与实现
├── messageQueue    # Kafka 视频 Feed Producer / Consumer
├── pojo            # entity / dto
├── proxy           # BeanFactory / AOP 代理
├── rocketmq        # 优惠券 RocketMQ Producer / Consumer
├── service         # Service 接口与实现
├── task            # 定时任务
├── utils           # JWT、Redis、OSS、XML SQL、Lua 等工具
└── websocket       # WebSocket Endpoint 与在线用户推送
```

## 配置

本地敏感配置文件位于：

```text
video-serve/src/main/resources/properties/
```

请参考 `application.properties.example` 创建本地配置：

```properties
db.url=jdbc:mysql://localhost:3306/video?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
db.username=root
db.password=your_password

redis.host=localhost
redis.port=6379
redis.password=
redis.database=0
redis.maxTotal=20
redis.maxIdle=10

jwt.secret=your_very_long_secret
jwt.ttl=3600000
jwt.tokenName=token

oss.endpoint=oss-cn-hangzhou.aliyuncs.com
oss.bucket=video-streaming-system
oss.domain=https://video-streaming-system.oss-cn-hangzhou.aliyuncs.com

kafka.bootstrap.servers=localhost:9092
kafka.video.publish.topic=video_published
kafka.video.notify.group.id=video-publish-notify-group
kafka.video.notification.group.id=video-publish-notification-group
kafka.video.feed-cache.group.id=video-feed-cache-group

rocketmq.namesrvAddr=localhost:9876
rocketmq.topic.couponSeckillTx=coupon_seckill_tx
rocketmq.producerGroup.couponSeckillTx=coupon_seckill_tx_producer_group
rocketmq.consumerGroup.couponSeckillTx=coupon_seckill_tx_consumer_group
rocketmq.consumerMaxReconsumeTimes.couponSeckillTx=16

nacos.serverAddr=localhost:8848
sentinel.nacos.group=SENTINEL_GROUP
sentinel.nacos.flowDataId=video-system-flow-rules
sentinel.nacos.paramFlowDataId=video-system-param-flow-rules
sentinel.nacos.degradeDataId=video-system-degrade-rules
sentinel.nacos.enabled=false
```

AccessKey 从环境变量读取，不要写进配置文件：

```bash
export OSS_ACCESS_KEY_ID=your_access_key_id
export OSS_ACCESS_KEY_SECRET=your_access_key_secret
```

当前 token 过期时间为 1 小时：

```properties
jwt.ttl=3600000
```

## 数据库

从 0 建库使用：

```text
video-serve/src/main/resources/db/init.sql
```

已有数据库升级请执行 `sql/migration/` 中的增量 SQL；本次事务消息补偿失败表为：

```text
sql/migration/20260430_add_coupon_seckill_fail.sql
```

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
video-serve/src/main/resources/lua/coupon_seckill.lua
```

用于原子读取并删除播放量增量 key，避免 `GET` 后 `DEL` 之间新增播放量被误删。

### 优惠券秒杀 Redis Key

```text
coupon:seckill:stock:{couponId}
coupon:seckill:users:{couponId}
```

管理员停用优惠券时，后端先更新 MySQL，将 `coupon.status` 改为 `0`，再删除：

```text
coupon:seckill:stock:{couponId}
coupon:seckill:users:{couponId}
```

当前普通优惠券列表接口固定只展示 `status=1` 的可用券；管理员优惠券列表可查看全部或按状态筛选。

Lua 返回码：

```text
0 成功
1 未开始
2 已结束
3 库存不足
4 重复抢券
```

## Kafka / RocketMQ

| topic | 说明 |
| --- | --- |
| `video_published` | Kafka：视频发布事件总线，异步处理 WebSocket 通知、站内通知、Feed 最新缓存刷新 |
| `coupon_seckill_tx` | RocketMQ：优惠券事务消息主链路，提交后推送抢券成功 |

WebSocket 只是实时通知通道，不是事件总线，也不是最终数据源：

- 视频发布实时通知来源：Kafka `video_published` -> `VideoPublishNotifyConsumer`
- 优惠券抢券实时通知来源：RocketMQ `coupon_seckill_tx` -> `CouponSeckillTxConsumer`
- 两条链路互不替代；Kafka 视频发布改造不会取消优惠券 `COUPON_SECKILL_RESULT`

`message_outbox` 表结构暂时保留用于安全回滚，但当前秒杀主链路不再写入、不再读取，也不再启动 Outbox 定时任务。

视频发布事件链路：

```text
/video/post
-> MySQL videos 落库成功
-> Kafka topic video_published
-> VideoPublishNotifyConsumer：给在线粉丝推 WebSocket
-> VideoPublishNotificationConsumer：写 notification 表
-> VideoFeedCacheConsumer：写 Redis feed:latest，删除旧列表/详情缓存
```

秒杀事务消息链路：

```text
Redis Lua 预扣
-> WebSocket PROCESSING
-> RocketMQ 半消息 coupon_seckill_tx
-> executeLocalTransaction 写 coupon_order
-> COMMIT_MESSAGE
-> Consumer 消费已提交消息
-> WebSocket SUCCESS
```

优惠券 WebSocket 消息示例：

```json
{"type":"COUPON_SECKILL_RESULT","status":"PROCESSING","couponId":1,"couponCode":null,"message":"抢券请求已受理"}
```

```json
{"type":"COUPON_SECKILL_RESULT","status":"SUCCESS","couponId":1,"couponCode":"CPN-XXXX-XXXX-XXXX-XXXX","message":"抢券成功"}
```

```json
{"type":"COUPON_SECKILL_RESULT","status":"FAILED","couponId":1,"couponCode":null,"message":"抢券失败，请稍后重试"}
```

如果 Redis 预扣成功后事务消息发送失败、本地事务回滚或事务回查确认失败，会执行 Redis 补偿；补偿失败会写入 `coupon_seckill_fail`，便于后续人工处理。

## Sentinel

秒杀入口 `/coupon/seckill/preDeduct` 使用 Sentinel 保护，资源名固定为：

```text
coupon_seckill_pre_deduct
```

当 `sentinel.nacos.enabled=true` 时，规则从 Nacos 拉取，项目启动时注册 Sentinel Nacos DataSource。代码里不再硬编码调用
`FlowRuleManager.loadRules`、`ParamFlowRuleManager.loadRules`、`DegradeRuleManager.loadRules`。

单体部署时可以关闭 Sentinel Nacos 动态监听，保留 Sentinel 本地限流能力：

```properties
sentinel.nacos.enabled=false
```

新增 Maven 依赖：

```xml
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
    <version>1.8.6</version>
</dependency>
```

Nacos 配置项：

```properties
nacos.serverAddr=localhost:8848
sentinel.nacos.group=SENTINEL_GROUP
sentinel.nacos.flowDataId=video-system-flow-rules
sentinel.nacos.paramFlowDataId=video-system-param-flow-rules
sentinel.nacos.degradeDataId=video-system-degrade-rules
```

建议在 Nacos 中创建以下 JSON 配置。

`video-system-flow-rules`：

```json
[
  {
    "resource": "coupon_seckill_pre_deduct",
    "grade": 1,
    "count": 100,
    "limitApp": "default",
    "strategy": 0,
    "controlBehavior": 0
  }
]
```

`video-system-param-flow-rules`：

```json
[
  {
    "resource": "coupon_seckill_pre_deduct",
    "paramIdx": 0,
    "count": 100,
    "durationInSec": 1,
    "paramFlowItemList": [
      {
        "object": "5",
        "classType": "java.lang.Long",
        "count": 20
      }
    ]
  }
]
```

`video-system-degrade-rules`：

```json
[
  {
    "resource": "coupon_seckill_pre_deduct",
    "grade": 0,
    "count": 300,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "slowRatioThreshold": 0.5,
    "statIntervalMs": 1000
  },
  {
    "resource": "coupon_seckill_pre_deduct",
    "grade": 1,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 1000
  }
]
```

规则含义：

| 类型 | 规则 | 返回信息 |
| --- | --- | --- |
| 普通 QPS 限流 | QPS=100 | `系统繁忙，请稍后再试` |
| 热点参数限流 | 按 `couponId` 限流，默认 QPS=100 | `当前优惠券过于火爆，请稍后再试` |
| 热点参数限流特殊值 | `couponId=5`，QPS=20 | `当前优惠券过于火爆，请稍后再试` |
| 熔断降级 | Sentinel 降级规则触发时 | `服务暂时不可用，请稍后再试` |

限流或降级时不执行 Redis Lua，也不会发送 RocketMQ 事务消息，接口统一返回：

```json
{"code":0,"msg":"对应错误信息","data":null}
```

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
Authorization: Bearer {{bearerToken}}
```

### 用户

```text
POST   /user/register
POST   /user/login
POST   /user/logout
POST   /user/update
PUT    /user/password
POST   /user/avatar
GET    /user/info
DELETE /user/delete
DELETE /user/remove
POST   /user/promote
```

说明：

- 所有登录后接口使用 `Authorization: Bearer {{bearerToken}}`
- `/user/update` 只用于修改展示资料，不允许修改 `username/password`
- `/user/password` 请求体为 `oldPassword/newPassword/confirmPassword`，后端会校验旧密码

### 视频

```text
GET    /video/get/id
GET    /video/get/title?title=&page=&pageSize=&sort=time|hot
GET    /video/list/hot
GET    /video/list/hot/cursor?pageSize=&cursorHotScore=&cursorCreateTime=&cursorId=
GET    /video/feed/cursor?sort=time|hot&pageSize=&cursorHotScore=&cursorCreateTime=&cursorId=
GET    /video/feed/following?pageSize=&cursorCreateTime=&cursorId=
GET    /video/list/new?page=&pageSize=
GET    /video/get/category?categoryId=&page=&pageSize=&sort=time|hot
POST   /video/post
DELETE /video/delete
POST   /video/changeLikes
```

首页“最新/热门”推荐流使用 `/video/feed/cursor`。第一页只传：

```text
sort=time&pageSize=20
```

后续页：

- `sort=time`：传上一页返回的 `nextCreateTime` 和 `nextId`
- `sort=hot`：传上一页返回的 `nextHotScore`、`nextCreateTime` 和 `nextId`
- 空字符串游标会按 `null` 处理，不会再触发 `NumberFormatException: empty String`

关注动态页面使用 `/video/feed/following`。第一版直接查 MySQL：

- 首页：只传 `pageSize=20`
- 后续页：传上一页返回的 `nextCreateTime` 和 `nextId`
- 当前 `videos.status` 是字符串，SQL 使用 `status='PUBLISHED'`

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

### Feed

```text
GET /feed/following
GET /video/feed/following
```

`/feed/following` 是旧版 Redis 收件箱接口；新关注动态页使用 `/video/feed/following`，直接从 MySQL 查询关注作者的视频，准确性优先。

### 优惠券

```text
GET  /coupon/list
GET  /coupon/detail?couponId=
POST /coupon/seckill/preDeduct
GET  /coupon/order/status?couponId=
```

`/coupon/seckill/preDeduct` 是当前唯一抢券入口，使用 Redis Lua 预扣 + RocketMQ 事务消息。

抢券 WebSocket 实时结果仍走优惠券 RocketMQ 链路，不走 Kafka `video_published`：

```text
COUPON_SECKILL_RESULT
```

普通用户 `/coupon/list` 当前只返回 `status=1` 的可用优惠券。

该接口已接入 Sentinel：

- 普通限流：`系统繁忙，请稍后再试`
- 热点参数限流：`当前优惠券过于火爆，请稍后再试`
- 熔断降级：`服务暂时不可用，请稍后再试`

### WebSocket

```text
ws://localhost:8080/video-serve/ws/{userId}
```

Apifox 中请新建 WebSocket 请求，协议选择 `ws`。认证方式：

```text
Authorization: Bearer {{bearerToken}}
```

也兼容：

```text
ws://localhost:8080/video-serve/ws/{userId}?token={{bearerToken}}
```

### 管理

```text
GET  /admin/users?page=&pageSize=
GET  /admin/users/search?nickname=&username=&page=&pageSize=
POST /user/promote?userId=&role=
DELETE /user/remove?id=
POST /admin/coupon/create
GET  /admin/coupon/list?status=&page=&pageSize=
DELETE /admin/coupon/delete?couponId=
POST /admin/video/view-count/flush
```

管理员接口需要 `role >= 2`。权限细节：

- role=2 可以管理普通用户/会员和优惠券
- role=2 不允许操作 role=3 用户
- role=2 不允许把用户提升为 role=2/3
- role=3 可以把低等级用户设置为 role=2/3

`/admin/coupon/create` 用于内部创建优惠券。创建成功后会写入 MySQL，并初始化 Redis 秒杀库存 key。

`/admin/coupon/delete` 当前实现是逻辑停用优惠券，将 `status` 改为 `0`，并删除秒杀 Redis key。

`/admin/video/view-count/flush` 用于手动触发播放量 Redis 增量刷库，需要管理员角色。

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
video-serve/target/video-serve-1.0-SNAPSHOT.war
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

### 秒杀链路本地测试

1. 启动 RocketMQ：

```bash
nohup sh bin/mqnamesrv &
nohup sh bin/mqbroker -n localhost:9876 &
```

2. 创建优惠券 topic：

```bash
sh bin/mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t coupon_seckill_tx
```

3. 复制并配置：

```text
video-serve/src/main/resources/properties/application.properties
```

4. 启动 Tomcat，连接 WebSocket：

```text
ws://localhost:8080/video-serve/ws/{userId}
```

5. 使用管理员 token 创建优惠券：

```text
POST /admin/coupon/create
```

6. 使用普通用户 token 抢券：

```text
POST /coupon/seckill/preDeduct
```

7. 检查结果：

```sql
SELECT * FROM coupon_order WHERE coupon_id = ?;
SELECT * FROM coupon_seckill_fail ORDER BY id DESC LIMIT 10;
```

预期：`coupon_order` 生成订单，WebSocket 先收到 `PROCESSING`，再收到 `SUCCESS`。

异常测试：

- 重复抢券：再次调用 `/coupon/seckill/preDeduct`，Lua 返回重复抢券错误码。
- 本地事务失败：临时制造数据库不可写或唯一键冲突，观察 RocketMQ 本地事务日志和 Redis 旧 key 补偿。
- 事务回查：在 RocketMQ 日志中观察 `checkLocalTransaction`，订单存在则提交，订单不存在则回滚。
- Consumer 重试：临时抛出系统异常时，RocketMQ 会按 `rocketmq.consumerMaxReconsumeTimes.couponSeckillTx` 重试。
- 死信 DLQ：超过最大重试后进入 `%DLQ%coupon_seckill_tx_consumer_group`，可用 `mqadmin queryMsgByTopic` 或控制台查看。
- Redis 补偿失败：检查 `coupon_seckill_fail` 表，根据 `order_id/coupon_id/user_id/reason` 人工处理后重发或补偿。

## 注意事项

- 当前项目不是 Spring Boot，不使用 Spring 注解。
- 运行上传视频 / 头像功能前，需要配置阿里云 OSS 环境变量。
- 使用 Feed 链路前，需要启动 Kafka，并创建 `video_published` topic。
- 使用秒杀异步链路前，需要启动 RocketMQ，并创建 `coupon_seckill_tx` topic。
- `openapi.yaml` 是接口调试入口，建议导入 Apifox 使用。由于OpenAPI本质是对 HTTP 规范，对WebSocket 支持是文档级而非执行级，因此需要我们手动在 apifox里面修改接口测试
