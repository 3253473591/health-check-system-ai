# 体检管理系统（含 RAG 辅助报告生成）

一套可完全本地化部署的体检管理系统。业务侧覆盖登记、数据采集、报告生成到发布查询的全流程；AI 侧通过 **AnythingLLM 本地知识库 + Ollama 本地模型** 为医生生成报告草稿，数据不出内网。

> 本科毕业设计作品，现已整理开源。
> **演示数据全部为虚构**，系统中不含任何真实个人信息。

## 为什么值得一看

市面上的 RAG demo 大多只演示"检索 → 生成"这条主干，一旦本地模型推理变慢就整条链路卡死。本项目在工程兜底上做了一些实际工作：

| 能力 | 说明 |
|---|---|
| **双引擎自动降级** | RAG 优先；30 秒内无 token 或连接异常，自动切换到纯本地模型继续生成，前端通过 SSE 事件实时告知用户 |
| **结果缓存** | 以 `exam_id + MD5(体检数据)` 为键缓存生成结果，默认 24 小时有效，同一份数据不重复消耗算力 |
| **流式资源管理** | `CompletableFuture` 线程隔离 + `AtomicBoolean` CAS 状态控制 + 空闲检测，防止僵尸 SSE 连接 |
| **幻觉约束** | Prompt 中显式禁止引用未上传的医学指南，并要求标注依据来源；输出强制纯文本以便后续正则解析 |
| **敏感字段加密** | 身份证号、手机号通过 MyBatis 自定义 typeHandler 做 AES-256-CBC 透明加解密 |

## 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Spring Boot 2.7.6 / Java 8 |
| 持久层 | MyBatis-Plus 3.5.5 + MySQL 8.0 |
| 安全 | Spring Security（BCrypt 口令校验）|
| 报表导入 | Apache POI 5.2.3 |
| 前端 | 原生 HTML5 / CSS3 / JavaScript（无构建步骤）|
| 本地 RAG | AnythingLLM（LanceDB 向量库）|
| 本地 LLM | Ollama + `deepseek-r1:8b-0528-qwen3-q8_0` |

## 架构

```
┌──────────────┐   SSE 流式   ┌─────────────────────────────────────┐
│  浏览器前端   │◄────────────►│  AiController                       │
│  (静态 HTML)  │              │    └─ AiServiceImpl                 │
└──────────────┘              │         ├─ AiPromptService  组装 Prompt
                              │         ├─ AiCacheService   指纹缓存
                              │         └─ AiGenerateService        │
                              └───────────────┬─────────────────────┘
                                              │
                        ┌─────────────────────┴─────────────────────┐
                        │ 优先：AnythingLLM     降级：Ollama         │
                        │ /api/v1/workspace/    /api/generate       │
                        │   {ws}/stream-chat                        │
                        │ 知识库检索 + 生成        纯本地模型生成      │
                        └───────────────────────────────────────────┘
```

## 快速开始

### 1. 初始化数据库

```bash
mysql -uroot -p < src/main/resources/db/schema.sql
```

脚本会创建库 `health_check_system`、10 张表，并写入演示数据（12 条体检记录，覆盖 PENDING / IN_PROGRESS / LOCKED 各状态）。

### 2. 配置环境变量

```bash
cp .env.example .env
```

按需修改 `DB_PASSWORD`、`RAG_API_KEY` 等。所有配置项在 `application.yml` 中都有对应占位符 `${VAR:默认值}`，不设环境变量也能以默认值启动。

### 3. 启动 AI 组件（可选）

AI 功能不影响主业务流程——不启动 Ollama / AnythingLLM，系统仍可作为常规体检管理系统正常使用，只是在点击"获取 AI 建议"时会收到服务不可用的提示。

```bash
# 本地模型
ollama pull deepseek-r1:8b-0528-qwen3-q8_0
ollama serve                       # http://localhost:11434

# 本地知识库
# 启动 AnythingLLM，上传医学指南 PDF 建立工作区，
# 工作区 slug 与 RAG_WORKSPACE 保持一致（默认 health-check）
# 在「设置 → 工具 → API Key」生成密钥填入 RAG_API_KEY
```

### 4. 运行

```bash
mvn spring-boot:run
```

访问 http://localhost:8080 ，用演示账号登录：

| 账号 | 密码 | 角色 |
|---|---|---|
| lqx | 123 | 管理员 |
| d1 | 123456 | 医生 |
| h1 | 123456789 | 企业 HR |

## 目录结构

```
src/main/java/com/lqx/health_check_system/
├── client/          AnythingLLM / Ollama 的 HTTP 流式客户端
├── common/          统一响应、异常处理、AES 透明加解密
├── config/          安全配置、角色拦截器、AI 配置绑定
├── controller/      REST 接口
├── dto/             传输对象
├── entity/          10 张表的实体
├── mapper/          MyBatis-Plus Mapper
└── service/         业务逻辑（impl 为具体实现）
src/main/resources/
├── db/schema.sql    建表 + 演示数据
├── mapper/*.xml     手写 SQL
└── static/          前端页面（原生 HTML/CSS/JS）
```

## 业务状态机

体检记录按 `PENDING → IN_PROGRESS → LOCKED → PUBLISHED` 流转：

- `PENDING` 已登记，等待检查
- `IN_PROGRESS` 数据录入中
- `LOCKED` 数据锁定，**不可再修改**，此时医生可撰写报告
- `PUBLISHED` 报告已发布，体检者可凭信息自助查询

数据录入接口在 `LOCKED` 与 `PUBLISHED` 状态下会拒绝写入，保证报告依据的数据不被篡改。

## 已知限制

这是一份毕业设计作品，达到"功能完整可用"，但**不适合直接用于生产**：

1. **认证强度不足**：`AuthServiceImpl` 签发的是 `mock-token-{userId}-{ts}-{role}` 明文串，拦截器直接按分隔符取出角色。上线前必须换成 JWT 或服务端会话。
2. **AES 密钥需更换**：`application.yml` 中的默认 key/iv 相同且为公开常量，仅用于开发。
3. **降级路径有缺陷**：`AiServiceImpl` 中 RAG 失败降级分支的 CAS 写法有误（预期值传成了 `false`），并发下状态管理不可靠。
4. **测试缺失**：仅有一个空的 Spring Boot 上下文测试。
5. **知识库依赖外部平台**：文档切分、向量化、检索参数均由 AnythingLLM 承担，Java 侧只负责 Prompt 组装与调用转发。

## 许可

MIT
