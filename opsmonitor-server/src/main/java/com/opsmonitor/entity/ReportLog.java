package com.opsmonitor.entity;

import java.util.Date;

/**
 * @ClassName:ReportLog.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 定时报表记录
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
public class ReportLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 报表类型：daily=每日 weekly=每周 manual=手动
     */
    private String reportType;

    /**
     * 报表标题
     */
    private String title;

    /**
     * HTML 报表正文
     */
    private String content;

    /**
     * 发送状态：0未发送 1已发送 2发送失败
     */
    private String sendStatus;

    /**
     * 收件人
     */
    private String receiver;

    /**
     * 创建时间
     */
    private Date createTime;

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
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

    public String getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
