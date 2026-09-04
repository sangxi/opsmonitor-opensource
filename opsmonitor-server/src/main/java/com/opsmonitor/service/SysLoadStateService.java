package com.opsmonitor.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.SysLoadState;
import com.opsmonitor.mapper.SysLoadStateMapper;
import com.opsmonitor.util.DateUtil;
import com.opsmonitor.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:SysLoadStateService.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: SysLoadStateService.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class SysLoadStateService {

    @Autowired
    private SysLoadStateMapper sysLoadStateMapper;

    public PageInfo selectByParams(Map<String, Object> params, int currPage, int pageSize) throws Exception {
        PageHelper.startPage(currPage, pageSize);
        List<SysLoadState> list = sysLoadStateMapper.selectByParams(params);
        PageInfo<SysLoadState> pageInfo = new PageInfo<SysLoadState>(list);
        return pageInfo;
    }

    public void save(SysLoadState SysLoadState) throws Exception {
        SysLoadState.setId(UUIDUtil.getUUID());
        SysLoadState.setCreateTime(DateUtil.getNowTime());
        SysLoadState.setDateStr(DateUtil.getDateTimeString(SysLoadState.getCreateTime()));
        sysLoadStateMapper.save(SysLoadState);
    }

    public void saveRecord(List<SysLoadState> recordList) throws Exception {
        if (recordList.size() < 1) {
            return;
        }
        for (SysLoadState as : recordList) {
            as.setId(UUIDUtil.getUUID());
            as.setDateStr(DateUtil.getDateTimeString(as.getCreateTime()));
        }
        sysLoadStateMapper.insertList(recordList);
    }

    public int deleteById(String[] id) throws Exception {
        return sysLoadStateMapper.deleteById(id);
    }

    public SysLoadState selectById(String id) throws Exception {
        return sysLoadStateMapper.selectById(id);
    }

    public List<SysLoadState> selectAllByParams(Map<String, Object> params) throws Exception {
        return sysLoadStateMapper.selectAllByParams(params);
    }



}
