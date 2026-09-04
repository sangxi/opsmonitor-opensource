package com.opsmonitor.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.DbTableCount;
import com.opsmonitor.mapper.DbTableCountMapper;
import com.opsmonitor.util.DateUtil;
import com.opsmonitor.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:DbTableCountCountService.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: DbTableCountService.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class DbTableCountService {

    @Autowired
    private DbTableCountMapper dbTableCountMapper;

    public PageInfo selectByParams(Map<String, Object> params, int currPage, int pageSize) throws Exception {
        PageHelper.startPage(currPage, pageSize);
        List<DbTableCount> list = dbTableCountMapper.selectByParams(params);
        PageInfo<DbTableCount> pageInfo = new PageInfo<DbTableCount>(list);
        return pageInfo;
    }

    public void save(DbTableCount DbTableCount) throws Exception {
        DbTableCount.setId(UUIDUtil.getUUID());
        DbTableCount.setCreateTime(DateUtil.getNowTime());
        dbTableCountMapper.save(DbTableCount);
    }

    public void saveRecord(List<DbTableCount> recordList) throws Exception {
        if (recordList.size() < 1) {
            return;
        }
        for (DbTableCount as : recordList) {
            as.setId(UUIDUtil.getUUID());
            as.setDateStr(DateUtil.getDateTimeString(as.getCreateTime()));
        }
        dbTableCountMapper.insertList(recordList);
    }


    public int countByParams(Map<String, Object> params) throws Exception {
        return dbTableCountMapper.countByParams(params);
    }

    @Transactional
    public int deleteById(String[] id) throws Exception {
        return dbTableCountMapper.deleteById(id);
    }

    public void updateById(DbTableCount DbTableCount)
            throws Exception {
        dbTableCountMapper.updateById(DbTableCount);
    }

    public DbTableCount selectById(String id) throws Exception {
        return dbTableCountMapper.selectById(id);
    }

    public List<DbTableCount> selectAllByParams(Map<String, Object> params) throws Exception {
        return dbTableCountMapper.selectAllByParams(params);
    }

    public int deleteByDate(Map<String, Object> map) throws Exception {
        return dbTableCountMapper.deleteByDate(map);
    }


}
