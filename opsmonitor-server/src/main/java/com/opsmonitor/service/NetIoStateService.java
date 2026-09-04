package com.opsmonitor.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.NetIoState;
import com.opsmonitor.mapper.NetIoStateMapper;
import com.opsmonitor.util.DateUtil;
import com.opsmonitor.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:NetIoStateService.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: NetIoStateService.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class NetIoStateService {

    @Autowired
    private NetIoStateMapper netIoStateMapper;

    public PageInfo selectByParams(Map<String, Object> params, int currPage, int pageSize) throws Exception {
        PageHelper.startPage(currPage, pageSize);
        List<NetIoState> list = netIoStateMapper.selectByParams(params);
        PageInfo<NetIoState> pageInfo = new PageInfo<NetIoState>(list);
        return pageInfo;
    }

    public void save(NetIoState NetIoState) throws Exception {
        NetIoState.setId(UUIDUtil.getUUID());
        NetIoState.setCreateTime(DateUtil.getNowTime());
        NetIoState.setDateStr(DateUtil.getDateTimeString(NetIoState.getCreateTime()));
        netIoStateMapper.save(NetIoState);
    }

    public void saveRecord(List<NetIoState> recordList) throws Exception {
        if (recordList.size() < 1) {
            return;
        }
        for (NetIoState as : recordList) {
            as.setId(UUIDUtil.getUUID());
            as.setDateStr(DateUtil.getDateTimeString(as.getCreateTime()));
        }
        netIoStateMapper.insertList(recordList);
    }

    public int deleteById(String[] id) throws Exception {
        return netIoStateMapper.deleteById(id);
    }

    public NetIoState selectById(String id) throws Exception {
        return netIoStateMapper.selectById(id);
    }

    public List<NetIoState> selectAllByParams(Map<String, Object> params) throws Exception {
        return netIoStateMapper.selectAllByParams(params);
    }


}
