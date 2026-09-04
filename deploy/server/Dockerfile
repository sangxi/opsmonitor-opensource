# OpsMonitor Server Dockerfile
# 基于 Temurin JDK 8 运行环境（openjdk:8-jre-slim 已被官方归档，国内镜像站多拒同步，故更换）

FROM eclipse-temurin:8-jre

LABEL maintainer="OpsMonitor"
LABEL description="OpsMonitor 运维监控系统 - Server 端"

# 安装基础工具
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    fontconfig \
    libfreetype6 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 复制 Server jar（从项目根目录的 server/target/ 拷贝）
COPY opsmonitor-server/target/opsmonitor-server-release.jar /app/opsmonitor-server.jar

# 复制外部配置文件（与 compose 挂载目录 deploy/server/config 一致，避免被空目录盖住）
COPY deploy/server/config/application.yml /app/config/application.yml

# 暴露端口：9999=Web, 9998=WebSSH
EXPOSE 9999 9998

# JVM 参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/opsmonitor-server.jar --spring.config.location=/app/config/application.yml --spring.config.additional-location=/app/config/"]
