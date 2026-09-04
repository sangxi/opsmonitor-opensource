package com.opsmonitor.mapper;

import com.opsmonitor.entity.Help;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @version v2.0
 * @ClassName:HelpMapper.java
 * @author: OpsMonitor Team
 * @Description: 教程内容
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Repository
public interface HelpMapper {

    public List<Help> selectAllByParams(Map<String, Object> params);

    public Help selectById(String id);

    public int save(Help help);

    public int updateById(Help help);

    public int deleteById(String[] id);
}
