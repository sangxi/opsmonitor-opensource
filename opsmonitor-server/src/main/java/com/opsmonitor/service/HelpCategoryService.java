package com.opsmonitor.service;

import com.opsmonitor.entity.HelpCategory;
import com.opsmonitor.mapper.HelpCategoryMapper;
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
 * @ClassName:HelpCategoryService.java
 * @author: OpsMonitor Team
 * @Description: 教程分类
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class HelpCategoryService {

    private static final Logger logger = LoggerFactory.getLogger(HelpCategoryService.class);

    @Autowired
    private HelpCategoryMapper helpCategoryMapper;

    public List<HelpCategory> selectAllByParams(Map<String, Object> params) {
        return helpCategoryMapper.selectAllByParams(params);
    }

    public HelpCategory selectById(String id) {
        return helpCategoryMapper.selectById(id);
    }

    public int save(HelpCategory helpCategory) {
        if (helpCategory.getId() == null || helpCategory.getId().trim().length() == 0) {
            helpCategory.setId(UUIDUtil.getUUID());
        }
        return helpCategoryMapper.save(helpCategory);
    }

    public int updateById(HelpCategory helpCategory) {
        return helpCategoryMapper.updateById(helpCategory);
    }

    public int deleteById(String[] id) {
        return helpCategoryMapper.deleteById(id);
    }

    public List<HelpCategory> selectAll() {
        return helpCategoryMapper.selectAllByParams(new HashMap<String, Object>());
    }
}
