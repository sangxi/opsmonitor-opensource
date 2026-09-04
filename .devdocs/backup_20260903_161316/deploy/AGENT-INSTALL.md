# OpsMonitor Agent 安装指南

本文档介绍如何在不同操作系统的被监控主机上安装 OpsMonitor Agent，将主机指标（CPU、内存、磁盘、进程等）上报到 Server 端。

> **前置条件**：OpsMonitor Server 已部署完成并能正常访问。假设 Server 访问地址为 `http://192.168.40.111:9999`，通信 token 为 `opsmonitor`。

## 1. 准备 Agent 安装包

在打包机器（开发机）上执行 Maven 打包：

```bash
cd opsmonitor-opensource
mvn clean package -DskipTests
```

打包完成后，安装包位置：

- Linux/Mac：`opsmonitor-agent/target/opsmonitor-agent-release.jar`
- Windows：`opsmonitor-agent\target\opsmonitor-agent-release.jar`

将 `opsmonitor-agent-release.jar` 拷贝到每台被监控主机上即可，无需其他依赖（只需 JRE 8+）。

## 2. 准备配置文件

在被监控主机上，与 jar 同目录创建 `application.yml`：

```yaml
server:
  port: 9998
  servlet:
    context-path: /opsmonitor-agent

spring:
  application:
    name: opsmonitor-agent

logging:
  file:
    path: ./log
    name: ./log/opsmonitor-agent.log
  level:
    root: INFO
    com.opsmonitor: INFO

# 自定义参数
base:
  # Server 端访问地址（改成你的实际 Server 地址）
  serverUrl: http://192.168.40.111:9999
  # 本机 IP（不要写 127.0.0.1 或 localhost，每台被监控主机要唯一）
  bindIp: 192.168.40.112
  # 通信 token，必须与 Server 端 application.yml 中的 params.wgToken 一致
  wgToken: opsmonitor
```

关键字段说明：

| 字段 | 说明 |
|---|---|
| `serverUrl` | Server 端的访问地址（含端口，不含 context-path） |
| `bindIp` | 被监控主机的实际 IP，**每台机器必须唯一**，不能填 127.0.0.1 |
| `wgToken` | 与 Server 端一致的通信 token，鉴权用 |

## 3. 各操作系统安装方式

### 3.1 Linux 安装（推荐生产环境）

#### 3.1.1 安装 JRE 8+

```bash
# CentOS / RHEL
sudo yum install -y java-1.8.0-openjdk

# Ubuntu / Debian
sudo apt-get update && sudo apt-get install -y openjdk-8-jre

# 验证
java -version
```

#### 3.1.2 部署 Agent

```bash
# 创建目录
sudo mkdir -p /opt/opsmonitor-agent
sudo chown $USER:$USER /opt/opsmonitor-agent

# 上传 jar 和 application.yml 到该目录
# 假设已上传，最终目录结构：
#   /opt/opsmonitor-agent/opsmonitor-agent-release.jar
#   /opt/opsmonitor-agent/application.yml

# 测试运行
cd /opt/opsmonitor-agent
java -Xms64m -Xmx128m -Dfile.encoding=UTF-8 \
  -jar opsmonitor-agent-release.jar \
  --spring.config.location=./application.yml
```

看到 `Started WgcloudServiceApplication` 即启动成功，按 Ctrl+C 停止。

#### 3.1.3 配置 systemd 开机自启（推荐）

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
StandardOutput=append:/opt/opsmonitor-agent/log/stdout.log
StandardError=append:/opt/opsmonitor-agent/log/stderr.log

[Install]
WantedBy=multi-user.target
```

启动并设置开机自启：

```bash
sudo systemctl daemon-reload
sudo systemctl enable opsmonitor-agent
sudo systemctl start opsmonitor-agent
sudo systemctl status opsmonitor-agent   # 查看运行状态
sudo journalctl -u opsmonitor-agent -f   # 实时查看日志
```

常用运维命令：

```bash
sudo systemctl restart opsmonitor-agent  # 重启
sudo systemctl stop opsmonitor-agent      # 停止
sudo systemctl status opsmonitor-agent   # 状态
```

### 3.2 Windows 安装

#### 3.2.1 安装 JRE 8+

下载并安装 [Oracle JDK 8](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html) 或 [OpenJDK 8](https://adoptium.net/temurin/releases/?version=8)，配置环境变量后验证：

```powershell
java -version
```

#### 3.2.2 部署 Agent

创建目录 `C:\opsmonitor-agent`，将 `opsmonitor-agent-release.jar` 和 `application.yml` 拷入。

测试运行（在 PowerShell 中）：

```powershell
cd C:\opsmonitor-agent
java -Xms64m -Xmx128m -Dfile.encoding=UTF-8 -jar opsmonitor-agent-release.jar --spring.config.location=.\application.yml
```

#### 3.2.3 注册为 Windows 服务（推荐）

下载 [WinSW](https://github.com/winsw/winsw/releases)，将 `WinSW.exe` 重命名为 `opsmonitor-agent.exe` 放到 `C:\opsmonitor-agent\`，并创建同目录的 `opsmonitor-agent.xml`：

```xml
<service>
  <id>opsmonitor-agent</id>
  <name>OpsMonitor Agent</name>
  <description>OpsMonitor 运维监控 Agent</description>
  <executable>java</executable>
  <arguments>-Xms64m -Xmx128m -Dfile.encoding=UTF-8 -jar C:\opsmonitor-agent\opsmonitor-agent-release.jar --spring.config.location=C:\opsmonitor-agent\application.yml</arguments>
  <workingdirectory>C:\opsmonitor-agent</workingdirectory>
  <logpath>C:\opsmonitor-agent\log</logpath>
  <log mode="roll"></log>
  <onfailure action="restart" delay="10 sec"/>
</service>
```

安装并启动服务（管理员 PowerShell）：

```powershell
cd C:\opsmonitor-agent
.\opsmonitor-agent.exe install
.\opsmonitor-agent.exe start
.\opsmonitor-agent.exe status    # 查看状态
```

常用命令：

```powershell
.\opsmonitor-agent.exe stop     # 停止
.\opsmonitor-agent.exe restart   # 重启
.\opsmonitor-agent.exe uninstall # 卸载服务
```

服务管理也可通过 `services.msc` 找到 `OpsMonitor Agent` 进行操作。

### 3.3 macOS 安装

#### 3.3.1 安装 JRE 8+

```bash
# 使用 Homebrew 安装（如已安装 Homebrew）
brew install openjdk@8

# 配置 JAVA_HOME（zsh）
echo 'export PATH="/opt/homebrew/opt/openjdk@8/bin:$PATH"' >> ~/.zshrc
echo 'export CPPFLAGS="-I/opt/homebrew/opt/openjdk@8/include"' >> ~/.zshrc
source ~/.zshrc

# 验证
java -version
```

#### 3.3.2 部署 Agent

```bash
sudo mkdir -p /opt/opsmonitor-agent
sudo chown $USER /opt/opsmonitor-agent

# 拷贝 jar 和 application.yml 到 /opt/opsmonitor-agent/
cd /opt/opsmonitor-agent

# 测试运行
java -Xms64m -Xmx128m -Dfile.encoding=UTF-8 \
  -jar opsmonitor-agent-release.jar \
  --spring.config.location=./application.yml
```

#### 3.3.3 配置 launchd 开机自启（推荐）

创建 `~/Library/LaunchAgents/com.opsmonitor.agent.plist`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.opsmonitor.agent</string>
    <key>ProgramArguments</key>
    <array>
        <string>/usr/bin/java</string>
        <string>-Xms64m</string>
        <string>-Xmx128m</string>
        <string>-Dfile.encoding=UTF-8</string>
        <string>-jar</string>
        <string>/opt/opsmonitor-agent/opsmonitor-agent-release.jar</string>
        <string>--spring.config.location=/opt/opsmonitor-agent/application.yml</string>
    </array>
    <key>WorkingDirectory</key>
    <string>/opt/opsmonitor-agent</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>StandardOutPath</key>
    <string>/opt/opsmonitor-agent/log/stdout.log</string>
    <key>StandardErrorPath</key>
    <string>/opt/opsmonitor-agent/log/stderr.log</string>
</dict>
</plist>
```

加载并启动：

```bash
mkdir -p /opt/opsmonitor-agent/log
launchctl load ~/Library/LaunchAgents/com.opsmonitor.agent.plist
launchctl list | grep opsmonitor   # 查看运行状态
```

停止与卸载：

```bash
launchctl unload ~/Library/LaunchAgents/com.opsmonitor.agent.plist
```

## 4. Docker 部署（可选）

如果被监控主机本身是 Docker 容器或希望用容器方式运行 Agent，使用项目根目录下的 `deploy/docker-compose.yml`：

```bash
cd opsmonitor-opensource
# 修改 .env 中的 SERVER_URL、BIND_IP、WGTOKEN
docker compose up -d opsmonitor-agent
docker compose logs -f opsmonitor-agent
```

> Docker 方式部署的 Agent 监控的是**容器所在宿主机**的资源（除非用 `--net=host`），生产监控宿主机指标时建议用前述原生安装方式。

## 5. 验证 Agent 是否正常工作

1. **查看 Agent 日志**：日志目录 `./log/opsmonitor-agent.log`，看到 `Started WgcloudServiceApplication` 即启动成功。
2. **登录 Server 后台** → 资源管理 → 主机管理，应能看到刚加入的主机（IP 为 `bindIp` 配置值），状态为绿色（10 分钟内有数据上报）。
3. **查看指标**：点击主机的"系统信息"或"图表"，可看到 CPU、内存、磁盘、网络等实时指标。
4. **排错**：若主机未出现，检查：
   - `serverUrl` 是否正确，能否从被监控主机访问（`curl http://192.168.40.111:9999/opsmonitor/login/toLogin` 应返回页面或 401）
   - `wgToken` 是否与 Server 端一致
   - `bindIp` 是否与该主机实际 IP 一致，且每台机器唯一
   - 防火墙是否放行 Agent→Server 的 9999 端口出站

## 6. 常见问题

### Q1：bindIp 该填什么？

填该被监控主机的**实际局域网 IP**（如 `192.168.40.112`），不要填 `127.0.0.1` 或 `localhost`。多台主机之间 bindIp **不能重复**，否则后加入的会覆盖前一台的数据。

### Q2：Agent 会占用多少资源？

Agent 内存占用约 64-128 MB，CPU 占用通常 < 1%，对被监控主机几乎无影响。

### Q3：如何修改 Agent 上报频率？

Agent 默认每分钟采集并上报一次。如需修改，编辑 `opsmonitor-agent/src/main/java/com/wgcloud/ScheduledTask.java` 中的 `@Scheduled` 注解的 `fixedRate` 值（单位毫秒）后重新打包。

### Q4：Agent 端口 9998 是干什么用的？

Agent 自带一个 HTTP 服务（端口 9998，context-path `/opsmonitor-agent`），主要用于 Server 端主动调用 Agent 进行心跳验证、远程指令等。生产环境可不开放下游访问，只要保证 Agent 能主动访问到 Server 的 9999 端口即可正常工作。

### Q5：如何卸载 Agent？

- Linux systemd：`sudo systemctl stop opsmonitor-agent && sudo systemctl disable opsmonitor-agent && sudo rm /etc/systemd/system/opsmonitor-agent.service && sudo rm -rf /opt/opsmonitor-agent`
- Windows：`.\opsmonitor-agent.exe stop && .\opsmonitor-agent.exe uninstall`，然后删除 `C:\opsmonitor-agent` 目录
- macOS：`launchctl unload ~/Library/LaunchAgents/com.opsmonitor.agent.plist && rm ~/Library/LaunchAgents/com.opsmonitor.agent.plist && sudo rm -rf /opt/opsmonitor-agent`

最后在 Server 后台 → 主机管理 中删除该主机记录即可。
