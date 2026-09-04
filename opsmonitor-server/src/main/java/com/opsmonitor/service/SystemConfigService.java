package com.opsmonitor.service;

import com.opsmonitor.entity.SystemConfig;
import com.opsmonitor.mapper.SystemConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置 Service
 *
 * <p>说明：GlobalModelAdvice 有 3 个 @ModelAttribute（systemName / systemShortName /
 * copyrightInfo），会在每个 Controller 请求前各读一次配置。这些值几乎不变，
 * 故这里加一层内存缓存（5 分钟），避免每个页面请求都查 3 次库；
 * 且查库失败时可用旧缓存兜底，避免数据库抖动把页面拖到连接超时。</p>
 */
@Service
public class SystemConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigService.class);

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    /**
     * 配置值缓存
     */
    private static final Map<String, String> VALUE_CACHE = new ConcurrentHashMap<String, String>();

    /**
     * 缓存写入时间戳
     */
    private static final Map<String, Long> CACHE_TIME = new ConcurrentHashMap<String, Long>();

    /**
     * 缓存有效期：5 分钟
     */
    private static final long CACHE_TTL = 5 * 60 * 1000L;

    /**
     * 获取所有配置
     */
    public List<SystemConfig> selectAll() throws Exception {
        return systemConfigMapper.selectAll();
    }

    /**
     * 按配置键名获取配置值
     */
    public SystemConfig getByConfigKey(String configKey) throws Exception {
        return systemConfigMapper.selectByConfigKey(configKey);
    }

    /**
     * 按键名获取配置值（带默认值）+ 内存缓存
     * 缓存命中直接返回；未命中或过期才查库；查库失败且有旧缓存时用旧值兜底。
     */
    public String getValue(String configKey, String defaultValue) {
        long now = System.currentTimeMillis();
        String cached = VALUE_CACHE.get(configKey);
        Long cachedAt = CACHE_TIME.get(configKey);
        if (cached != null && cachedAt != null && (now - cachedAt) < CACHE_TTL) {
            return cached;
        }
        try {
            SystemConfig config = systemConfigMapper.selectByConfigKey(configKey);
            if (config != null && config.getConfigValue() != null) {
                VALUE_CACHE.put(configKey, config.getConfigValue());
                CACHE_TIME.put(configKey, now);
                return config.getConfigValue();
            }
        } catch (Exception e) {
            logger.warn("读取系统配置[{}]失败，使用缓存/默认值兜底：{}", configKey, e.toString());
            if (cached != null) {
                return cached;
            }
        }
        return defaultValue;
    }

    /**
     * 更新配置（同时清除该键的缓存，保证后台修改后立即生效）
     */
    public int updateById(SystemConfig config) throws Exception {
        int rows = systemConfigMapper.updateById(config);
        try {
            if (config != null && config.getConfigKey() != null) {
                VALUE_CACHE.remove(config.getConfigKey());
                CACHE_TIME.remove(config.getConfigKey());
            }
        } catch (Exception e) {
            logger.warn("清除系统配置缓存失败：", e);
        }
        return rows;
    }
}
