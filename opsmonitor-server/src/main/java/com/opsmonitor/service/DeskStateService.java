package com.opsmonitor.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.DeskState;
import com.opsmonitor.mapper.DeskStateMapper;
import com.opsmonitor.util.DateUtil;
import com.opsmonitor.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:DeskStateService.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: DeskStateService.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class DeskStateService {

    @Autowired
    private DeskStateMapper deskStateMapper;

    public PageInfo selectByParams(Map<String, Object> params, int currPage, int pageSize) throws Exception {
        PageHelper.startPage(currPage, pageSize);
        List<DeskState> list = deskStateMapper.selectByParams(params);
        PageInfo<DeskState> pageInfo = new PageInfo<DeskState>(list);
        return pageInfo;
    }

    public void save(DeskState DeskState) throws Exception {
        DeskState.setId(UUIDUtil.getUUID());
        DeskState.setCreateTime(DateUtil.getNowTime());
        DeskState.setDateStr(DateUtil.getDateTimeString(DeskState.getCreateTime()));
        deskStateMapper.save(DeskState);
    }

    @Transactional
    public void saveRecord(List<DeskState> recordList) throws Exception {
        if (recordList.size() < 1) {
            return;
        }
        for (DeskState as : recordList) {
            as.setId(UUIDUtil.getUUID());
            as.setDateStr(DateUtil.getDateTimeString(as.getCreateTime()));
        }
        deskStateMapper.insertList(recordList);
    }

    public int deleteById(String[] id) throws Exception {
        return deskStateMapper.deleteById(id);
    }

    public DeskState selectById(String id) throws Exception {
        return deskStateMapper.selectById(id);
    }

    public List<DeskState> selectAllByParams(Map<String, Object> params) throws Exception {
        return deskStateMapper.selectAllByParams(params);
    }

    public int deleteByAccHname(Map<String, Object> params) throws Exception {
        return deskStateMapper.deleteByAccHname(params);
    }



}
