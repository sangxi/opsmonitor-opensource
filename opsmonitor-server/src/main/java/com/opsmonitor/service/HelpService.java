package com.opsmonitor.service;

import com.opsmonitor.entity.Help;
import com.opsmonitor.mapper.HelpMapper;
import com.opsmonitor.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version v2.0
 * @ClassName:HelpService.java
 * @author: OpsMonitor Team
 * @Description: 使用教程
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class HelpService {

    private static final Logger logger = LoggerFactory.getLogger(HelpService.class);

    @Autowired
    private HelpMapper helpMapper;

    public List<Help> selectAllByParams(Map<String, Object> params) {
        return helpMapper.selectAllByParams(params);
    }

    public Help selectById(String id) {
        return helpMapper.selectById(id);
    }

    public int save(Help help) {
        if (help.getId() == null || help.getId().trim().length() == 0) {
            help.setId(UUIDUtil.getUUID());
        }
        return helpMapper.save(help);
    }

    public int updateById(Help help) {
        return helpMapper.updateById(help);
    }

    public int deleteById(String[] id) {
        return helpMapper.deleteById(id);
    }

    public List<Help> selectByCategory(String categoryId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("categoryId", categoryId);
        return helpMapper.selectAllByParams(params);
    }
}
