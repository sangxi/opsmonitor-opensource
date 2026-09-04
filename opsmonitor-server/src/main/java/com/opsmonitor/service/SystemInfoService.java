package com.opsmonitor.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.SystemInfo;
import com.opsmonitor.mapper.SystemInfoMapper;
import com.opsmonitor.util.DateUtil;
import com.opsmonitor.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:SystemInfoService.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: SystemInfoService.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class SystemInfoService {

    @Autowired
    private SystemInfoMapper systemInfoMapper;

    public PageInfo selectByParams(Map<String, Object> params, int currPage, int pageSize) throws Exception {
        PageHelper.startPage(currPage, pageSize);
        List<SystemInfo> list = systemInfoMapper.selectByParams(params);
        PageInfo<SystemInfo> pageInfo = new PageInfo<SystemInfo>(list);
        return pageInfo;
    }

    public void save(SystemInfo SystemInfo) throws Exception {
        SystemInfo.setId(UUIDUtil.getUUID());
        SystemInfo.setCreateTime(DateUtil.getNowTime());
        systemInfoMapper.save(SystemInfo);
    }

    @Transactional
    public void saveRecord(List<SystemInfo> recordList) throws Exception {
        if (recordList.size() < 1) {
            return;
        }
        for (SystemInfo as : recordList) {
            as.setId(UUIDUtil.getUUID());
            as.setCreateTime(DateUtil.getNowTime());
        }
        systemInfoMapper.insertList(recordList);
    }

    @Transactional
    public void updateRecord(List<SystemInfo> recordList) throws Exception {
        if (recordList.size() < 1) {
            return;
        }
        systemInfoMapper.updateList(recordList);
    }

    @Transactional
    public void updateById(SystemInfo SystemInfo) throws Exception {
        systemInfoMapper.updateById(SystemInfo);
    }

    public int deleteById(String[] id) throws Exception {
        return systemInfoMapper.deleteById(id);
    }

    public SystemInfo selectById(String id) throws Exception {
        return systemInfoMapper.selectById(id);
    }

    public List<SystemInfo> selectAllByParams(Map<String, Object> params) throws Exception {
        return systemInfoMapper.selectAllByParams(params);
    }

    public int countByParams(Map<String, Object> params) throws Exception {
        return systemInfoMapper.countByParams(params);
    }

    public List<SystemInfo> selectByAccountId(String accountId) throws Exception {
        return systemInfoMapper.selectByAccountId(accountId);
    }

    public int deleteByAccHname(Map<String, Object> params) throws Exception {
        return systemInfoMapper.deleteByAccHname(params);
    }




    public List<String> selectGroups() throws Exception {
        return systemInfoMapper.selectGroups();
    }
}
