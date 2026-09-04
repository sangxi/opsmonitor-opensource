package com.opsmonitor.entity;

import java.util.Date;

/**
 * @version v2.0
 * @ClassName:Help.java
 * @author: OpsMonitor Team
 * @Description: 使用教程内容
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
public class Help extends BaseEntity {

    private static final long serialVersionUID = 8275401182049154321L;

    /**
     * 教程ID
     */
    private String id;

    /**
     * 分类ID
     */
    private String categoryId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容（Markdown）
     */
    private String content;

    /**
     * 排序
     */
    private Integer sortNum;

    /**
     * 状态 1启用 0停用
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    // ---- 联查字段（非表字段）----
    private String categoryName;
    private String categoryIcon;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
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

    public Integer getSortNum() {
        return sortNum;
    }

    public void setSortNum(Integer sortNum) {
        this.sortNum = sortNum;
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

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }
}
