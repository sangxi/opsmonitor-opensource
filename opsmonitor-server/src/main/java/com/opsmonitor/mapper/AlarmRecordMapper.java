package com.opsmonitor.mapper;

import com.opsmonitor.entity.AlarmRecord;

import java.util.List;
import java.util.Map;

/**
 * @ClassName:AlarmRecordMapper.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 告警中心记录 Mapper
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
public interface AlarmRecordMapper {

    List<AlarmRecord> selectAllByParams(Map<String, Object> params) throws Exception;

    List<AlarmRecord> selectByParams(Map<String, Object> params) throws Exception;

    AlarmRecord selectById(String id) throws Exception;

    int save(AlarmRecord alarmRecord) throws Exception;

    int updateById(AlarmRecord alarmRecord) throws Exception;

    int updateState(AlarmRecord alarmRecord) throws Exception;

    int deleteById(String[] id) throws Exception;

    int countByParams(Map<String, Object> params) throws Exception;

    /**
     * 统计各类状态数量（用于告警中心概览）
     */
    List<Map<String, Object>> countGroupByState() throws Exception;
}
