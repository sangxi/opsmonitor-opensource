# OpsMonitor 运维监控系统 - 部署指南

> 基于 opsmonitor 开源版 v3.6.3 (Apache-2.0) 二次开发
> 目标环境：Linux + 1Panel（同时支持原生 Docker / 直接 JAR 部署）

---

## 一、部署前准备

### 1.1 服务器环境要求

| 组件 | 最低版本 | 推荐 | 备注 |
|------|---------|------|------|
| OS | CentOS 7+ / Ubuntu 18+ | CentOS 7.9 | 64位 |
| CPU/内存 | 2C/2G | 4C/4G | 监控目标多时建议 4G+ |
| Java | 1.8.0_202+ | JDK 8 | 仅 JAR 部署需要 |
| MySQL | 5.7+ | 8.0 | 也可用云数据库 RDS |
| Docker | 20.10+ | 24.0+ | 仅 Docker 部署需要 |
| Docker Compose | 2.0+ | 2.20+ | 仅 Docker 部署需要 |
| 磁盘 | 10G | 50G+ | 历史监控数据保留天数决定 |

### 1.2 端口规划

| 端口 | 用途 | 必须开放 |
|------|------|---------|
| 9999 | Server Web 端口 | 对外（浏览器访问） |
| 3306 | MySQL | 仅内网（Server 访问） |
| 9998 | WebSSH（可选） | 仅内网 |

### 1.3 项目目录结构

```
opsmonitor-opensource/
├── opsmonitor-server/              # Server 端源码 + jar
│   └── target/opsmonitor-server-release.jar
├── opsmonitor-agent/               # Agent 端源码 + jar
│   └── target/opsmonitor-agent-release.jar
├── sql/                         # 数据库脚本
│   ├── opsmonitor-MySQL.sql        # 全量初始化（全新部署用）
│   └── upgrade-v3.6.3-webhook.sql  # 增量升级（老环境升级用）
└── deploy/                      # 部署相关文件
    ├── .env                     # 环境变量配置（必改）
    ├── docker-compose.yml       # Docker Compose 编排
    ├── server/
    │   ├── Dockerfile           # Server 镜像构建
    │   └── application.yml      # Server 外部配置
    ├── agent/
    │   ├── Dockerfile           # Agent 镜像构建
    │   └── application.yml      # Agent 外部配置
    ├── start.sh                 # 一键启动脚本（JAR 部署用）
    ├── stop.sh                  # 停止脚本
    ├── init-db.sh               # 数据库初始化脚本
    ├── DEPLOY.md                # 本文档
    └── AGENT-INSTALL.md         # 被监控端 Agent 安装指南
```

---

## 二、方案 A：Docker Compose 部署（推荐，适配 1Panel）

### 2.1 上传项目到服务器

将整个 `opsmonitor-opensource` 目录上传到服务器（推荐路径 `/opt/opsmonitor`）：

```bash
# 在服务器上
mkdir -p /opt/opsmonitor
cd /opt/opsmonitor
# 用 scp/sftp/1Panel 文件管理上传 zip 后解压
unzip opsmonitor-opensource.zip
```

### 2.2 修改环境变量

编辑 `/opt/opsmonitor/deploy/.env`：

```ini
# ===== 数据库配置（必改）=====
DB_HOST=192.168.40.111          # MySQL 主机 IP
DB_PORT=3306
DB_NAME=opsmonitor                # 数据库名
DB_USER=jk                       # 数据库用户名
DB_PASSWORD=arjtHmZXPEc5sKpn     # 数据库密码

# ===== 通信 token（Server 与 Agent 必须一致）=====
OMTOKEN=opsmonitor

# ===== Server 端口（宿主机映射端口）=====
SERVER_PORT=9999

# ===== Agent 配置 =====
SERVER_URL=http://opsmonitor-server:9999  # Docker 内部通信，保持原值
BIND_IP=                                   # 留空自动获取
```

### 2.3 初始化数据库（首次部署必做）

```bash
cd /opt/opsmonitor/deploy

# 若服务器没装 mysql 客户端，先安装：
# CentOS/RHEL:  yum install -y mysql
# Ubuntu/Debian: apt-get install -y mysql-client

bash init-db.sh
```

脚本会自动：
- 读取 `.env` 中的数据库配置
- 创建 `.env` 中 `DB_NAME` 指定的数据库（默认 `opsmonitor`，utf8mb4 编码）
- 导入 `sql/opsmonitor-MySQL.sql` 全量表结构
- 导入 `sql/opsmonitor-help.sql`（使用教程表 + 种子数据，含全系统部署与文件清单）
- 输出表数量验证导入成功

### 2.4 打包 jar 文件

Docker 部署需要先打包好 jar 文件。在内存足够的机器上执行（建议 ≥ 4G 可用内存）：

```bash
cd /opt/opsmonitor/opsmonitor-server
mvn clean package -DskipTests
# 产物：target/opsmonitor-server-release.jar

cd /opt/opsmonitor/opsmonitor-agent
mvn clean package -DskipTests
# 产物：target/opsmonitor-agent-release.jar
```

**内存不足时**，限制 Maven 堆大小：

```bash
export MAVEN_OPTS="-Xms128m -Xmx1024m -XX:MaxMetaspaceSize=256m"
mvn clean package -DskipTests
```

### 2.5 构建镜像并启动

```bash
cd /opt/opsmonitor
docker-compose -f deploy/docker-compose.yml up -d --build
```

首次构建会下载 OpenJDK 8 基础镜像，约 200MB，需要几分钟。

### 2.6 验证部署

```bash
# 查看容器状态（两个容器都应为 Up）
docker ps | grep opsmonitor

# 查看 Server 启动日志（看到 "Started OpsMonitorServiceApplication" 即成功）
docker logs -f opsmonitor-server

# 测试 HTTP 访问
curl -I http://localhost:9999/opsmonitor/login/toLogin
```

浏览器访问：`http://服务器IP:9999/opsmonitor/login/toLogin`
- 账号：`admin`
- 密码：`111111`

### 2.7 在 1Panel 中管理

1Panel 默认能识别 Docker Compose 项目。打开 1Panel → 容器 → 编排，可以看到 `opsmonitor` 项目，支持：
- 启动 / 停止 / 重启
- 查看实时日志
- 进入容器终端

### 2.8 常用运维命令

```bash
cd /opt/opsmonitor

# 启动
docker-compose -f deploy/docker-compose.yml up -d

# 停止
docker-compose -f deploy/docker-compose.yml down

# 重启
docker-compose -f deploy/docker-compose.yml restart

# 查看日志
docker logs -f opsmonitor-server
docker logs -f opsmonitor-agent

# 升级代码后重新构建
docker-compose -f deploy/docker-compose.yml up -d --build
```

---

## 三、方案 B：直接 JAR 部署（无 Docker）

适合 1Panel 上有 Java 环境或不希望使用 Docker 的情况。

### 3.1 安装 Java 8

```bash
# CentOS / RHEL
yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel

# Ubuntu / Debian
apt-get install -y openjdk-8-jdk

# 验证
java -version
```

### 3.2 上传项目并初始化数据库

参考方案 A 的 2.1、2.2、2.3 步骤。

### 3.3 打包 jar

```bash
cd /opt/opsmonitor/opsmonitor-server
mvn clean package -DskipTests
cd /opt/opsmonitor/opsmonitor-agent
mvn clean package -DskipTests
```

### 3.4 修改 Server 配置

编辑 `/opt/opsmonitor/deploy/server/config/application.yml`，确认数据库连接信息与 `.env` 一致。也可通过环境变量传入：

```bash
export DB_HOST=192.168.40.111
export DB_PORT=3306
export DB_NAME=opsmonitor
export DB_USER=jk
export DB_PASSWORD=arjtHmZXPEc5sKpn
export OMTOKEN=opsmonitor
```

### 3.5 一键启动

```bash
cd /opt/opsmonitor/deploy
bash start.sh
```

脚本会自动：
- 检查 Java 环境
- 启动 Server（端口 9999，-Xms256m -Xmx512m）
- 启动 Agent（如果存在 jar）
- 输出访问地址和 PID 文件位置

### 3.6 停止服务

```bash
bash stop.sh
```

### 3.7 配置 systemd 开机自启（生产环境推荐）

创建 `/etc/systemd/system/opsmonitor-server.service`：

```ini
[Unit]
Description=OpsMonitor Server
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/opsmonitor
Environment="DB_HOST=192.168.40.111"
Environment="DB_PORT=3306"
Environment="DB_NAME=opsmonitor"
Environment="DB_USER=jk"
Environment="DB_PASSWORD=arjtHmZXPEc5sKpn"
Environment="OMTOKEN=opsmonitor"
ExecStart=/usr/bin/java -Xms256m -Xmx512m -Dfile.encoding=UTF-8 -jar /opt/opsmonitor/opsmonitor-server/target/opsmonitor-server-release.jar --spring.config.location=/opt/opsmonitor/deploy/server/config/application.yml
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用并启动：

```bash
systemctl daemon-reload
systemctl enable opsmonitor-server
systemctl start opsmonitor-server
systemctl status opsmonitor-server
```

---

## 四、数据库说明

### 4.1 两个 SQL 文件的区别

| 文件 | 用途 | 适用场景 |
|------|------|---------|
| `sql/opsmonitor-MySQL.sql` | 全量初始化（DROP + CREATE 所有表） | **全新部署**，从零搭建 |
| `sql/upgrade-v3.6.3-webhook.sql` | 增量升级（ALTER TABLE 加新字段） | **老版本升级**到 v3.6.3 |
| `sql/opsmonitor-m5-upgrade.sql` | M5 功能增强增量（新增 ALARM_RECORD 告警记录 / AUDIT_LOG 操作审计 / OPEN_API_TOKEN 开放 API 令牌三张表，含默认演示令牌） | **老库升级到 M5 版** |
| `sql/opsmonitor-enhance-upgrade.sql` | M6 功能增强增量（SYSTEM_INFO 增加 DISK_PER / GROUP_NAME / TAGS 列 + 新增 REPORT_LOG 报表记录表 + 报表配置项，幂等加列） | **老库升级到 M6 版（主机分组标签 / 定时报表）** |

**首次部署只需要执行 `opsmonitor-MySQL.sql`，无需再执行升级脚本。**
**已在 M5 之前部署过的老库，升级时需额外执行一次 `sql/opsmonitor-m5-upgrade.sql`**（幂等，IF NOT EXISTS 保护，可重复执行），否则告警中心 / 操作审计 / 开放 API 页面会因缺表报错。
**已在 M5 之后部署过的老库，升级时需额外执行一次 `sql/opsmonitor-enhance-upgrade.sql`**（幂等加列，可重复执行），否则主机分组 / 标签列与报表记录表缺失，分组标签编辑与定时报表功能不可用。

`init-db.sh` 默认执行的就是 `opsmonitor-MySQL.sql`。

### 4.2 数据库字符集要求

数据库必须为 utf8mb4（推荐）或 utf8。`init-db.sh` 会自动用 utf8mb4 创建。

### 4.3 MySQL 用户授权

```sql
-- 在 MySQL 服务器上执行
CREATE USER 'jk'@'%' IDENTIFIED BY 'arjtHmZXPEc5sKpn';
GRANT ALL PRIVILEGES ON opsmonitor.* TO 'jk'@'%';
FLUSH PRIVILEGES;
```

---

## 五、被监控端 Agent 安装

详细步骤见 [AGENT-INSTALL.md](./AGENT-INSTALL.md)，覆盖 Linux / Windows / macOS。

### 5.1 Linux 快速安装

```bash
# 1. 上传 opsmonitor-agent-release.jar 和 application.yml 到被监控机
mkdir -p /opt/opsmonitor-agent
cp opsmonitor-agent-release.jar /opt/opsmonitor-agent/
cp application.yml /opt/opsmonitor-agent/

# 2. 修改 application.yml 中的 serverUrl 和 omToken
vi /opt/opsmonitor-agent/application.yml
# serverUrl: http://OpsMonitor服务器IP:9999
# omToken: opsmonitor  （必须与 Server 端 .env 中的 OMTOKEN 一致）

# 3. 启动
cd /opt/opsmonitor-agent
nohup java -Xms64m -Xmx128m -Dfile.encoding=UTF-8 \
    -jar opsmonitor-agent-release.jar \
    --spring.config.location=./application.yml \
    > agent.out 2>&1 &
```

### 5.2 Agent systemd 自启模板

创建 `/etc/systemd/system/opsmonitor-agent.service`：

```ini
[Unit]
Description=OpsMonitor Agent
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/opsmonitor-agent
ExecStart=/usr/bin/java -Xms64m -Xmx128m -Dfile.encoding=UTF-8 -jar /opt/opsmonitor-agent/opsmonitor-agent-release.jar --spring.config.location=/opt/opsmonitor-agent/application.yml
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

---

## 六、登录与默认配置

### 6.1 默认账号

| 项 | 值 |
|---|---|
| 账号 | `admin` |
| 密码 | `111111` |
| 登录地址 | `http://服务器IP:9999/opsmonitor/login/toLogin` |

**生产环境部署后请立即在系统配置页修改密码。**

### 6.2 系统配置

登录后进入「系统配置」页面，可自定义：
- 系统名称（显示在登录页和浏览器标题）
- 系统简称
- 版权信息
- CPU / 内存 / 磁盘告警阈值（%）

### 6.3 告警配置

进入「邮件告警配置」页面，可配置：
- 邮件告警（SMTP）
- 钉钉机器人 Webhook 告警（带加签验证）
- 企业微信群机器人 Webhook 告警

---

### 6.4 敏感配置管理（源码仓库）

源码 `opsmonitor-server/src/main/resources/application.yml` 中的敏感项均已改为环境变量占位符，真实凭据不进 git：

| 配置项 | 环境变量 | 默认值（仅本地开发用） |
|--------|----------|----------------------|
| 数据库连接 | `OPSMONITOR_DB_URL` | 本机 3306 / opsmonitor 库 |
| 数据库用户名 | `OPSMONITOR_DB_USER` | opsmonitor |
| 数据库密码 | `OPSMONITOR_DB_PWD` | 空 |
| 管理员密码 | `OPSMONITOR_ADMIN_PWD` | 111111（务必覆盖） |
| Agent 通信 token | `OPSMONITOR_OM_TOKEN` | opsmonitor（建议覆盖为随机长串） |

本地开发时，将真实值写入 `opsmonitor-server/src/main/resources/application-local.yml`（已被 `.gitignore` 忽略），并以 `--spring.profiles.active=local` 启动。该文件不会被打包进 git 提交，但若存在会在构建时打进 jar，请勿将本地 jar 直接分发给外部。

---

## 七、验证码功能说明

本次 v3.6.3 已完成验证码安全加固：

### 7.1 安全特性

| 特性 | 说明 |
|------|------|
| SecureRandom | 替代弱随机源 Random，防止随机数预测 |
| 字符集精简 | 去除易混淆字符 0/O、1/I/L，避免人工识别错误 |
| 一次性使用 | 验证码校验后立即从 session 清除，防止重放攻击 |
| 5 分钟有效期 | 超时自动失效，需重新刷新 |
| 登录失败自动刷新 | 任何登录失败场景都会触发图片自动刷新 |
| 字符旋转 + 噪点 | ±25° 旋转 + 60 噪点，兼顾可读性与防 OCR 识别 |

### 7.2 体验优化

- 图片尺寸 120×40，便于阅读
- 字体使用 SansSerif 逻辑字体，Linux/Windows 跨平台一致显示
- "看不清？换一张"提示，点击图片或提示均可刷新
- 登录失败后输入框自动清空，焦点回到验证码框

### 7.3 关键文件

| 文件 | 修改要点 |
|------|---------|
| `CodeController.java` | 验证码生成（SecureRandom + 字符集 + 时间戳） |
| `LoginController.java` | 校验逻辑（一次性消费 + 有效期 + 失败标志） |
| `AuthRestFilter.java` | 白名单放行 `/code/generate`，否则未登录加载图片会被重定向 |
| `login.html` | 前端刷新逻辑（context-path 动态解析 + 自动刷新） |
| `StaticKeys.java` | 新增验证码时间戳 session key 和有效期常量 |

---

## 八、常见问题

### Q1：mvn 打包报 `Could not reserve enough space for object heap`

内存不足。解决：

```bash
export MAVEN_OPTS="-Xms128m -Xmx1024m -XX:MaxMetaspaceSize=256m"
mvn clean package -DskipTests
```

Windows PowerShell：
```powershell
$env:MAVEN_OPTS = "-Xms128m -Xmx1024m -XX:MaxMetaspaceSize=256m"
mvn clean package -DskipTests
```

### Q2：MySQL 连接失败 `Unknown column 'SEND_WEBHOOK'`

数据库表结构未升级。执行升级脚本：

```bash
cd /opt/opsmonitor
mysql -h192.168.40.111 -P3306 -ujk -p opsmonitor < sql/upgrade-v3.6.3-webhook.sql
```

### Q3：访问 `http://IP:9999/opsmonitor/login/toLogin` 打不开

按顺序检查：

1. **Server 是否启动**：`docker ps | grep opsmonitor` 或 `ps -ef | grep opsmonitor`
2. **端口监听**：`netstat -tlnp | grep 9999`
3. **防火墙**：
   ```bash
   firewall-cmd --add-port=9999/tcp --permanent
   firewall-cmd --reload
   ```
4. **1Panel 防火墙**：1Panel → 主机 → 防火墙，放行 9999
5. **云服务器安全组**：在云控制台安全组规则中放行 9999

### Q4：验证码图片不显示

打开浏览器 F12 控制台看 `/code/generate` 请求：
- 返回 200 + `content-type: image/jpeg`：正常，刷新页面
- 返回 200 + `text/html`：被重定向到登录页，检查 `AuthRestFilter.java` 白名单是否包含 `/code/generate`
- 返回 404：URL 路径错误，检查 context-path 是否为 `/opsmonitor`

### Q5：Docker Agent 监控不到宿主机数据

Docker 容器内只能监控容器自身。要监控宿主机，必须在宿主机直接跑 Agent（见第五章）。

### Q6：登录提示"验证码已失效，请刷新后重试"

正常行为。验证码一次性消费，每次提交后都会刷新。点击"看不清？换一张"或图片本身即可刷新。

### Q7：登录失败次数过多被锁定

连续 5 次密码错误会锁定 30 分钟。等待 30 分钟自动解锁，或重启 Server 进程清除内存锁定记录（生产环境不推荐重启）。

### Q8：日志路径

- Docker 部署：`/opt/opsmonitor/deploy/server/logs/`
- JAR 部署：`/opt/opsmonitor/deploy/logs/`
- 容器内查看：`docker logs opsmonitor-server`

---

## 九、运维与备份

### 9.1 数据库备份

```bash
# 全量备份
mysqldump -h192.168.40.111 -ujk -p opsmonitor > opsmonitor_$(date +%Y%m%d).sql

# 自动备份（crontab）
echo "0 3 * * * mysqldump -h192.168.40.111 -ujk -parjtHmZXPEc5sKpn opsmonitor > /backup/opsmonitor_$(date +\%Y\%m\%d).sql" | crontab -
```

### 9.2 升级流程

```bash
# 1. 备份数据库
mysqldump -h... -ujk -p opsmonitor > backup.sql

# 2. 停止服务
cd /opt/opsmonitor
docker-compose -f deploy/docker-compose.yml down
# 或 bash deploy/stop.sh

# 3. 替换代码（保留 deploy/.env 和 sql/）
# 上传新的 opsmonitor-opensource 覆盖

# 4. 重新打包
cd opsmonitor-server && mvn clean package -DskipTests
cd ../opsmonitor-agent && mvn clean package -DskipTests

# 5. 如有 SQL 升级脚本，执行
mysql -h... -ujk -p opsmonitor < sql/upgrade-vX.X.X.sql

# 6. 启动
cd /opt/opsmonitor
docker-compose -f deploy/docker-compose.yml up -d --build
```

### 9.3 性能调优

修改 `deploy/server/config/application.yml`：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30  # 监控目标多时调大
server:
  tomcat:
    max-threads: 200  # 并发访问多时调大
```

JVM 参数调整：
- 小型部署（≤ 50 台监控）：`-Xms256m -Xmx512m`
- 中型部署（50-200 台）：`-Xms512m -Xmx1g`
- 大型部署（200+ 台）：`-Xms1g -Xmx2g`

---

## 十、技术支持

- 项目基于开源 opsmonitor v3.6.3 (Apache-2.0) 二次开发
- 作者：OpsMonitor 团队
- 版本：v3.6.3
- 更新日期：2026-08-11

### 主要新增功能

1. **品牌定制**：登录页 EdgeOne 风格美化、后台自定义系统名称/版权
2. **告警通道扩展**：钉钉机器人（加签）、企业微信群机器人
3. **监控能力增强**：TCP 端口监控、HTTP 接口监控
4. **告警阈值动态配置**：后台可调 CPU/内存/磁盘告警阈值
5. **验证码安全加固**：SecureRandom + 一次性消费 + 有效期 + 自动刷新
6. **Dashboard 美化**：深色渐变 banner + 卡片悬停效果
7. **Docker 化部署**：完整 Dockerfile + docker-compose + 1Panel 适配
