# TVA · 人生星轨 后端服务（tva-server）

对应文档：《TVA-人生星轨-PRD-V2.0》（V2.2 内容）、《TVA-技术设计文档-V1.0》。

**本期范围**：环境搭建 + 三张表 DDL + 实体类/Mapper + 登录鉴权模块（注册 / 邮箱验证码 / 登录 / 刷新 / 登出 / 当前用户）+ 事件与时间节点模块（事件 CRUD、节点 CRUD）。

## 技术栈

Java 17 · Spring Boot 3.2.5 · MyBatis-Plus 3.5.6 · MySQL 8 · Redis · JWT（jjwt 0.12.3）· Knife4j 4.4.0 · spring-boot-starter-mail

## Roadmap

状态说明：**已完成**表示当前仓库已有对应后端实现；**进行中**表示仅完成部分基础工作；**未开始**表示当前仓库尚未实现。前端、AI、视频与社区功能不属于当前后端仓库的已交付范围。

### 当前完成情况

- [x] **项目基础设施**：Spring Boot 工程、统一响应与异常处理、MyBatis-Plus、Knife4j、异步任务配置。
- [x] **数据库基础**：用户、事件、时间节点三张表 DDL，以及对应 Entity / Mapper。
- [x] **F1 用户系统后端（P0）**：邮箱验证码、注册、登录、双 Token 刷新、登出、当前用户信息。
- [x] **基础安全能力**：BCrypt 密码哈希、接口鉴权、资源归属校验基础、Redis 黑名单与 Refresh Token 吊销。
- [x] **F2 私人时间线后端（P0）**：事件创建（内嵌节点列表、同事务批量写入）、分页列表、详情、修改、逻辑删除（级联节点）；节点添加、有序查询、修改、删除；全链路资源归属校验。
- [x] **单元测试（2026-11-24）**：登录模块业务分支、JWT、全局异常处理、接口参数校验、Redis key 契约，共 5 个测试类 55 个用例。

### 功能优先级与进度

| PRD 模块 | 优先级 | 当前状态 | 已完成 / 待完成内容 |
|---|---|---|---|
| F1 用户系统 | P0 | **后端已完成** | 邮箱验证码注册、登录、刷新、登出、当前用户接口已完成 |
| F2 私人时间线 | P0 | **后端已完成** | 事件与节点 CRUD、事务及归属校验已完成；同刻唯一（10001）、最后节点删除保护（10003）等业务约束待启用 |
| F3 星轨长卷视图 | P0（本期范围） | **未开始** | 四主线渲染、规则曲线、横拖缩放、节点悬停与点击等前端能力待实现 |
| F4 AI 拟合曲线 | P1 | **未开始** | 规则曲线兜底可随 F3 先行；AI 接口、结构化输出、超时降级待实现 |
| F5 人生历程视频生成 | P1 | **未开始** | 本期不做，后续确定播放、录制或后端合成方案 |
| F6 互鉴浏览 | P1 | **未开始** | 本期不做，待补充入场门槛、可见性、内容审核及浏览接口 |
| F7 综合时间线交叉 / 多人推荐 | P2 | **未开始** | Roadmap 后续能力，暂不计入近期交付 |

> 优先级口径：PRD 功能列表曾将 F3 标为 P1，但 F3 详述及本期范围将其列为 P0。本 Roadmap 以“本期纳入 F3”为准；建议下次评审统一 PRD 中的标注。

### 近期迭代顺序

#### M1 · 完成私人时间线后端闭环（P0）

- [x] 事件创建（内嵌节点列表，至少一个，同事务批量写入）、分页列表、详情、修改与逻辑删除。
- [x] 时间节点添加、有序查询、修改与逻辑删除。
- [ ] 补齐同刻唯一、最后节点删除保护等业务约束及对应业务错误码。
- [ ] 完成事件/节点模块接口测试，并保持 Knife4j 文档同步。

#### M2 · 完成星轨长卷基础体验（P0 + 规则曲线兜底）

- [ ] 提供前端所需的完整时间线数据查询能力，避免大量逐事件请求。
- [ ] 前端接入真实事件与节点数据，实现四主线、光点、规则曲线与时间刻度。
- [ ] 实现横向拖动、缩放、节点详情、主线筛选，以及空状态、加载失败和 WebGL 降级。
- [ ] 明确出生日期来源、主线连接规则与“交汇星球”的数据判定规则。

#### M3 · AI 增强与回顾能力（P1）

- [ ] 基于 LangChain4j 接入 AI 拟合，返回分段贝塞尔控制点 JSON。
- [ ] AI 超时或失败时自动降级为规则曲线，不阻断时间线浏览。
- [ ] 设计并实现人生历程播放；视频文件生成另做技术评审。

#### M4 · 社区与多人互鉴（P1 / P2）

- [ ] 实现“先提交后浏览”、事件级可见性与内容审核机制。
- [ ] 实现他人时间线浏览，再逐步扩展多人交叉与推荐。
- [ ] 补充隐私保护、浏览统计、举报与运营治理能力。

## 单元测试

```bash
mvn test        # JDK 17
```

| 测试类 | 用例数 | 覆盖内容 |
|---|---|---|
| `serviceimpl.AuthServiceImplTest` | 24 | 发码（冷却 / 成功）、注册（成功 / 验证码过期 / 错码 / 连错 5 次作废 / 重名 / 重邮箱 / 并发唯一索引兜底）、登录、刷新（验签 / type / jti 重放 / 吊销）、登出、当前用户 |
| `com.tva.startrail.util.JwtUtilTest` | 8 | 签发解析往返、真实过期、篡改验签、access / refresh type 区分、remainingSeconds 边界 |
| `com.tva.startrail.common.GlobalExceptionHandlerTest` | 8 | BizException 透传、参数绑定、缺参、唯一索引冲突、404、500 兜底 |
| `com.tva.startrail.controller.AuthControllerTest` | 10 | MockMvc 接口参数校验（缺字段 / 邮箱格式 / 验证码 6 位）与各端点正常路径 |
| `com.tva.startrail.util.RedisKeysTest` | 5 | 五类 Redis key 拼接契约 |

共计 55 个用例（2026-11-24 全量通过）。未覆盖：`AuthInterceptor` 黑名单命中 / Redis 降级分支、事件/节点模块（待补充）。

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

## 已实现接口（/api/events、/api/nodes）

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | /api/events | 创建事件（内嵌节点列表，至少 1 个、最多 50 个，同事务批量写入） | 是 |
| GET  | /api/events | 我的事件分页列表（可按主线筛选，含节点数与起止时间聚合） | 是 |
| GET  | /api/events/{id} | 事件详情（节点按 happen_time ASC, id ASC 全量返回） | 是 |
| PUT  | /api/events/{id} | 修改事件（字段均选填） | 是 |
| DELETE | /api/events/{id} | 逻辑删除事件，级联逻辑删除全部节点 | 是 |
| POST | /api/events/{eventId}/nodes | 添加节点 | 是 |
| GET  | /api/events/{eventId}/nodes | 节点列表（happen_time ASC, id ASC） | 是 |
| PUT  | /api/nodes/{id} | 修改节点（字段均选填） | 是 |
| DELETE | /api/nodes/{id} | 删除节点 | 是 |

归属链：节点 → 所属事件 → 当前用户，越权访问一律 404（不暴露他人资源存在性）。

鉴权方式：请求头 `Authorization: Bearer <accessToken>`；Access 30 分钟 / Refresh 7 天；
登出黑名单与 Refresh jti 存 Redis，Redis 故障时黑名单检查降级跳过（30 分钟短有效期兜底）。

## 目录结构

```
src/main/java/com/tva/startrail
├── common/      Result、BizException、全局异常处理、UserContext
├── config/      MyBatis-Plus（分页+字段填充）、AuthInterceptor、WebConfig、Async、Knife4j
├── controller/  AuthController、EventController、NodeController
├── service/     AuthService、UserService、MailService、EventService、NodeService（+ impl）
├── mapper/      UserMapper、EventsInfoMapper、TimelineNodeMapper
├── entity/      BaseEntity、User、EventsInfo、TimelineNode
├── dto/         RegisterDTO、EmailCodeDTO、LoginDTO、RefreshDTO、EventCreateDTO（内嵌 NodeItemDTO 列表）、EventQueryDTO、EventUpdateDTO、NodeCreateDTO、NodeUpdateDTO
├── vo/          UserVO、LoginVO、RefreshVO、EventDetailVO、EventListItemVO、NodeVO、NodeAggVO、PageVO
└── util/        JwtUtil、RedisKeys
```

## 后续迭代

- 业务约束（同刻唯一 10001 / 节点数下限 10003 等）随 P1 迭代启用，接口契约不变
- F3 星轨长卷前端、F4 AI 拟合曲线，见上方 Roadmap
