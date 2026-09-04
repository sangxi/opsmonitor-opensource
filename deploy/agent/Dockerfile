# OpsMonitor Agent Dockerfile
# 基于 Temurin JDK 8 运行环境（openjdk:8-jre-slim 已被官方归档，国内镜像站多拒同步，故更换）

FROM eclipse-temurin:8-jre

LABEL maintainer="OpsMonitor"
LABEL description="OpsMonitor 运维监控系统 - Agent 端"

WORKDIR /app

# 复制 Agent jar
COPY opsmonitor-agent/target/opsmonitor-agent-release.jar /app/opsmonitor-agent.jar

# 复制外部配置文件（与 compose 挂载目录 deploy/agent/config 一致，避免被空目录盖住）
COPY deploy/agent/config/application.yml /app/config/application.yml

# 暴露端口
EXPOSE 9998

# JVM 参数（Agent 内存占用较小）
ENV JAVA_OPTS="-Xms64m -Xmx128m -Dfile.encoding=UTF-8"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/opsmonitor-agent.jar --spring.config.location=/app/config/application.yml"]
