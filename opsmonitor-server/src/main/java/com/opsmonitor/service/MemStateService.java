package com.opsmonitor.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.MemState;
import com.opsmonitor.mapper.MemStateMapper;
import com.opsmonitor.util.DateUtil;
import com.opsmonitor.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:MemStateService.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: MemStateService.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class MemStateService {

    @Autowired
    private MemStateMapper memStateMapper;

    public PageInfo selectByParams(Map<String, Object> params, int currPage, int pageSize) throws Exception {
        PageHelper.startPage(currPage, pageSize);
        List<MemState> list = memStateMapper.selectByParams(params);
        PageInfo<MemState> pageInfo = new PageInfo<MemState>(list);
        return pageInfo;
    }

    public void save(MemState MemState) throws Exception {
        MemState.setId(UUIDUtil.getUUID());
        MemState.setCreateTime(DateUtil.getNowTime());
        MemState.setDateStr(DateUtil.getDateTimeString(MemState.getCreateTime()));
        memStateMapper.save(MemState);
    }

    public void saveRecord(List<MemState> recordList) throws Exception {
        if (recordList.size() < 1) {
            return;
        }
        for (MemState as : recordList) {
            as.setId(UUIDUtil.getUUID());
            as.setDateStr(DateUtil.getDateTimeString(as.getCreateTime()));
        }
        memStateMapper.insertList(recordList);
    }

    public int deleteById(String[] id) throws Exception {
        return memStateMapper.deleteById(id);
    }

    public MemState selectById(String id) throws Exception {
        return memStateMapper.selectById(id);
    }

    public List<MemState> selectAllByParams(Map<String, Object> params) throws Exception {
        return memStateMapper.selectAllByParams(params);
    }


}
