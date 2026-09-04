-- =============================================================
-- OpsMonitor 使用教程模块 建表 + 种子数据
-- 用法：mysql -h<host> -P<port> -u<user> -p<password> opsmonitor < opsmonitor-help.sql
-- 也可在 宝塔/1Panel 的数据库管理里导入本文件
-- =============================================================

-- 1. 教程分类表
DROP TABLE IF EXISTS `HELP_CATEGORY`;
CREATE TABLE `HELP_CATEGORY` (
  `ID` char(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类ID',
  `NAME` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `ICON` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标(fontawesome)',
  `SORT_NUM` int(11) DEFAULT 0 COMMENT '排序',
  `STATUS` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '状态 1启用 0停用',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用教程分类';

-- 2. 教程内容表
DROP TABLE IF EXISTS `HELP`;
CREATE TABLE `HELP` (
  `ID` char(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教程ID',
  `CATEGORY_ID` char(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类ID',
  `TITLE` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `CONTENT` text COLLATE utf8mb4_unicode_ci COMMENT '内容(Markdown)',
  `SORT_NUM` int(11) DEFAULT 0 COMMENT '排序',
  `STATUS` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '状态 1启用 0停用',
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ID`),
  KEY `IDX_HELP_CATEGORY` (`CATEGORY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用教程内容';

-- 3. 种子数据：分类
INSERT INTO `HELP_CATEGORY` (`ID`, `NAME`, `ICON`, `SORT_NUM`, `STATUS`) VALUES
('cat_linux',   'Linux 部署',   'fab fa-linux',   1, '1'),
('cat_windows', 'Windows 部署', 'fab fa-windows', 2, '1'),
('cat_macos',   'macOS 部署',   'fab fa-apple',   3, '1'),
('cat_docker',  'Docker 部署',  'fab fa-docker',  4, '1'),
('cat_baota',   '宝塔面板',     'fas fa-tower-observation', 5, '1'),
('cat_onepanel','1Panel 部署',  'fas fa-layer-group', 6, '1'),
('cat_arm',     'ARM/树莓派',   'fas fa-microchip', 7, '1'),
('cat_alarm',   '告警配置',     'fas fa-bell',     8, '1'),
('cat_faq',     '常见问题 FAQ', 'fas fa-question-circle', 9, '1');

-- 4. 种子数据：教程内容
INSERT INTO `HELP` (`ID`, `CATEGORY_ID`, `TITLE`, `CONTENT`, `SORT_NUM`, `STATUS`) VALUES

-- ============ Linux 部署 ============
('help_linux_1', 'cat_linux', 'Linux 环境部署 Server（监控中心）',
'## 适用环境
- 系统：CentOS 7+ / Ubuntu 18+ / Debian 10+
- 依赖：JDK 8、MySQL 5.7+、Maven 3.6+（仅源码打包需要）

## 一、需要准备的文件
| 文件 | 来源 | 说明 |
|------|------|------|
| `opsmonitor-server-release.jar` | 源码 `opsmonitor-server/target/` 打包 | Server 主程序 |
| `opsmonitor-MySQL.sql` | 项目 `sql/` 目录 | 数据库初始化脚本 |
| `application.yml` | `deploy/server/config/` | Server 外部配置（含数据库/token） |
| `opsmonitor-agent-release.jar` | 源码 `opsmonitor-agent/target/` 打包 | 被监控端 Agent（可选） |

## 二、初始化数据库
\`\`\`bash
mysql -u root -p < sql/opsmonitor-MySQL.sql
\`\`\`

## 三、修改配置
编辑 `application.yml`，确认：
\`\`\`yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/opsmonitor?characterEncoding=utf8
    username: opsmonitor
    password: 你的密码
base:
  omToken: 与Agent一致的通信token
\`\`\`

## 四、启动 Server
\`\`\`bash
java -Xms256m -Xmx512m -jar opsmonitor-server-release.jar --spring.config.location=./application.yml
\`\`\`
看到 `Started OpsMonitorServiceApplication` 即启动成功。

## 五、systemd 开机自启
\`\`\`ini
[Unit]
Description=OpsMonitor Server
After=network.target mysql.service
[Service]
Type=simple
User=root
WorkingDirectory=/opt/opsmonitor-server
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/opsmonitor-server/opsmonitor-server-release.jar
Restart=on-failure
RestartSec=10
[Install]
WantedBy=multi-user.target
\`\`\`
\`\`\`bash
systemctl daemon-reload && systemctl enable opsmonitor-server && systemctl start opsmonitor-server
\`\`\`

## 六、验证
浏览器访问 `http://IP:9999/opsmonitor/login/toLogin`，默认账号 admin / 111111。',
1, '1'),

('help_linux_2', 'cat_linux', 'Linux 被监控端 Agent 安装',
'## 适用环境
- 任何 Linux 发行版（x86_64 / aarch64 / armhf）

## 一、需要准备的文件
| 文件 | 来源 | 说明 |
|------|------|------|
| `opsmonitor-agent-release.jar` | `opsmonitor-agent/target/` | Agent 程序 |
| `application.yml` | `deploy/agent/config/` | Agent 配置（serverUrl + omToken） |

## 二、安装步骤
\`\`\`bash
# 1. 创建目录并上传文件
mkdir -p /opt/opsmonitor-agent
cp opsmonitor-agent-release.jar /opt/opsmonitor-agent/
cp application.yml /opt/opsmonitor-agent/

# 2. 修改配置
vi /opt/opsmonitor-agent/application.yml
# serverUrl: http://监控中心IP:9999
# omToken: 与Server端一致

# 3. 启动
cd /opt/opsmonitor-agent
nohup java -Xms64m -Xmx128m -jar opsmonitor-agent-release.jar > agent.out 2>&1 &
\`\`\`

## 三、验证
查看日志 `./log/opsmonitor-agent.log`，出现 `Started OpsMonitorServiceApplication` 即成功。约 1 分钟后 Server 端「监控概要」可见该主机。

> 提示：被监控主机需能访问 Server 的 9999 端口；Docker 容器内运行 Agent 只能监控容器自身。',
2, '1'),

-- ============ Windows 部署 ============
('help_win_1', 'cat_windows', 'Windows 部署 Server / Agent',
'## 适用环境
- Windows Server 2008 R2+ / Windows 7 / 10 / 11
- 需安装 JDK 8 并配置环境变量 JAVA_HOME

## 一、需要准备的文件
| 文件 | 说明 |
|------|------|
| `opsmonitor-server-release.jar` | Server 主程序（部署监控中心时） |
| `opsmonitor-agent-release.jar` | Agent 程序（部署被监控机时） |
| `application.yml` | 对应配置文件 |
| `opsmonitor-MySQL.sql` | 数据库脚本（Server 部署时） |

## 二、安装 JDK 8
下载 JDK 8 安装包，安装后设置环境变量：
\`\`\`powershell
# 系统属性 → 环境变量
JAVA_HOME = C:\\Program Files\\Java\\jdk1.8.0_481
PATH 追加 = %JAVA_HOME%\\bin
\`\`\`

## 三、启动 Server（前台）
\`\`\`powershell
cd D:\\opsmonitor-server
java -Xms256m -Xmx512m -jar opsmonitor-server-release.jar --spring.config.location=./application.yml
\`\`\`

## 四、注册为 Windows 服务（WinSW）
使用 WinSW 工具将 jar 注册为系统服务：
\`\`\`xml
<!-- opsmonitor-server.xml -->
<service>
  <id>opsmonitor-server</id>
  <name>OpsMonitor Server</name>
  <executable>java</executable>
  <arguments>-Xms256m -Xmx512m -jar D:\\opsmonitor-server\\opsmonitor-server-release.jar</arguments>
  <logmode>rotate</logmode>
</service>
\`\`\`
\`\`\`powershell
# 安装并启动服务
opsmonitor-server.exe install
opsmonitor-server.exe start
\`\`\`

## 五、验证
浏览器访问 `http://IP:9999/opsmonitor/login/toLogin`，或任务管理器查看 Java 进程运行。',
1, '1'),

('help_win_2', 'cat_windows', 'Windows Agent 开机自启（计划任务）',
'## 一、准备文件
- `opsmonitor-agent-release.jar`
- `application.yml`（serverUrl 指向监控中心，omToken 一致）

## 二、配置计划任务开机自启
\`\`\`powershell
# 创建计划任务（管理员 PowerShell）
$action = New-ScheduledTaskAction -Execute "C:\\Program Files\\Java\\jdk1.8.0_481\\bin\\java.exe" -Argument "-jar D:\\opsmonitor-agent\\opsmonitor-agent-release.jar"
$trigger = New-ScheduledTaskTrigger -AtStartup
Register-ScheduledTask -TaskName "OpsMonitorAgent" -Action $action -Trigger $trigger -RunLevel Highest
\`\`\`

## 三、说明
- Agent 默认每分钟采集一次并上报
- 日志位于 `./log/opsmonitor-agent.log`
- 被监控机需能访问 Server 的 9999 端口',
2, '1'),

-- ============ macOS 部署 ============
('help_mac_1', 'cat_macos', 'macOS 部署 Agent（被监控）',
'## 适用环境
- macOS 10.15+ / 11 / 12 / 13（Intel amd64 与 Apple Silicon arm64 均可）

## 一、需要准备的文件
| 文件 | 说明 |
|------|------|
| `opsmonitor-agent-release.jar` | Agent 程序（注意架构，Apple Silicon 用 arm64 版） |
| `application.yml` | Agent 配置 |

## 二、安装 JDK 8
\`\`\`bash
# 使用 Homebrew 安装 OpenJDK 8
brew install openjdk@8
# 配置 PATH
echo '"'"'export PATH="/usr/local/opt/openjdk@8/bin:$PATH"'"'"' >> ~/.zshrc
source ~/.zshrc
\`\`\`

## 三、安装 Agent
\`\`\`bash
# 1. 上传文件
mkdir -p ~/opsmonitor-agent
cp opsmonitor-agent-release.jar application.yml ~/opsmonitor-agent/
cd ~/opsmonitor-agent

# 2. 修改配置
# serverUrl: http://监控中心IP:9999
# omToken: 与Server端一致

# 3. 启动
java -Xms64m -Xmx128m -jar opsmonitor-agent-release.jar &
\`\`\`

## 四、launchd 开机自启
创建 `~/Library/LaunchAgents/com.opsmonitor.agent.plist`：
\`\`\`xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key><string>com.opsmonitor.agent</string>
    <key>ProgramArguments</key>
    <array>
        <string>/usr/bin/java</string>
        <string>-jar</string>
        <string>/Users/你的用户名/opsmonitor-agent/opsmonitor-agent-release.jar</string>
    </array>
    <key>RunAtLoad</key><true/>
    <key>KeepAlive</key><true/>
</dict>
</plist>
\`\`\`
\`\`\`bash
launchctl load ~/Library/LaunchAgents/com.opsmonitor.agent.plist
\`\`\`

## 五、验证
查看 `~/opsmonitor-agent/log/opsmonitor-agent.log`，出现启动成功日志即可。',
1, '1'),

-- ============ Docker 部署 ============
('help_docker_1', 'cat_docker', 'Docker Compose 一键部署（推荐）',
'## 适用环境
- 装有 Docker 20.10+ 与 Docker Compose 2.0+ 的 Linux 服务器
- 也适配 1Panel 的「容器编排」功能

## 一、需要准备的文件
| 文件 | 来源 | 说明 |
|------|------|------|
| `docker-compose.yml` | `deploy/` | 编排文件（server + agent 两个服务） |
| `.env` | `deploy/` | 环境变量（数据库/token/端口） |
| `deploy/server/Dockerfile` | `deploy/` | Server 镜像构建 |
| `deploy/agent/Dockerfile` | `deploy/` | Agent 镜像构建 |
| `deploy/server/config/application.yml` | `deploy/` | Server 外部配置 |
| `deploy/agent/config/application.yml` | `deploy/` | Agent 外部配置 |
| `opsmonitor-server-release.jar` | 打包产物 | 会被 Dockerfile 复制进镜像 |
| `opsmonitor-agent-release.jar` | 打包产物 | 同上 |

## 二、修改 .env
\`\`\`bash
cd opsmonitor-opensource
# 编辑 deploy/.env
# DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD
# OMTOKEN（Server 与 Agent 必须一致）
# SERVER_PORT（宿主机映射端口，默认 9999）
# SERVER_URL=http://opsmonitor-server:9999（Docker 内部通信，保持默认）
\`\`\`

## 三、初始化数据库（首次部署）
\`\`\`bash
cd deploy && bash init-db.sh
\`\`\`

## 四、打包 jar（若没有现成 jar）
\`\`\`bash
cd opsmonitor-server && mvn clean package -DskipTests
cd ../opsmonitor-agent && mvn clean package -DskipTests
\`\`\`

## 五、构建并启动
\`\`\`bash
cd /opt/opsmonitor
docker-compose -f deploy/docker-compose.yml up -d --build
\`\`\`

## 六、验证
\`\`\`bash
docker ps | grep opsmonitor   # 两个容器均 Up
docker logs -f opsmonitor-server  # 看到 Started OpsMonitorServiceApplication
curl -I http://localhost:9999/opsmonitor/login/toLogin
\`\`\`

> 注意：Agent 容器只能监控容器自身；要监控宿主机请在宿主机直接跑 Agent。',
1, '1'),

('help_docker_2', 'cat_docker', 'Docker 常用运维命令',
'## 常用命令速查
\`\`\`bash
# 启动 / 停止 / 重启
docker-compose -f deploy/docker-compose.yml up -d
docker-compose -f deploy/docker-compose.yml down
docker-compose -f deploy/docker-compose.yml restart

# 查看日志
docker logs -f opsmonitor-server
docker logs -f opsmonitor-agent

# 升级后重新构建
docker-compose -f deploy/docker-compose.yml up -d --build

# 进入容器
docker exec -it opsmonitor-server bash
\`\`\`

## 挂载目录说明
| 容器 | 挂载 |
|------|------|
| server | `./server/logs:/app/logs`、`./server/config:/app/config` |
| agent | `./agent/logs:/app/logs`、`./agent/config:/app/config`，`pid: host` |

> 修改挂载的 `config/application.yml` 后需 `docker-compose restart` 生效。',
2, '1'),

-- ============ 宝塔面板 ============
('help_baota_1', 'cat_baota', '宝塔面板部署完整流程',
'## 适用环境
- 宝塔面板 7.x + Docker 管理器（或已安装 Docker）

## 一、需要准备的文件
| 文件 | 说明 |
|------|------|
| `opsmonitor-opensource` 整个项目目录 | 上传到服务器 `/www/opsmonitor/` |
| `deploy/docker-compose.yml` | 编排文件 |
| `deploy/.env` | 环境变量（必改） |
| `opsmonitor-MySQL.sql` | 数据库脚本 |

## 二、创建数据库
宝塔 → 数据库 → 添加数据库：
- 数据库名：`opsmonitor`
- 用户名：`opsmonitor`
- 密码：自己设置

## 三、导入 SQL
宝塔 → 数据库 → 选中 `opsmonitor` → 管理 → 导入 `sql/opsmonitor-MySQL.sql`。

## 四、修改 .env
\`\`\`ini
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=opsmonitor
DB_USER=opsmonitor
DB_PASSWORD=你的密码
OMTOKEN=替换成32位以上随机字符串
SERVER_PORT=9999
\`\`\`

## 五、Docker 编排启动
宝塔 → Docker → 编排（Compose）→ 新建 → 上传/粘贴 `docker-compose.yml` → 启动。

或命令行：
\`\`\`bash
cd /www/opsmonitor/opsmonitor-opensource
docker-compose -f deploy/docker-compose.yml up -d --build
\`\`\`

## 六、放行端口
宝塔 → 安全 → 放行 9999 端口；云服务器安全组同步放行。

## 七、验证
访问 `http://IP:9999/opsmonitor/login/toLogin`，账号 admin / 111111。',
1, '1'),

-- ============ 1Panel ============
('help_onepanel_1', 'cat_onepanel', '1Panel 面板部署',
'## 适用环境
- 1Panel 面板（Linux 服务器管理面板）

## 一、需要准备的文件
| 文件 | 说明 |
|------|------|
| `opsmonitor-opensource` 项目目录 | 上传到服务器 `/opt/opsmonitor/` |
| `deploy/docker-compose.yml` | 1Panel 容器编排识别 |
| `deploy/.env` | 环境变量 |
| `sql/opsmonitor-MySQL.sql` | 数据库脚本 |

## 二、创建数据库
1Panel → 数据库 → MySQL → 创建数据库 `opsmonitor`，记下用户/密码。

## 三、导入 SQL
1Panel → 数据库 → 选择 `opsmonitor` → 导入 `sql/opsmonitor-MySQL.sql`。

## 四、修改 .env
\`\`\`ini
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=opsmonitor
DB_USER=opsmonitor
DB_PASSWORD=你的密码
OMTOKEN=随机长字符串
SERVER_PORT=9999
\`\`\`

## 五、容器编排
1Panel → 容器 → 编排 → 创建 → 选择 `deploy/docker-compose.yml` → 启动。
1Panel 会自动识别 compose 项目，支持启动/停止/日志/终端。

## 六、防火墙
1Panel → 主机 → 防火墙 → 放行 9999。

## 七、验证
浏览器访问 `http://IP:9999/opsmonitor/login/toLogin`。',
1, '1'),

-- ============ ARM / 树莓派 ============
('help_arm_1', 'cat_arm', 'ARM / 树莓派部署',
'## 适用环境
- 树莓派 3B/4B、ARM 服务器（arm64/aarch64、armhf）
- 需要 JDK 8 的 ARM 版本（如 openjdk-8-jre-armhf / arm64）

## 一、需要准备的文件
| 文件 | 说明 |
|------|------|
| `opsmonitor-agent-release.jar` | Agent（选择对应 ARM 架构构建的 jar） |
| `application.yml` | Agent 配置 |
| （可选）`opsmonitor-server-release.jar` | 若 ARM 设备作为监控中心 |

## 二、安装 JDK 8
\`\`\`bash
# Debian / Ubuntu ARM
apt-get install -y openjdk-8-jre
# CentOS ARM
yum install -y java-1.8.0-openjdk
\`\`\`

## 三、安装 Agent
\`\`\`bash
mkdir -p /opt/opsmonitor-agent
cp opsmonitor-agent-release.jar application.yml /opt/opsmonitor-agent/
cd /opt/opsmonitor-agent
# 修改 application.yml 的 serverUrl 和 omToken
nohup java -Xms64m -Xmx128m -jar opsmonitor-agent-release.jar > agent.out 2>&1 &
\`\`\`

## 四、说明
- ARM 设备建议作为被监控端（Agent）使用，功耗低
- 若 ARM 设备内存 < 1G，Server 端建议内存充足，Agent 用 `-Xmx64m` 即可',
1, '1'),

-- ============ 告警配置 ============
('help_alarm_1', 'cat_alarm', '告警通道配置（邮件/钉钉/企业微信）',
'## 一、邮件告警（SMTP）
进入「邮件告警」→「设置」：
- SMTP 服务器地址（如 smtp.qq.com / smtp.163.com）
- 端口（SSL 465 / 普通 25）
- 发件邮箱 + 授权码
- 收件人邮箱（可多个，逗号分隔）
- 设置 CPU / 内存 / 磁盘告警阈值（%）

点击「发送测试邮件」验证配置。

## 二、钉钉机器人告警
1. 钉钉群 → 群设置 → 智能群助手 → 添加机器人 → 自定义
2. 记录 Webhook 地址和加签密钥（Secret）
3. 在「邮件告警」设置中填写：
   - 开启「发送Webhook告警」
   - 钉钉 Webhook 地址
   - 加签密钥

> 加签方式：使用 HMAC-SHA256 对时间戳+密钥做签名，系统自动计算。

## 三、企业微信机器人告警
1. 企业微信群 → 群机器人 → 添加
2. 复制 Webhook 地址
3. 在设置中填写「企业微信 Webhook」

## 四、告警触发时机
- CPU / 内存 / 磁盘使用率超过设定阈值
- 服务接口（HTTP/TCP）返回异常
- 主机离线（Agent 上报超时）',
1, '1'),

('help_alarm_2', 'cat_alarm', '通信 Token 与安全配置',
'## 一、通信 Token（omToken）
Server 与 Agent 之间的鉴权凭证，二者必须完全一致。

- Server：`deploy/.env` 中的 `OMTOKEN`，或 `application.yml` 中 `base.omToken`
- Agent：`application.yml` 中 `base.omToken`

建议设置为 32 位以上随机字符串：
\`\`\`bash
# 生成随机 token
openssl rand -hex 32
\`\`\`

> 修改后需重启 Server 与所有 Agent，否则鉴权失败。

## 二、修改默认密码
- 默认账号 admin / 111111
- 生产环境部署后立即在「系统设置」中修改

## 三、环境变量
| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `OPSMONITOR_DB_URL` | 数据库连接 | 本机 opsmonitor 库 |
| `OPSMONITOR_DB_USER` | 数据库用户 | opsmonitor |
| `OPSMONITOR_DB_PWD` | 数据库密码 | 空 |
| `OPSMONITOR_ADMIN_PWD` | 管理员密码 | 111111 |
| `OPSMONITOR_OM_TOKEN` | 通信 token | opsmonitor |',
2, '1'),

-- ============ FAQ ============
('help_faq_1', 'cat_faq', '常见问题 FAQ',
'## Q1：打包报「Could not reserve enough space for object heap」
内存不足，限制 Maven 堆大小：
\`\`\`bash
export MAVEN_OPTS="-Xms128m -Xmx1024m -XX:MaxMetaspaceSize=256m"
mvn clean package -DskipTests
\`\`\`

## Q2：MySQL 连接失败「Unknown column」错误
表结构未升级或未初始化。重新导入 `sql/opsmonitor-MySQL.sql`（全新部署）。

## Q3：访问 9999 端口打不开
按顺序排查：
1. Server 是否启动（`docker ps | grep opsmonitor` 或 `ps -ef | grep opsmonitor`）
2. 端口监听：`netstat -tlnp | grep 9999`
3. 防火墙：`firewall-cmd --add-port=9999/tcp --permanent && firewall-cmd --reload`
4. 云服务器安全组是否放行 9999

## Q4：验证码图片不显示
- F12 控制台看 `/code/generate` 请求
- 返回 200 + image/jpeg：正常，刷新页面
- 返回 200 + text/html：被重定向，检查登录状态
- 返回 404：URL 错误，确认 context-path 为 `/opsmonitor`

## Q5：Docker Agent 监控不到宿主机数据
Docker 容器内只能监控容器自身。监控宿主机需在宿主机直接运行 Agent。

## Q6：登录提示「验证码已失效」
正常行为。验证码一次性消费，点击验证码图片即可刷新。

## Q7：登录失败次数过多被锁定
连续 5 次密码错误锁定 30 分钟。等待自动解锁或重启 Server 进程。

## Q8：历史数据保留与清理
- 监控状态表会随运行时间增长
- 可定期清理：DELETE 旧数据或调整采集周期',
1, '1'),

('help_faq_2', 'cat_faq', '升级与备份指南',
'## 一、数据库备份
\`\`\`bash
# 全量备份
mysqldump -h127.0.0.1 -uopsmonitor -p opsmonitor > opsmonitor_$(date +%Y%m%d).sql

# 定时备份（每天凌晨 3 点）
echo "0 3 * * * mysqldump -h127.0.0.1 -uopsmonitor -p你的密码 opsmonitor > /backup/opsmonitor_$(date +\\%Y\\%m\\%d).sql" | crontab -
\`\`\`

## 二、升级流程
1. 备份数据库
2. 停止服务（`docker-compose down` 或 `stop.sh`）
3. 替换代码（保留 `deploy/.env` 和 `sql/`）
4. 重新打包：`mvn clean package -DskipTests`
5. 如有 SQL 升级脚本则执行
6. 启动并验证

## 三、性能调优
`deploy/server/config/application.yml`：
\`\`\`yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
server:
  tomcat:
    max-threads: 200
\`\`\`
JVM：小型（≤50台）-Xms256m -Xmx512m；中型（50-200台）-Xms512m -Xmx1g；大型（200+台）-Xms1g -Xmx2g。',
2, '1');
