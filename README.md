# TVA · 人生星轨 后端服务（tva-server）

对应文档：《TVA-人生星轨-PRD-V2.0》（V2.2 内容）、《TVA-技术设计文档-V1.0》。

**本期范围**：环境搭建 + 三张表 DDL + 实体类/Mapper + 登录鉴权模块（注册 / 邮箱验证码 / 登录 / 刷新 / 登出 / 当前用户）。
事件与时间节点模块本期**不实现**（实体与 Mapper 已就位，接口后续迭代）。

## 技术栈

Java 17 · Spring Boot 3.2.5 · MyBatis-Plus 3.5.6 · MySQL 8 · Redis · JWT（jjwt 0.12.3）· Knife4j 4.4.0 · spring-boot-starter-mail

## 快速开始

```bash
# 1. 建库建表
mysql -u root -p < sql/schema.sql        # 库名 tva_startrail

# 2. 启动（JDK 17）
mvn spring-boot:run
```

启动后接口文档：`http://localhost:8080/doc.html`（Knife4j）

## 配置项（环境变量覆盖）

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DB` | localhost / 3306 / tva_startrail | 数据库地址 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | root / 空 | 数据库账号 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | localhost / 6379 / 空 | Redis |
| `MAIL_HOST` / `MAIL_PORT` | smtp.qq.com / 465 | SMTP 服务 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | 空 | **SMTP 授权码，非邮箱登录密码**；不配置时验证码打日志（开发模式） |
| `MAIL_FROM` | = MAIL_USERNAME | 发件人地址 |
| `JWT_SECRET` | 内置开发值 | 生产必须覆盖（≥32 字节） |

## 已实现接口（/api/auth）

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | /api/auth/email-code | 发送邮箱验证码（60s 冷却、5 分钟有效、连错 5 次作废） | 否 |
| POST | /api/auth/register | 注册（验证码 + 用户名/邮箱查重，成功即登录） | 否 |
| POST | /api/auth/login | 登录（签发双 Token） | 否 |
| POST | /api/auth/refresh | 刷新 Access Token | 否（凭 Refresh Token） |
| POST | /api/auth/logout | 登出（黑名单 + 吊销 Refresh Token） | 是 |
| GET  | /api/auth/me | 当前用户信息 | 是 |

鉴权方式：请求头 `Authorization: Bearer <accessToken>`；Access 30 分钟 / Refresh 7 天；
登出黑名单与 Refresh jti 存 Redis，Redis 故障时黑名单检查降级跳过（30 分钟短有效期兜底）。

## 目录结构

```
src/main/java/com/tva/startrail
├── common/      Result、BizException、全局异常处理、UserContext
├── config/      MyBatis-Plus（分页+字段填充）、AuthInterceptor、WebConfig、Async、Knife4j
├── controller/  AuthController
├── service/     AuthService、UserService、MailService（+ impl）
├── mapper/      UserMapper、EventsInfoMapper、TimelineNodeMapper
├── entity/      BaseEntity、User、EventsInfo、TimelineNode
├── dto/         RegisterDTO、EmailCodeDTO、LoginDTO、RefreshDTO
├── vo/          UserVO、LoginVO、RefreshVO
└── util/        JwtUtil、RedisKeys
```

## 后续迭代（契约已冻结，见技术文档第四章）

- 事件模块：POST/GET/PUT/DELETE `/api/events`（创建内嵌首个节点，同事务）
- 节点模块：`/api/events/{eventId}/nodes`、`/api/nodes/{id}`（排序 `happen_time ASC, id ASC`）
- 业务约束（同刻唯一 10001 / 节点数下限 10003 等）随 P1 迭代启用，接口契约不变
