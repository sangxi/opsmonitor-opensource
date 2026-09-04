<p align="center">
  <a target="_blank" href="#">
    <img src="./demo/logo.png" alt="OpsMonitor" width="120">
  </a>
</p>

<h1 align="center">OpsMonitor 运维监控系统</h1>

<p align="center">
  轻量级分布式运维监控平台 · 极简部署 · 自动化运行 · 零模板零脚本
</p>

---

## 项目简介

OpsMonitor 是一款开箱即用的轻量级分布式运维监控系统，主打**极简部署、自动化运行、零模板零脚本**。Server 端 + Agent 端协同工作，可监控数千台主机。

**核心监控指标**：CPU 使用率、CPU 温度、内存使用率、磁盘容量、磁盘 IO、硬盘 SMART 健康状态、系统负载、MAC 地址、连接数量、网卡流量、硬件系统信息等。

**支持的资源监控**：
- 服务器进程应用
- 文件防篡改
- 端口监听
- 日志文件
- Docker 容器
- 数据库
- 数据表
- 服务接口 API
- 数通设备（交换机、路由器、打印机等）

**可视化与告警**：
- 自动生成网络拓扑图
- **自研监控大屏**（深色科技风，ECharts 可视化，支持全屏投屏）
- Web SSH（堡垒机）
- 统计分析图表
- 指令下发批量执行
- 告警信息推送（邮件、钉钉、微信、短信等）
- **内置使用教程**（各系统部署指南 + 文件挂载清单）

**关键特性**：
1. 极轻量，Server 端基于 Spring Boot 2.6.6 + JDK 8
2. 采集端使用 OSHI 组件，跨平台一致
3. 默认每分钟上报一次（可调）
4. 支持 Linux / Windows / macOS / Unix
5. 全中文 UI，部署即用
6. 支持 Docker Compose / 1Panel / 宝塔面板 / 裸 JAR 多种部署方式

---

## 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                     浏览器 (Web UI)                       │
│   登录页 · 监控概览 · 主机详情 · 监控大屏 · 使用教程        │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP / HTTPS
┌──────────────────────────▼──────────────────────────────┐
│              OpsMonitor Server (端口 9999)               │
│   Spring Boot 2.6.6 + Thymeleaf + AdminLTE3 + ECharts   │
│   数据聚合 / 告警判断 / 邮件·钉钉·企微通知 / 报表 / 权限   │
└───────┬──────────────────────────────┬──────────────────┘
        │ 通信 token（omToken，MD5 校验） │ JDBC
┌───────▼──────────────┐      ┌─────────▼─────────────────┐
│  OpsMonitor Agent     │      │        MySQL 数据库        │
│  (端口 9998)          │      │   (默认库名 opsmonitor)     │
│  OSHI 跨平台采集       │      │   主机 / 状态 / 日志 / 配置 │
│  Linux/Windows/macOS  │      └───────────────────────────┘
└──────────────────────┘
```

---

## 运行环境

- **JDK**：1.8 或 11
- **数据库**：MySQL 5.5+ / MariaDB（默认库名 `opsmonitor`）
- **支持系统**：
  - Linux：Debian、RedHat、CentOS、Ubuntu、Fedora、SUSE、麒麟、统信(UOS)、龙芯(mips) 等
  - Windows：Server 2008 R2 / 2012 / 2016 / 2019 / 2022，Windows 7/8/10/11
  - Unix：Solaris、FreeBSD、OpenBSD
  - macOS：amd64 / arm64
  - 其他：ARM、Android(安卓)、riscv64、s390x、树莓派、AIX

---

## 快速开始

### Server 端（监控中心）

```bash
# 1. 导入数据库（首次部署）
mysql -u root -p < sql/opsmonitor-MySQL.sql

# 2. 打包 Server
cd opsmonitor-server
mvn clean package -DskipTests
# 产物：target/opsmonitor-server-release.jar

# 3. 启动
cd ..
java -Xms256m -Xmx512m -jar opsmonitor-server/target/opsmonitor-server-release.jar
```

浏览器访问：`http://服务器IP:9999/opsmonitor/login/toLogin`
- 默认账号：`admin`
- 默认密码：`111111`（**生产环境务必修改**）

### Agent 端（被监控主机）

详见 [deploy/AGENT-INSTALL.md](./deploy/AGENT-INSTALL.md)，覆盖 Linux / Windows / macOS 三平台，含 systemd / Windows 服务 / launchd 自启配置。

> **重要**：Server 与 Agent 的通信 token（`omToken`）必须一致，否则鉴权失败。

---

## 详细部署文档

| 文档 | 适用场景 |
|------|---------|
| [DEPLOY.md](./DEPLOY.md) | 1Panel / 原生 Docker / 直接 JAR 三种部署方案 |
| [宝塔面板部署教程.md](./宝塔面板部署教程.md) | 宝塔面板 + Docker 从零开始 |
| [deploy/AGENT-INSTALL.md](./deploy/AGENT-INSTALL.md) | 被监控端 Agent 安装（Linux/Windows/macOS） |
| 系统内置「使用教程」 | 登录后侧边栏 → 使用教程（各系统部署 + 文件挂载清单） |

---

## 主要功能

1. **品牌定制**：登录页现代化美化、自研品牌图标、后台可自定义系统名称 / 版权
2. **内页深色主题**：整套 AdminLTE 改造为深色现代监控大屏风格（毛玻璃卡片、渐变指标卡、状态呼吸灯）
3. **自研监控大屏**：ECharts 5 深色科技风，12 区块布局，30 秒轮询，支持全屏投屏、免登录访问
4. **告警通道扩展**：邮件、钉钉机器人（加签）、企业微信群机器人
5. **监控能力增强**：TCP 端口监控、HTTP 接口监控
6. **告警阈值动态配置**：后台可调 CPU / 内存 / 磁盘告警阈值
7. **使用教程模块**：侧边栏内置分系统部署指南 + 文件挂载清单 + FAQ
8. **全局搜索**：顶栏搜索主机，快速定位
9. **验证码安全加固**：SecureRandom + 一次性消费 + 有效期 + 自动刷新
10. **Docker 化部署**：完整 Dockerfile + docker-compose + 1Panel / 宝塔适配

---

## 版权与许可

- 本项目基于 Apache-2.0 协议开源
- OpsMonitor 品牌及视觉设计由 OpsMonitor 团队所有
- 商业使用请遵守 Apache-2.0 协议

---

## 联系我们

- 项目主页：本仓库
- 提交 Issue：本仓库 Issues 页

> OpsMonitor 团队 · 2026
