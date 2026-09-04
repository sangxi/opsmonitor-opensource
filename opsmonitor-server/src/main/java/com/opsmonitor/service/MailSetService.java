package com.opsmonitor.service;

import com.opsmonitor.entity.MailSet;
import com.opsmonitor.mapper.MailSetMapper;
import com.opsmonitor.util.DateUtil;
import com.opsmonitor.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:DiskIoStateService.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: DiskIoStateService.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class MailSetService {

    @Autowired
    private MailSetMapper mailSetMapper;

    public void save(MailSet MailSet) throws Exception {
        MailSet.setId(UUIDUtil.getUUID());
        MailSet.setCreateTime(DateUtil.getNowTime());
        MailSet.setFromMailName(MailSet.getFromMailName().trim());
        MailSet.setFromPwd(MailSet.getFromPwd().trim());
        MailSet.setToMail(MailSet.getToMail().trim());
        MailSet.setSmtpHost(MailSet.getSmtpHost().trim());
        mailSetMapper.save(MailSet);
    }


    public int deleteById(String[] id) throws Exception {
        return mailSetMapper.deleteById(id);
    }

    public List<MailSet> selectAllByParams(Map<String, Object> params) throws Exception {
        return mailSetMapper.selectAllByParams(params);
    }

    public int updateById(MailSet MailSet) throws Exception {
        return mailSetMapper.updateById(MailSet);
    }



}
