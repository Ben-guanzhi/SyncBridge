# 配置化数据同步平台

基于 Spring Boot 3 + MyBatis-Plus + Hutool + OkHttp + Aviator 表达式引擎开发。

## 技术栈

| 组件 | 版本/框架 |
|------|----------|
| Java | 17 |
| Spring Boot | 3.2.x |
| MyBatis-Plus | 3.5.6 |
| HikariCP | 5.x（连接池） |
| **SQLite** | **3.x（开发/轻量部署）** |
| MySQL | 8.x（生产环境可选） |
| Hutool | 5.x（工具集 + Db） |
| OkHttp | 4.x（HTTP 客户端） |
| Aviator | 5.x（表达式引擎） |
| Jackson | 2.17.x（JSON 处理） |
| Lombok | 简化 Getter/Setter |
| **前端** | **Vue 3 + Vite + Element Plus** |

## 快速启动

### 开发环境（SQLite 文件数据库 + 前端 Dev Server）

SQLite 数据库文件将自动创建在 `~/.syncbridge/sync_platform.db`

**后端:**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
```

**前端:**
```bash
cd frontend
npm install
npm run dev    # http://localhost:3000
```

访问 `http://localhost:3000`，Vite 会自动代理 `/api/*` 到后端 8080。

### 生产环境（SQLite 文件数据库 + 静态资源打包）

```bash
# 1. 构建前端静态资源
cd frontend && npm install && npm run build
mkdir -p ../src/main/resources/static
cp -r dist/* ../src/main/resources/static/

# 2. 修改 application.yml → 切换数据源（默认已配置 SQLite）

# 3. 启动
mvn package -DskipTests
java -jar target/db-sync-platform-1.0.0-SNAPSHOT.jar
```

数据库文件位于 `~/.syncbridge/sync_platform.db`

访问 `http://localhost:8080`

### 生产环境（可选：MySQL + 静态资源打包）

如需使用 MySQL 作为生产数据库：

```bash
# 1. MySQL 建表
mysql -u root -p db_sync_platform < docs/schema.sql
mysql -u root -p db_sync_platform < docs/init-demo-data.sql

# 2. 修改 application.yml → 指向真实 MySQL

# 3. 构建前端并启动（同上）
```

## 运行 Demo 示例

项目提供了 8 个同步场景 Demo，演示不同数据源组合的同步方式：

### 基础同步模式 Demo

| Demo | 场景 | 说明 |
|------|------|------|
| `DemoPurchaseOrderSync` | DB→API | 采购申请单同步到金蝶云星空 |
| `DemoSalesOrderDbToDbSync` | DB→DB | 销售订单在两个数据库间同步 |
| `DemoSupplierApiToDbSync` | API→DB | 从供应商API同步到本地数据库 |
| `DemoCustomerApiToApiSync` | API→API | CRM客户数据同步到ERP API |

### 参考项目集成 Demo（来自 D:\OLP\jindie_api 和 D:\OLP\olp_cloud\olp-module-yonyou）

| Demo | 场景 | 说明 |
|------|------|------|
| `DemoCloudDataSyncJob` | 热更新配置同步 | 模拟 CloudDataSyncJob 模式，支持热更新、增量游标、14个同步任务 |
| `DemoKingdeeMesBidirectionalSync` | 金蝶↔MES双向同步 | 8种业务单据 + 14种主数据 + 状态同步 + 统计报告 |
| `DemoKingdeeBillSyncStats` | 金蝶单据同步统计 | 从金蝶API拉取单据并统计同步结果 |
| `DemoFileDataSync` | Excel/SQL文件同步 | 支持Excel文件和SQL文件作为数据源，支持数据预览 |

**运行单个 Demo：**
```bash
# 基础同步模式
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoPurchaseOrderSync" -Dexec.classpathScope=test
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoSalesOrderDbToDbSync" -Dexec.classpathScope=test
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoSupplierApiToDbSync" -Dexec.classpathScope=test
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoCustomerApiToApiSync" -Dexec.classpathScope=test

# 参考项目集成 Demo
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoCloudDataSyncJob" -Dexec.classpathScope=test
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoKingdeeMesBidirectionalSync" -Dexec.classpathScope=test
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoKingdeeBillSyncStats" -Dexec.classpathScope=test
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoFileDataSync" -Dexec.classpathScope=test
```

**运行所有 Demo：**
```bash
.\run-demos.bat
```

## 项目结构

```
├── src/main/java/com/sync/
│   ├── SyncPlatformApplication.java      # Spring Boot 启动类
│   ├── common/GlobalExceptionHandler.java # 全局异常处理
│   ├── config/                           # 配置类
│   │   ├── MybatisPlusConfig.java        # MP 分页插件（SQLite）
│   │   ├── JsonTypeHandler.java          # JSON 列处理器
│   │   ├── CorsConfig.java               # CORS 跨域
│   │   └── WebMvcConfig.java             # 静态资源映射
│   ├── controller/                       # REST API
│   │   ├── DatasourceController.java     # 数据源 CRUD
│   │   ├── JobController.java            # 任务 CRUD + 执行
│   │   ├── LogController.java            # 日志查询
│   │   └── FrontendController.java       # SPA 路由回退
│   ├── engine/                           # 核心引擎
│   │   ├── SyncEngine.java               # READ→FILTER→MAP→WRITE 编排
│   │   ├── AviatorFilter.java            # Aviator 行过滤 + 转换
│   │   ├── FieldMapper.java              # 字段重命名 + 转换
│   │   ├── MasterDetailAssembler.java    # 主+明细树形展开
│   │   ├── ApiPaginationFetcher.java     # API 分页循环拉取
│   │   └── IncrementalPositionManager.java # 增量位点管理
│   ├── enums/                            # DataSourceType, SyncMode, WriteMode
│   ├── mapper/                           # MyBatis-Plus Mapper 接口
│   ├── model/                            # JPA/MP 实体
│   ├── reader/                           # DataReader 抽象 + DbReader/ApiReader
│   ├── service/                          # Service 接口 + 实现
│   └── writer/                           # DataWriter 抽象 + DbWriter/ApiWriter
├── src/test/java/com/sync/demo/
│   ├── DemoPurchaseOrderSync.java        # DB→API 采购申请单同步（金蝶云星空）
│   ├── DemoSalesOrderDbToDbSync.java     # DB→DB 销售订单同步
│   ├── DemoSupplierApiToDbSync.java      # API→DB 供应商数据同步
│   ├── DemoCustomerApiToApiSync.java     # API→API 客户数据同步
│   ├── DemoCloudDataSyncJob.java         # 热更新配置同步（来自 olp-module-yonyou）
│   ├── DemoKingdeeMesBidirectionalSync.java  # 金蝶↔MES双向同步（来自 jindie_api）
│   ├── DemoKingdeeBillSyncStats.java     # 金蝶单据同步统计
│   └── DemoFileDataSync.java             # Excel/SQL文件数据同步
├── frontend/src/                         # Vue 3 前端源码
│   ├── api/                              # Axios 请求模块
│   ├── components/Layout.vue             # 侧边栏 + 顶栏布局
│   ├── router/index.js                   # 路由
│   ├── stores/main.js                    # Pinia 状态管理
│   ├── utils/http.js                     # Axios 封装
│   └── views/                            # 页面
│       ├── DatasourceList.vue            # 数据源管理页
│       ├── JobList.vue                   # 同步任务列表页
│       ├── JobEdit.vue                   # 任务创建/编辑页（核心）
│       └── LogList.vue                   # 执行日志查询页
├── src/main/resources/
│   ├── application.yml                   # 生产配置（SQLite 默认）
│   ├── application-dev.yml               # 开发配置（SQLite 文件库）
│   ├── schema-sqlite.sql                 # SQLite 建表（开发/生产）
│   └── mapper/                           # MyBatis XML 映射
├── docs/
│   ├── schema-sqlite.sql                 # SQLite 建表脚本
│   ├── schema.sql                        # MySQL 建表脚本（可选）
│   └── init-demo-data.sql                # 示例数据
├── pom.xml                               # Maven 依赖
└── README.md
```

## 标准数据流

```
源数据 (DB/API)
  ↓
DataReader.readPage()           ← 分页拉取
  ↓
MasterDetailAssembler.assemble()← 主+明细展开（可选）
  ↓
AviatorFilter.filter()          ← Aviator 行过滤
  ↓
FieldMapper.map()               ← 字段映射 + 转换
  ↓
DataWriter.batchWrite()         ← 批量写入目标
  ↓
IncrementalPositionManager      ← 位点更新（空结果不推进）
```

## WebUI 控制台

### 页面一览

| 页面 | 功能 | 说明 |
|------|------|------|
| **数据源管理** | 增删改查 + 连接测试 | 区分 DB / API 类型，表单动态切换字段 |
| **同步任务** | 列表 + 运行一次 + 执行全部定时 | 统计卡片、模式标签、批处理 |
| **任务编辑** | 配置段 + 字段映射 + 主明细 | 所见即所得编排完整 Pipeline |
| **执行日志** | 批次号搜索 | 查看每段的读取/过滤/写入统计 |

### 截图说明

- **Layout 布局**：左侧固定导航（数据源/任务/日志），顶部面包屑 + 折叠按钮
- **任务编辑**：支持多个配置段，每个段包含源表、目标表、写入模式、主明细标记、嵌套路径
- **字段映射**：弹窗内联编辑 `source_field → target_field (transform)`，支持 Aviator 表达式
- **执行结果弹窗**：任务列表中点击"运行一次"弹出分段的详细执行数据

## 核心设计原则

1. **配置驱动**：零硬编码，所有同步逻辑通过 `sync_job_config` 配置
2. **接口隔离**：`DataReader` / `DataWriter` 抽象，按需扩展新读取/写入方式
3. **位点安全**：全量过滤时不推进 last_sync_value
4. **连接池自动回收**：`DataSourceManager` 管理生命周期
5. **前后端分离**：Vite 代理开发，Spring MVC 打包生产静态资源

## 后续扩展方向

1. **文件数据源支持** ✅ 已完成：支持 Excel (.xlsx/.xls) 和 SQL 文件作为数据源，支持数据预览
2. **WebHook 预处理**：在 FILTER→MAP 之间插入可配置的 HTTP 回调
3. **权限/Audit**：Spring Security + 操作审计表
4. **分布式调度**：XXL-JOB / Elastic-Job 替代本地 Cron
5. **导入导出**：CSV / Excel 格式的数据导出
6. **告警通知**：失败日志触发邮件 / 钉钉 / 企业微信
