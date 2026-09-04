package com.opsmonitor.entity;

/**
 * @version v2.0
 * @ClassName:HelpCategory.java
 * @author: OpsMonitor Team
 * @Description: 使用教程分类
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
public class HelpCategory extends BaseEntity {

    private static final long serialVersionUID = 6821201438657189101L;

    /**
     * 分类ID
     */
    private String id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 图标（fontawesome）
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sortNum;

    /**
     * 状态 1启用 0停用
     */
    private String status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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
}
