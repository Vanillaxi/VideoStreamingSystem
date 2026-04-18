#  VideoStreamingSystem - 视频推流系统



## 项目简介

本项目是一个**完全手写框架**的视频推流后端系统，**不依赖 Spring 任何组件**，从零实现了：

-  **自定义 IOC 容器** — 注解扫描、Bean 管理、依赖注入
-  **自定义 AOP 切面编程** — JDK 动态代理实现 Service 层拦截
-  **自定义 MVC 框架** — 路由映射、参数绑定、统一响应
- **自定义 ORM 映射** — XML SQL 解析（类 MyBatis 思想）



---

##  核心功能模块

| 模块         | 功能描述                                                  |
| ------------ | --------------------------------------------------------- |
| **用户系统** | 注册 / 登录 / 登出 / 信息修改 / 账号注销 / 角色权限管理   |
| **视频管理** | 发布视频 / 按ID查询 / 模糊搜索 / 点赞                     |
| **评论系统** | 发表评论 / 分页查看(含点赞数) / 删除评论 / 评论点赞       |
| **关注系统** | 关注/取关 / 关注列表 / 粉丝列表 / 互粉列表 / 是否关注判断 |
| **安全机制** | JWT Token 认证 + BCrypt 密码加密 + RBAC 权限控制          |
| **缓存策略** | Redis 缓存热点数据，减轻数据库压力                        |

---



### 核心组件

| 组件             | 说明                                | 对应文件                                                     |
| ---------------- | ----------------------------------- | ------------------------------------------------------------ |
| `@MyComponent`   | Bean 标记注解                       | [MyComponent.java](video-serve/src/main/java/com/video/annotation/MyComponent.java) |
| `@MyAutowired`   | 依赖注入注解                        | [MyAutowired.java](video-serve/src/main/java/com/video/annotation/MyAutowired.java) |
| `@MyMapping`     | 路由映射注解 (支持 GET/POST/DELETE) | [MyMapping.java](video-serve/src/main/java/com/video/annotation/MyMapping.java) |
| `@MyHeader`      | 请求头参数绑定注解                  | [MyHeader.java](video-serve/src/main/java/com/video/annotation/MyHeader.java) |
| `@RequireRole`   | 角色权限校验注解                    | [RequireRole.java](video-serve/src/main/java/com/video/annotation/RequireRole.java) |
| `BeanFactory`    | IOC 容器核心 (扫描/实例化/DI/AOP)   | [BeanFactory.java](video-serve/src/main/java/com/video/proxy/BeanFactory.java) |
| `ServiceProxy`   | JDK 动态代理 (AOP 切面)             | [ServiceProxy.java](video-serve/src/main/java/com/video/proxy/ServiceProxy.java) |
| `BaseController` | 自定义 MVC 基类 (路由分发/参数解析) | [BaseController.java](video-serve/src/main/java/com/video/controller/BaseController.java) |

---

##  技术栈

*  MySQL数据库：实现数据的持久化

* Redis 缓存：加速数据的访问

* BCrypt 加密

  

---



## 快速开始

### 环境要求

| 依赖       | 版本要求 | 说明                               |
| ---------- | -------- | ---------------------------------- |
| **JDK**    | 17+      | 推荐 OpenJDK 17                    |
| **Maven**  | 3.8+     | 项目构建与依赖管理                 |
| **MySQL**  | 8.0.x    | 关系型数据库                       |
| **Redis**  | 6.0+     | 缓存中间件                         |
| **Tomcat** | 10.1.x   | Servlet 容器 (使用 `jakarta` 包名) |

###  克隆项目

```bash
git clone https://github.com/你的用户名/VideoStreamingSystem.git
cd VideoStreamingSystem
```

###  

### 数据库初始化

**该脚本暂未更新，请自行编写**

结构：

- `user` — 用户表（含角色权限字段）
- `videos` — 视频表
- `user_follow` — 关注关系表
- `video_like` — 视频点赞表
- `comments` — 评论表



###  配置文件

项目敏感配置已通过 `.gitignore` 忽略。请根据 `properties/` 目录下的 `.example` 文件创建本地配置：

#### DB.properties — 数据库连接

```properties
db.url=jdbc:mysql://localhost:3306/Video?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
db.username=root
db.password=your_password
```

#### JWT.properties — JWT 配置

```properties
jwt.secret=your_very_long_and_complex_secret_key_here
jwt.ttl=604800000
jwt.tokenName=token
```

#### Redis.properties — Redis 连接

```properties
redis.host=localhost
redis.port=6379
redis.password=
redis.database=0
redis.maxTotal=20
redis.maxIdle=10
```

###  构建与部署

```bash
# Maven 打包
mvn clean package -DskipTests

# 将生成的 video-serve.war 部署到 Tomcat webapps 目录
# 启动 Tomcat，访问 http://localhost:8080/video-serve/
```

---

##  API 接口文档

本项目提供完整的 RESTful API，支持在线调试：

🔗 **[Apifox 在线文档](https://s.apifox.cn/3e7de016-3c19-4e6a-91e1-1558a9781ee7)**

---



##  核心设计

### 1. 自定义 IOC 容器

通过 `BeanFactory` 实现 Spring Core 的核心功能：

- **包扫描**: 递归扫描指定包下带 `@MyComponent` 注解的类
- **单例管理**: 使用 Map 维护 Bean 单例池
- **依赖注入**: 通过 `@MyAutowired` 按类型自动装配
- **AOP 包装**: 对所有 ServiceImpl 创建 JDK 动态代理

### 2. 自定义 MVC 框架

`BaseController` 作为所有 Controller 的基类：

- **路由分发**: 通过 `@MyMapping` 注解实现方法级路由映射
- **参数绑定**: 支持 QueryParam、RequestBody、Header 参数自动绑定
- **统一响应**: 所有接口返回标准 `Result` 格式
- **异常处理**: 全局捕获业务异常和系统异常

### 3. XML SQL 映射

仿 MyBatis 设计，使用 dom4j 解析 XML 中的 SQL 语句，实现 SQL 与代码分离。

### 4. 多层安全防护

- **JWT Token**: 无状态认证，支持过期时间配置
- **BCrypt 加密**: 密码单向哈希存储
- **RBAC 权限**: 基于 `@RequireRole` 注解的角色级别控制
- **LoginFilter**: 过滤器链实现请求拦截

---

##  开发指南

### 运行测试

项目包含单元测试，覆盖以下场景：

```bash
mvn test
```

测试用例位于 [test/java/com/video/test/](video-serve/src/test/java/com/video/test/)：

- `IocTest` — IOC 容器测试
- `DBTest` — 数据库连接测试
- `RedisTest` — Redis 连接测试
- `CachePreheatTest` — 缓存预热测试

### 日志配置

日志通过 Logback 配置，配置文件：[logback.xml](video-serve/src/main/resources/logback.xml)



