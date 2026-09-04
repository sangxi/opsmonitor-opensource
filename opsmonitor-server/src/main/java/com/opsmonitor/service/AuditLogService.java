package com.opsmonitor.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.AuditLog;
import com.opsmonitor.mapper.AuditLogMapper;
import com.opsmonitor.util.DateUtil;
import com.opsmonitor.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @ClassName:AuditLogService.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 操作审计日志 Service
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogMapper auditLogMapper;

    public PageInfo selectByParams(Map<String, Object> params, int currPage, int pageSize) throws Exception {
        PageHelper.startPage(currPage, pageSize);
        List<AuditLog> list = auditLogMapper.selectByParams(params);
        PageInfo<AuditLog> pageInfo = new PageInfo<AuditLog>(list);
        return pageInfo;
    }

    /**
     * 记录一条审计日志（失败不抛异常，避免影响主流程）
     */
    public void save(AuditLog log) {
        try {
            if (log.getCreateTime() == null) {
                log.setCreateTime(DateUtil.getNowTime());
            }
            if (log.getId() == null) {
                log.setId(UUIDUtil.getUUID());
            }
            if (log.getResult() == null) {
                log.setResult("success");
            }
            if (log.getAction() == null) {
                log.setAction("other");
            }
            auditLogMapper.save(log);
        } catch (Exception e) {
            logger.error("保存审计日志异常：", e);
        }
    }

    /**
     * 快捷记录
     */
    public void save(String account, String action, String content, String ip, String result) {
        AuditLog log = new AuditLog();
        log.setAccount(account);
        log.setAction(action);
        log.setContent(content);
        log.setIp(ip);
        log.setResult(result);
        save(log);
    }

    public int deleteById(String[] id) throws Exception {
        return auditLogMapper.deleteById(id);
    }

    public int countByParams(Map<String, Object> params) throws Exception {
        return auditLogMapper.countByParams(params);
    }

    public java.util.List<AuditLog> selectAllByParams(Map<String, Object> params) throws Exception {
        return auditLogMapper.selectAllByParams(params);
    }
}
