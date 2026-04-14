# 🎬 视频推流系统 (VideoStreamingSystem)

本项目是一个基于 **Java Web 原生技术栈** 实现的视频互动后端系统。通过手写底层实现，深入实践了 Servlet 生命周期、JDBC 数据库连接池封装、以及基于 JWT 的前后端权限校验。



## 📋 依赖环境 (Prerequisites)

在运行本项目之前，请确保您的开发环境已安装以下软件：

| **依赖**   | **版本要求** | **说明**                                               |
| ---------- | ------------ | ------------------------------------------------------ |
| **JDK**    | 17 或更高    | 推荐使用 OpenJDK 17                                    |
| **Maven**  | 3.8+         | 用于项目构建和依赖管理                                 |
| **MySQL**  | 8.0.x        | 数据库存储                                             |
| **Tomcat** | 10.1.x       | Servlet 容器 (注意：Tomcat 10 之后使用 `jakarta` 包名) |
| **Apifox** | 最新版       | 接口调试与文档查看                                     |

------



## 🚀 运行指南 (Quick Start)

### 1. 数据库初始化

1. 创建数据库 `video_system`。
2. 运行项目根目录下 `sql/` 目录中的 SQL 脚本以初始化表结构和基础数据。

### 2. 配置文件修改

本项目敏感配置已通过 `.gitignore` 忽略，请在 `src/main/resources/mapper` 目录下，根据对应的 `.example` 文件创建本地配置文件：

1. **DB.properties**: 配置 MySQL 数据库连接。
2. **JWT.properties**: 
   - `jwt.secret`: 请设置一个复杂的长字符串作为签名密钥。
   - `jwt.ttl`: Token 过期时间（毫秒）。
3. **Redis.properties**: 配置 Redis 主机地址及端口。

> **注意**：必须完成上述配置重命名并填写正确参数，否则项目启动时 `BeanFactory` 初始化连接池将报错。



### 3. 构建与部署

- **本地 IDE 运行**:

  1. 在 IntelliJ IDEA 中配置 Tomcat 10 服务器。
  2. 设置 **Deployment** 路径为 `videoSystem` (Context Path)。
  3. 点击运行。

- **命令行构建**:

  Bash

  ```
  mvn clean package
  ```

  将生成的 `.war` 文件拷贝至 Tomcat 的 `webapps` 目录下即可自动部署。

------



## 📖 API 接口说明

本项目采用 **Apifox** 进行 API 管理，支持在线调试：

🔗 **[点击跳转：在线 API 接口文档](https://www.google.com/search?q=https://s.apifox.cn/44e8c9f0-faee-43fc-9ed3-2f60263bd208/442060787e0)**

### 核心接口预览：

- **用户模块**: 注册、登录（返回 JWT Token）。
- **视频模块**: 视频详情展示。
- **评论模块**: 发表评论、查看视频评论列表。

------



## 🛠️ 技术核心实现

- **统一结果封装**: 使用 `Result<T>` 泛型类包装所有响应，包含 `code`, `data`, `msg`。
- **BaseDAO 封装**: 利用 **反射 (Reflection)** 和 **泛型** 极大减少了重复的 SQL 映射代码。
- **权限拦截器**: 结合 `Filter` 与 `JWT`，实现对 `/comment/add` 等敏感接口的登录校验。
- **ThreadLocal 状态管理**: 通过 `UserHolder` 工具类，在同一个请求线程中优雅地共享用户信息。

------



## 👤 作者

Vanilla_xi

