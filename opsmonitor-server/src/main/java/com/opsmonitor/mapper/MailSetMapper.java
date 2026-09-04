package com.opsmonitor.mapper;

import com.opsmonitor.entity.MailSet;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:MailSetMapper.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: 查看磁盘IO使用情况
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Repository
public interface MailSetMapper {


    public List<MailSet> selectAllByParams(Map<String, Object> map) throws Exception;

    public void save(MailSet MailSet) throws Exception;

    public int deleteById(String[] id) throws Exception;

    public int updateById(MailSet MailSet) throws Exception;


}
