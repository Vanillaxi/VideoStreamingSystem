---
title: Video
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
code_clipboard: true
highlight_theme: darkula
headingLevel: 2
generator: "@tarslib/widdershins v4.0.30"

---

# Video

Base URLs:

# Authentication

# User相关接口

## POST 注册

POST /user/register

> Body 请求参数

```json
"{\r\n    \"username\": \"香草\"\r\n    \"password\": \"123456\"\r\n    \"nickname\":\"香草\"\r\n}"
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|object| 是 |none|
|» username|body|string| 是 |用户名|
|» password|body|string| 是 |密码|
|» nickname|body|string| 是 |昵称|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "注册成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据|
|» msg|string|true|none||提示信息|

## POST 登录

POST /user/login

> Body 请求参数

```json
{
  "username": "香草",
  "password": "123456"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|object| 是 |none|
|» username|body|string| 是 |用户名|
|» password|body|string| 是 |密码|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJWYW5pbGxhX3hpIiwicm9sZSI6MywiaWF0IjoxNzc2NDA5MDczLCJleHAiOjE3NzcwMTM4NzN9.7xUEBc2WqwKqAfK7EH043HZ5agdD6iW-xS2IjXi8o5M",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据（token）|
|» msg|string|true|none||提示信息|

## POST 登出

POST /user/logout

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|token|header|string| 是 |token|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "退出成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据|
|» msg|string|true|none||提示信息|

## POST 修改用户信息

POST /user/update

> Body 请求参数

```json
{
  "password": "123456",
  "nickname": "Vanilla_xi"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|token|header|string| 是 |token|
|body|body|object| 是 |none|
|» password|body|string| 否 |密码|
|» nickname|body|string| 否 |昵称|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "个人资料修改成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据|
|» msg|string|true|none||提示信息|

## DELETE 注销自己

DELETE /user/delete

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|token|header|string| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "账号注销成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据|
|» msg|string|true|none||提示信息|

## DELETE 封号

DELETE /user/remove

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|integer| 是 |用户id|
|token|header|string| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "封号成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据|
|» msg|string|true|none||提示信息|

## POST 提权

POST /user/promote

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|userId|query|integer| 是 |待提权者id|
|role|query|integer| 是 |提升等级|
|token|header|string| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "提权成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据|
|» msg|string|true|none||提示信息|

## GET 查询用户信息

GET /user/info

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|integer| 是 |none|
|token|header|string| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": {
    "id": 3,
    "nickname": "香草",
    "password": "$2a$10$hDqsaDbdf7qULiKF0ifX3uXrgfg4bLFk.QFAE49EK4QcFihEBqTGy",
    "role": 1,
    "username": "香草"
  },
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» data|object|true|none||none|
|»» id|integer|true|none||none|
|»» nickname|string|true|none||none|
|»» password|string|true|none||none|
|»» role|integer|true|none||none|
|»» username|string|true|none||none|
|» msg|string|true|none||none|

# Video相关接口

## GET 根据videoId查看视频（含点赞数）

GET /video/get/id

> Body 请求参数

```yaml
{}

```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|string| 是 |视频id|
|token|header|string| 是 |token|
|body|body|object| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": {
    "createTime": "2026-04-18 11:35:37",
    "id": 3,
    "likesCount": 0,
    "title": "游玩",
    "url": "https://www.bilibili.com/video/BV18bmVY7EMy/?spm_id_from=333.337.search-card.all.click",
    "userId": 1
  },
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|object|true|none||数据|
|»» createTime|string|true|none||视频发布时间|
|»» id|integer|true|none||视频id|
|»» likesCount|integer|true|none||点赞数|
|»» title|string|true|none||视频标题|
|»» url|string|true|none||视频链接|
|»» userId|integer|true|none||博主id|
|» msg|string|true|none||提示信息|

## POST 更改点赞

POST /video/changeLikes

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|videoId|query|string| 是 |视频id|
|token|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "更新成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据|
|» msg|string|true|none||提示信息|

## POST 发表视频

POST /video/post

> Body 请求参数

```json
{
  "title": "游玩",
  "url": "https://www.bilibili.com/video/BV1Py5izFEYK/?spm_id_from=333.337.search-card.all.click&vd_source=409122dde074c21994778f85f11a9d6e"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|title|query|string| 否 |none|
|url|query|string| 否 |none|
|token|header|string| 是 |none|
|body|body|object| 是 |none|
|» title|body|string| 是 |视频名|
|» url|body|string| 是 |视频路径|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "发布成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据|
|» msg|string|true|none||提示信息|

## GET 模糊查询视频（title)

GET /video/get/title

> Body 请求参数

```json
"{\r\n    \"title\":\"游玩\"\r\n    \"page\":\"1\"\r\n    \"pageSize\":\"2\"\r\n}"
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|title|query|string| 是 |视频标题|
|page|query|string| 是 |起始页|
|pageSize|query|string| 是 |每页的视频数量|
|token|header|string| 是 |none|
|body|body|object| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": {
    "records": [
      {
        "id": 4,
        "title": "游玩",
        "url": "https://www.bilibili.com/video/BV1Py5izFEYK/?spm_id_from=333.337.search-card.all.click&vd_source=409122dde074c21994778f85f11a9d6e"
      },
      {
        "id": 2,
        "title": "游玩",
        "url": "https://www.bilibili.com/video/BV18bmVY7EMy/?spm_id_from=333.337.search-card.all.click"
      }
    ],
    "total": 3
  },
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|object|true|none||数据|
|»» records|[object]|true|none||none|
|»»» id|integer|true|none||视频ID|
|»»» title|string|true|none||视频标题|
|»»» url|string|true|none||视频路径|
|»» total|integer|true|none||记录数|
|» msg|string|true|none||提示信息|

# Comment相关接口

## POST 发表评论

POST /comment/add

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|videoId|query|integer(int64)| 是 |视频id|
|content|query|string| 是 |评论内容|
|token|header|string| 是 |token|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "发表成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据|
|» msg|string|true|none||提示信息|

## GET 分页查看评论（含点赞数）

GET /comment/list

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|videoId|query|integer(int64)| 是 |评论id|
|page|query|string| 是 |起始页|
|pageSize|query|string| 是 |每页评论数|
|token|header|string| 是 |token|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": {
    "records": [
      {
        "content": "你好",
        "createTime": "2026-04-18 11:55:29",
        "likesCount": 0
      },
      {
        "content": "你好呀",
        "createTime": "2026-04-18 11:53:42",
        "likesCount": 0
      },
      {
        "content": "好玩好玩",
        "createTime": "2026-04-18 11:49:47",
        "likesCount": 0
      }
    ],
    "total": 3
  },
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|object|true|none||数据|
|»» records|[object]|true|none||none|
|»»» content|string|true|none||评论内容|
|»»» createTime|string|true|none||评论时间|
|»»» likesCount|integer|true|none||点赞数|
|»» total|integer|true|none||总计评论条数|
|» msg|string|true|none||提示信息|

## DELETE 删除评论

DELETE /comment/delete

> Body 请求参数

```xml
<?xml version="1.0" encoding="UTF-8" ?>
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|commentId|query|integer| 是 |评论id|
|token|header|string| 是 |none|
|body|body|object| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "删除成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» data|string|true|none||none|
|» msg|string|true|none||none|

## POST 更改点赞

POST /comment/update

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|commentId|query|string| 是 |评论id|
|token|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "操作成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据|
|» msg|string|true|none||提示信息|

# 关注相关接口

## GET 查询关注列表（含数量）

GET /follow/followings

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|userId|query|string| 是 |用户ID|
|page|query|string| 是 |起始页|
|pageSize|query|string| 是 |每页数量|
|token|header|string| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": {
    "records": [
      {
        "id": 5,
        "nickname": "香草",
        "username": "香草111"
      }
    ],
    "total": 1
  },
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|object|true|none||数据|
|»» records|[object]|true|none||none|
|»»» id|integer|false|none||用户id|
|»»» nickname|string|false|none||用户昵称|
|»»» username|string|false|none||用户名|
|»» total|integer|true|none||总数|
|» msg|string|true|none||提示信息|

## GET 查询粉丝列表（含数量）

GET /follow/followers

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|userId|query|string| 是 |用户ID|
|page|query|string| 是 |起始页|
|pageSize|query|string| 是 |每页数量|
|token|header|string| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": {
    "records": [
      {
        "id": 4,
        "nickname": "香草",
        "username": "香草"
      }
    ],
    "total": 1
  },
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|object|true|none||数据|
|»» records|[object]|true|none||none|
|»»» id|integer|false|none||用户id|
|»»» nickname|string|false|none||用户昵称|
|»»» username|string|false|none||用户名|
|»» total|integer|true|none||总数|
|» msg|string|true|none||提示信息|

## GET 查询互粉列表（含数量）

GET /follow/friends

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|userId|query|string| 是 |用户ID|
|page|query|string| 是 |起始页|
|pageSize|query|string| 是 |每页数量|
|token|header|string| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": {
    "records": [
      {
        "id": 4,
        "nickname": "香草",
        "username": "香草"
      }
    ],
    "total": 1
  },
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|object|true|none||数据|
|»» records|[object]|true|none||none|
|»»» id|integer|false|none||用户ID|
|»»» nickname|string|false|none||用户昵称|
|»»» username|string|false|none||用户名|
|»» total|integer|true|none||none|
|» msg|string|true|none||提示信息|

## POST 更改关注

POST /follow/changeFollow

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|FollowingId|query|string| 是 |要关注的id|
|token|header|string| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "修改关注成功",
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|string|true|none||数据|
|» msg|string|true|none||提示信息|

## GET 查询是否关注

GET /follow/isFollow

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|followingId|query|string| 是 |我关注的人的id|
|token|header|string| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": false,
  "msg": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||编码|
|» data|boolean|true|none||数据（是否关注）|
|» msg|string|true|none||提示信息|

