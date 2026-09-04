package com.opsmonitor.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.ReportLog;
import com.opsmonitor.mapper.ReportLogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @ClassName:ReportLogService.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 定时报表记录 Service
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class ReportLogService {

    @Resource
    private ReportLogMapper reportLogMapper;

    public PageInfo selectByParams(Map<String, Object> params, int currPage, int pageSize) throws Exception {
        PageHelper.startPage(currPage, pageSize);
        List<ReportLog> list = reportLogMapper.selectByParams(params);
        return new PageInfo(list);
    }

    public List<ReportLog> selectAllByParams(Map<String, Object> params) throws Exception {
        return reportLogMapper.selectAllByParams(params);
    }

    public ReportLog selectById(String id) throws Exception {
        return reportLogMapper.selectById(id);
    }

    public void save(ReportLog reportLog) throws Exception {
        reportLogMapper.save(reportLog);
    }

    public void updateById(ReportLog reportLog) throws Exception {
        reportLogMapper.updateById(reportLog);
    }

    public int deleteById(String[] id) throws Exception {
        return reportLogMapper.deleteById(id);
    }

    public int countByParams(Map<String, Object> params) throws Exception {
        return reportLogMapper.countByParams(params);
    }
}
