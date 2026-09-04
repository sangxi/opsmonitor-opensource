package com.opsmonitor.entity;

import java.util.Date;

/**
 * @version v2.3
 * @ClassName:HeathMonitor.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: app端口信息
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
public class HeathMonitor extends BaseEntity {

    /**
     *
     */
    private static final long serialVersionUID = -2913111613773445949L;


    /**
     * 应用服务名称
     */
    private String appName;

    /**
     * 监控类型：1 HTTP接口 2 TCP端口
     */
    private String monitorType;

    /**
     * TCP 监控目标主机 IP
     */
    private String hostIp;

    /**
     * TCP 监控目标端口
     */
    private Integer port;

    /**
     * TCP 连接超时（毫秒）
     */
    private Integer timeoutMs;

    /**
     * 心跳检测Url
     */
    private String heathUrl;

    /**
     * 状态
     */
    private String heathStatus;


    /**
     * 创建时间
     */
    private Date createTime;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getMonitorType() {
        return monitorType;
    }

    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getHeathUrl() {
        return heathUrl;
    }

    public void setHeathUrl(String heathUrl) {
        this.heathUrl = heathUrl;
    }

    public String getHeathStatus() {
        return heathStatus;
    }

    public void setHeathStatus(String heathStatus) {
        this.heathStatus = heathStatus;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}