---
title: 默认模块
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

* <a href="http://dev-cn.your-api-server.com">开发环境: http://dev-cn.your-api-server.com</a>



# Authentication

# User相关接口

## POST 注册

POST /user/register

> Body 请求参数

```json
{
  "username": "Vanilla_xi2",
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
  "data": "注册成功"
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
|» code|integer|true|none||响应码|
|» data|string|true|none||响应信息|

## POST 登录

POST /user/login

> Body 请求参数

```json
{
  "username": "Vanilla_xi",
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
  "data": "token"
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
|» code|integer|true|none||响应码|
|» data|string|true|none||token|

# Video相关接口

## GET 查看视频

GET /video/get

> Body 请求参数

```json
"{\r\n    videoId=1;\r\n}"
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|string| 是 |视频id|
|Authorization|header|string| 是 |token|
|body|body|object| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": {
    "createTime": "2026-04-12 21:27:54",
    "id": 1,
    "title": "动物",
    "url": "https://www.w3schools.com/html/mov_bbb.mp4",
    "userId": 1
  }
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
|» code|integer|true|none||响应码|
|» data|object|true|none||响应信息|
|»» createTime|string|true|none||视频发布时间|
|»» id|integer|true|none||视频id|
|»» title|string|true|none||视频标题|
|»» url|string|true|none||视频链接|
|»» userId|integer|true|none||博主id|

# Comment相关接口

## POST 发表评论

POST /comment/add

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|videoId|query|integer(int64)| 是 |视频id|
|content|query|string| 是 |评论内容|
|Authorization|header|string| 是 |token|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": "评论成功"
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
|» code|integer|true|none||响应码|
|» data|string|true|none||响应信息|

## GET 查看评论

GET /comment/list

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|videoId|query|integer(int64)| 是 |评论id|
|Authorization|header|string| 是 |token|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "data": [
    {
      "content": "哇哦",
      "createTime": "2026-04-14 16:55:47",
      "username": "Vanilla_xi"
    },
    {
      "content": "哇哦",
      "createTime": "2026-04-14 16:15:11",
      "username": "Vanilla_xi"
    },
    {
      "content": "这视频拍得真不错，点赞！",
      "createTime": "2026-04-12 22:22:42",
      "username": "test_user_1775983383561"
    }
  ]
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
|» code|integer|true|none||响应码|
|» data|[object]|true|none||响应信息|
|»» content|string|true|none||评论内容|
|»» createTime|string|true|none||评论时间|
|»» username|string|true|none||评论者|

