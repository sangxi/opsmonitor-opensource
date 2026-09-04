package com.opsmonitor.mapper;

import com.opsmonitor.entity.AuditLog;

import java.util.List;
import java.util.Map;

/**
 * @ClassName:AuditLogMapper.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 操作审计日志 Mapper
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
public interface AuditLogMapper {

    List<AuditLog> selectAllByParams(Map<String, Object> params) throws Exception;

    List<AuditLog> selectByParams(Map<String, Object> params) throws Exception;

    AuditLog selectById(String id) throws Exception;

    int save(AuditLog auditLog) throws Exception;

    int deleteById(String[] id) throws Exception;

    int countByParams(Map<String, Object> params) throws Exception;
}
