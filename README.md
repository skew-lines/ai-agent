# ai-agent

基于 Spring Boot 3、Spring AI、DashScope、PostgreSQL/pgvector 的 AI Agent 项目。

当前项目已经实现了：

- 普通同步聊天
- 流式聊天
- 基于 pgvector 的 RAG 问答
- Tool Calling 工具调用
- MCP Client 集成
- Agent 运行过程持久化
- 聊天记忆持久化
- Swagger / Knife4j 接口文档

## 技术栈

- Java 21
- Spring Boot 3.5.12
- Spring AI 1.0.0
- Spring AI Alibaba DashScope
- PostgreSQL
- pgvector
- Knife4j OpenAPI 3
- Hutool
- Jsoup
- iText PDF

## 项目结构

```text
src/main/java/com/yl
├─ advisor        自定义 Advisor
├─ agent          Agent 抽象与实现
├─ app            业务应用层，封装聊天入口
├─ chatmemory     聊天记忆实现
├─ config         Web 配置
├─ controller     HTTP 接口
├─ demo           独立调用示例
├─ persistence    会话、步骤、产物持久化
├─ rag            RAG 文档加载、索引、检索配置
└─ tools          本地工具注册与实现

src/main/resources
├─ application.yaml
├─ application-local.yaml
├─ mcp-servers.json
└─ document       RAG 知识库 Markdown 文档
```

## 核心能力

### 1. 聊天能力

项目中的聊天入口由 `CodingApp` 统一封装，支持：

- 普通同步聊天
- 流式聊天
- RAG 增强聊天
- 本地工具调用聊天
- MCP 工具调用聊天

聊天记忆通过 `PgChatMemory` 落库到 PostgreSQL，不依赖进程内存，服务重启后仍可继续基于 `chatId` 读取上下文。

### 2. RAG 能力

RAG 的实现链路如下：

1. 从 `src/main/resources/document/*.md` 加载知识文档
2. 将 Markdown 解析为 Spring AI `Document`
3. 为文档追加 `filename`、`status` 等 metadata
4. 调用 Embedding 模型向量化
5. 写入 PostgreSQL 的 `public.vector_store`
6. 查询时先做 Query Rewrite
7. 再通过 `QuestionAnswerAdvisor` + `pgVectorVectorStore` 做检索增强

默认知识文档目录：

- `src/main/resources/document/编程AI助手_java.md`
- `src/main/resources/document/编程AI助手_mysql.md`

### 3. Agent 能力

项目中包含一套简化的 Agent 执行框架：

- `BaseAgent`
- `ReActAgent`
- `ToolCallAgent`
- `OpenManus`

支持将工具调用过程、执行步骤、工具产物等信息持久化到数据库。

### 4. 工具能力

当前已注册的本地工具包括：

- 文件读写工具
- Web 搜索工具
- 网页抓取工具
- 资源下载工具
- 终端命令工具
- PDF 生成工具
- 终止工具

工具统一在 `ToolRegistration` 中注册为 `ToolCallback[]`。

### 5. MCP 能力

项目通过 `spring-ai-starter-mcp-client` 集成 MCP Client。

当前 `mcp-servers.json` 中配置了：

- 高德地图 MCP
- `image-search-mcp` 子项目

## 配置说明

项目默认启用 `local` profile。

`application.yaml` 中的配置项通过占位符读取，实际默认值在 `application-local.yaml` 中。

当前代码依赖以下关键配置：

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://host:port/db
    username: xxx
    password: xxx
```

### DashScope 配置

```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key
      chat:
        options:
          model: qwen-plus
```

### pgvector 配置

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        dimensions: 1536
        distance-type: COSINE_DISTANCE
        max-document-batch-size: 10000
```

### 服务配置

根据当前 `application-local.yaml`，默认启动信息为：

- 端口：`8123`
- 上下文路径：`/api`

因此接口实际访问前缀为：

```text
http://localhost:8123/api
```

## 环境准备

启动前至少需要准备以下依赖：

1. Java 21
2. Maven 3.9+
3. PostgreSQL
4. pgvector 扩展
5. DashScope API Key

## 数据库要求

项目代码中显式依赖了以下几类数据表：

- `vector_store`
- `chat_session`
- `chat_message`
- `agent_task`
- `agent_step`
- `tool_artifact`

其中：

- `vector_store` 用于 RAG 向量检索
- `chat_session` 和 `chat_message` 用于聊天记忆
- `agent_task`、`agent_step`、`tool_artifact` 用于 Agent 执行过程持久化

注意：

- `PgVectorVectorStoreConfig` 中设置了 `initializeSchema(false)`
- 这意味着 `vector_store` 表不会自动建表，需要提前准备

结合代码，`vector_store` 至少需要满足类似结构：

```sql
CREATE TABLE vector_store (
    id UUID PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VECTOR(1536)
);
```

## 启动方式

### 1. 开发模式启动

```bash
mvn spring-boot:run
```

### 2. 打包后启动

```bash
mvn clean package -DskipTests
java -jar target/ai-agent-0.0.1-SNAPSHOT.jar
```

### 3. Windows 示例

```powershell
mvn clean package -DskipTests
java -jar target\ai-agent-0.0.1-SNAPSHOT.jar
```

## RAG 知识库构建

项目提供了一个离线入口类 `KnowledgeBaseIndexer`，用于把 Markdown 文档重新写入 pgvector。

执行逻辑：

- 加载 `classpath:document/*.md`
- 解析为 `Document`
- 分批做 Embedding
- 清空 `public.vector_store`
- 重新写入向量库

你可以用 IDE 直接运行：

```text
com.yl.rag.KnowledgeBaseIndexer
```

也可以自行封装成命令行启动方式。

注意：

- 当前实现是全量重建
- 重建前会执行 `delete from public.vector_store`
- DashScope embedding 单批最多支持 25 条文本，代码中已按 25 条分批写入

## 主要接口

### 健康检查

```http
GET /health
```

完整地址：

```text
http://localhost:8123/api/health
```

### 同步聊天

```http
GET /ai/love_app/chat/sync?message=你好&chatId=test-001
```

### RAG 聊天

```http
GET /ai/love_app/chat/rag?message=Java里HashMap和ConcurrentHashMap的区别是什么&chatId=test-rag-001
```

### 流式聊天

```http
GET /ai/love_app/chat/sse?message=你好&chatId=test-sse-001
```

### SSE Event 聊天

```http
GET /ai/love_app/chat/server_sent_event?message=你好&chatId=test-sse-002
```

### SseEmitter 聊天

```http
GET /ai/love_app/chat/sse_emitter?message=你好&chatId=test-sse-003
```

### Agent 聊天

```http
GET /ai/manus/chat?message=帮我搜索一下Spring AI最新能力&chatId=manus-001
```

## Swagger / Knife4j

项目已集成 Knife4j，启动后可直接访问：

```text
http://localhost:8123/api/swagger-ui.html
```

OpenAPI 文档地址：

```text
http://localhost:8123/api/v3/api-docs
```

## 关键实现说明

### 聊天记忆

- `PgChatMemory` 实现了 Spring AI 的 `ChatMemory`
- 会根据 `chatId` 从 `chat_session` / `chat_message` 中读写上下文
- 当前默认只读取最近 20 条消息

### RAG 查询

- `QueryRewriter` 会先对用户问题做重写
- `QuestionAnswerAdvisor` 基于 `pgVectorVectorStore` 做检索增强
- 当前使用的向量表是 `public.vector_store`

### 文档切分

`DocumentLoader` 使用 `MarkdownDocumentReader` 解析文档，配置上具备以下特点：

- 按 Markdown 水平分隔线拆分文档
- 保留代码块
- 不保留引用块
- 自动注入 `filename` 和 `status` metadata

### Agent 持久化

`AgentPersistenceService` 会保存：

- 任务主记录
- 每一步 thought / action / output
- 工具产物文件路径

## 开发建议

1. 不要把真实密钥直接提交到仓库中，建议改为环境变量或本地私有配置
2. 如果只测试普通聊天或 RAG，先确认 DashScope 网络可达
3. 如果只测试 RAG，先确认 `vector_store` 中已经有知识库数据
4. 如果 MCP 服务不可用，应用启动可能会被其初始化影响，建议先检查 `mcp-servers.json`

## 当前仓库中的已知注意点

根据当前代码和配置，使用时需要特别注意：

- `application-local.yaml` 中存在实际配置内容，建议改为本地私有文件或环境变量管理
- `vector_store` 不会自动建表
- RAG 功能依赖外部模型服务和数据库
- MCP 依赖外部子进程或外部服务可正常启动

## 测试示例

### curl 示例

```bash
curl "http://localhost:8123/api/ai/love_app/chat/sync?message=你好&chatId=test-001"
curl "http://localhost:8123/api/ai/love_app/chat/rag?message=Java里HashMap和ConcurrentHashMap的区别是什么&chatId=test-rag-001"
```

### 浏览器直接访问

```text
http://localhost:8123/api/ai/love_app/chat/sync?message=你好&chatId=test-001
http://localhost:8123/api/ai/love_app/chat/rag?message=Java里HashMap和ConcurrentHashMap的区别是什么&chatId=test-rag-001
```

## 后续待补充内容

