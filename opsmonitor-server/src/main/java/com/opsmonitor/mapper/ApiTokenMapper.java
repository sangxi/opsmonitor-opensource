package com.opsmonitor.mapper;

import com.opsmonitor.entity.ApiToken;

import java.util.List;
import java.util.Map;

/**
 * @ClassName:ApiTokenMapper.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 开放 API 令牌 Mapper
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
public interface ApiTokenMapper {

    List<ApiToken> selectAllByParams(Map<String, Object> params) throws Exception;

    List<ApiToken> selectByParams(Map<String, Object> params) throws Exception;

    ApiToken selectById(String id) throws Exception;

    ApiToken selectByToken(String token) throws Exception;

    int save(ApiToken apiToken) throws Exception;

    int updateById(ApiToken apiToken) throws Exception;

    int deleteById(String[] id) throws Exception;

    int countByParams(Map<String, Object> params) throws Exception;
}
