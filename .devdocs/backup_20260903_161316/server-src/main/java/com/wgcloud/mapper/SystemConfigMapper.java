package com.wgcloud.mapper;

import com.wgcloud.entity.SystemConfig;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统配置 Mapper
 */
@Repository
public interface SystemConfigMapper {

    public List<SystemConfig> selectAll() throws Exception;

    public SystemConfig selectByConfigKey(String configKey) throws Exception;

    public int updateById(SystemConfig config) throws Exception;
}
