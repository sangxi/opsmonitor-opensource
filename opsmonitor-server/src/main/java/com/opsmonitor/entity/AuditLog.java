package com.opsmonitor.entity;

import java.util.Date;

/**
 * @ClassName:AuditLog.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 操作审计日志实体
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
public class AuditLog extends BaseEntity {

    private static final long serialVersionUID = -2718281828459045235L;

    /**
     * 操作账号
     */
    private String account;

    /**
     * 操作类型：login/logout/config/export/api/other
     */
    private String action;

    /**
     * 操作内容
     */
    private String content;

    /**
     * 来源IP
     */
    private String ip;

    /**
     * 结果：success/fail
     */
    private String result;

    /**
     * 创建时间
     */
    private Date createTime;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
