package com.opsmonitor.entity;

import java.util.Date;

/**
 * @ClassName:AlarmRecord.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 告警中心记录实体
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
public class AlarmRecord extends BaseEntity {

    private static final long serialVersionUID = -3141592653589793238L;

    /**
     * 告警类型：cpu/mem/disk/hostdown/appdown/heath/intrusion
     */
    private String alarmType;

    /**
     * 告警对象主机名
     */
    private String hostname;

    /**
     * 告警标题
     */
    private String title;

    /**
     * 告警内容
     */
    private String content;

    /**
     * 级别：info/warn/error
     */
    private String level;

    /**
     * 处理状态：0未处理 1已处理 2已忽略
     */
    private String state;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 处理时间
     */
    private Date updateTime;

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
