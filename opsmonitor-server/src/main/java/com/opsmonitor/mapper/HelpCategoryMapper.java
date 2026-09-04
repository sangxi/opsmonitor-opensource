package com.opsmonitor.mapper;

import com.opsmonitor.entity.HelpCategory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @version v2.0
 * @ClassName:HelpCategoryMapper.java
 * @author: OpsMonitor Team
 * @Description: 教程分类
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Repository
public interface HelpCategoryMapper {

    public List<HelpCategory> selectAllByParams(Map<String, Object> params);

    public HelpCategory selectById(String id);

    public int save(HelpCategory helpCategory);

    public int updateById(HelpCategory helpCategory);

    public int deleteById(String[] id);
}
