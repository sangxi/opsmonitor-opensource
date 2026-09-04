package com.opsmonitor.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.AlarmRecord;
import com.opsmonitor.mapper.AlarmRecordMapper;
import com.opsmonitor.util.DateUtil;
import com.opsmonitor.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @ClassName:AlarmRecordService.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 告警中心记录 Service
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class AlarmRecordService {

    private static final Logger logger = LoggerFactory.getLogger(AlarmRecordService.class);

    @Autowired
    private AlarmRecordMapper alarmRecordMapper;

    public PageInfo selectByParams(Map<String, Object> params, int currPage, int pageSize) throws Exception {
        PageHelper.startPage(currPage, pageSize);
        List<AlarmRecord> list = alarmRecordMapper.selectByParams(params);
        PageInfo<AlarmRecord> pageInfo = new PageInfo<AlarmRecord>(list);
        return pageInfo;
    }

    /**
     * 记录一条告警。带简单去重：相同 type+hostname+title 且未处理的不重复插入
     */
    public void save(AlarmRecord record) {
        try {
            if (record.getCreateTime() == null) {
                record.setCreateTime(DateUtil.getNowTime());
            }
            if (record.getId() == null) {
                record.setId(UUIDUtil.getUUID());
            }
            if (record.getState() == null) {
                record.setState("0");
            }
            if (record.getLevel() == null) {
                record.setLevel("warn");
            }
            // 去重检查：相同类型+主机+标题且未处理的告警，不重复记录
            Map<String, Object> params = new java.util.HashMap<String, Object>();
            params.put("alarmType", record.getAlarmType());
            params.put("hostname", record.getHostname());
            params.put("title", record.getTitle());
            params.put("state", "0");
            int exist = alarmRecordMapper.countByParams(params);
            if (exist > 0) {
                return;
            }
            alarmRecordMapper.save(record);
        } catch (Exception e) {
            logger.error("保存告警记录异常：", e);
        }
    }

    /**
     * 快捷记录
     */
    public void save(String alarmType, String hostname, String title, String content, String level) {
        AlarmRecord record = new AlarmRecord();
        record.setAlarmType(alarmType);
        record.setHostname(hostname);
        record.setTitle(title);
        record.setContent(content);
        record.setLevel(level);
        save(record);
    }

    public void updateState(String id, String state) {
        try {
            AlarmRecord record = new AlarmRecord();
            record.setId(id);
            record.setState(state);
            alarmRecordMapper.updateState(record);
        } catch (Exception e) {
            logger.error("更新告警状态异常：", e);
        }
    }

    public int deleteById(String[] id) throws Exception {
        return alarmRecordMapper.deleteById(id);
    }

    public int countByParams(Map<String, Object> params) throws Exception {
        return alarmRecordMapper.countByParams(params);
    }

    public AlarmRecord selectById(String id) throws Exception {
        return alarmRecordMapper.selectById(id);
    }

    /**
     * 各状态数量统计，返回 [{STATE:x, CNT:n}]
     */
    public List<Map<String, Object>> countGroupByState() throws Exception {
        return alarmRecordMapper.countGroupByState();
    }

    /**
     * 未处理告警数量（顶栏/大屏角标用）
     */
    public int countUnhandled() {
        try {
            Map<String, Object> params = new java.util.HashMap<String, Object>();
            params.put("state", "0");
            return alarmRecordMapper.countByParams(params);
        } catch (Exception e) {
            logger.error("统计未处理告警异常：", e);
            return 0;
        }
    }

    public List<AlarmRecord> selectAllByParams(Map<String, Object> params) throws Exception {
        return alarmRecordMapper.selectAllByParams(params);
    }
}
