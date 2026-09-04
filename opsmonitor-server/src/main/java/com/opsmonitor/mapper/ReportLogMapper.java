package com.opsmonitor.mapper;

import com.opsmonitor.entity.ReportLog;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @ClassName:ReportLogMapper.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 定时报表记录 Mapper
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Repository
public interface ReportLogMapper {

    ReportLog selectById(String id) throws Exception;

    List<ReportLog> selectAllByParams(Map<String, Object> params) throws Exception;

    List<ReportLog> selectByParams(Map<String, Object> params) throws Exception;

    int save(ReportLog reportLog) throws Exception;

    int updateById(ReportLog reportLog) throws Exception;

    int deleteById(String[] id) throws Exception;

    int countByParams(Map<String, Object> params) throws Exception;
}
