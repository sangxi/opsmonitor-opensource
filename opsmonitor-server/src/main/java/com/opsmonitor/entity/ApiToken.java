package com.opsmonitor.entity;

import java.util.Date;

/**
 * @ClassName:ApiToken.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 开放 API 访问令牌实体
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
public class ApiToken extends BaseEntity {

    private static final long serialVersionUID = -1618033988749894848L;

    /**
     * API 访问令牌
     */
    private String token;

    /**
     * 令牌名称
     */
    private String name;

    /**
     * 状态：1启用 0停用
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 过期时间，空为永不过期
     */
    private Date expireTime;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }
}
