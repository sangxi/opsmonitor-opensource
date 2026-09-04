package com.opsmonitor.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.ApiToken;
import com.opsmonitor.mapper.ApiTokenMapper;
import com.opsmonitor.util.DateUtil;
import com.opsmonitor.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @ClassName:ApiTokenService.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 开放 API 令牌 Service
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Service
public class ApiTokenService {

    private static final Logger logger = LoggerFactory.getLogger(ApiTokenService.class);

    @Autowired
    private ApiTokenMapper apiTokenMapper;

    public PageInfo selectByParams(Map<String, Object> params, int currPage, int pageSize) throws Exception {
        PageHelper.startPage(currPage, pageSize);
        List<ApiToken> list = apiTokenMapper.selectByParams(params);
        PageInfo<ApiToken> pageInfo = new PageInfo<ApiToken>(list);
        return pageInfo;
    }

    public void save(ApiToken token) {
        try {
            if (token.getCreateTime() == null) {
                token.setCreateTime(DateUtil.getNowTime());
            }
            if (token.getId() == null) {
                token.setId(UUIDUtil.getUUID());
            }
            if (token.getStatus() == null) {
                token.setStatus("1");
            }
            apiTokenMapper.save(token);
        } catch (Exception e) {
            logger.error("保存 API 令牌异常：", e);
        }
    }

    public void updateById(ApiToken token) {
        try {
            apiTokenMapper.updateById(token);
        } catch (Exception e) {
            logger.error("更新 API 令牌异常：", e);
        }
    }

    public int deleteById(String[] id) throws Exception {
        return apiTokenMapper.deleteById(id);
    }

    public ApiToken selectById(String id) throws Exception {
        return apiTokenMapper.selectById(id);
    }

    /**
     * 校验令牌是否有效（启用 + 未过期）
     */
    public boolean valid(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }
            ApiToken apiToken = apiTokenMapper.selectByToken(token.trim());
            if (apiToken == null) {
                return false;
            }
            // 过期校验
            Date expire = apiToken.getExpireTime();
            if (expire != null && expire.before(new Date())) {
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("校验 API 令牌异常：", e);
            return false;
        }
    }

    public List<ApiToken> selectAllByParams(Map<String, Object> params) throws Exception {
        return apiTokenMapper.selectAllByParams(params);
    }
}
