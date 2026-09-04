package com.opsmonitor.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.CpuState;
import com.opsmonitor.mapper.CpuStateMapper;
import com.opsmonitor.util.DateUtil;
import com.opsmonitor.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:CpuStateService.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: CpuStateService.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class CpuStateService {

    @Autowired
    private CpuStateMapper cpuStateMapper;

    public PageInfo selectByParams(Map<String, Object> params, int currPage, int pageSize) throws Exception {
        PageHelper.startPage(currPage, pageSize);
        List<CpuState> list = cpuStateMapper.selectByParams(params);
        PageInfo<CpuState> pageInfo = new PageInfo<CpuState>(list);
        return pageInfo;
    }

    public void save(CpuState CpuState) throws Exception {
        CpuState.setId(UUIDUtil.getUUID());
        CpuState.setCreateTime(DateUtil.getNowTime());
        CpuState.setDateStr(DateUtil.getDateTimeString(CpuState.getCreateTime()));
        cpuStateMapper.save(CpuState);
    }

    public void saveRecord(List<CpuState> recordList) throws Exception {
        if (recordList.size() < 1) {
            return;
        }
        for (CpuState as : recordList) {
            as.setId(UUIDUtil.getUUID());
            as.setDateStr(DateUtil.getDateTimeString(as.getCreateTime()));
        }
        cpuStateMapper.insertList(recordList);
    }

    public int deleteById(String[] id) throws Exception {
        return cpuStateMapper.deleteById(id);
    }

    public CpuState selectById(String id) throws Exception {
        return cpuStateMapper.selectById(id);
    }

    public List<CpuState> selectAllByParams(Map<String, Object> params) throws Exception {
        return cpuStateMapper.selectAllByParams(params);
    }



}
