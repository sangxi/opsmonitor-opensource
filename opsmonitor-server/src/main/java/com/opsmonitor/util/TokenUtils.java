package com.opsmonitor.util;

import cn.hutool.json.JSONObject;
import com.opsmonitor.config.CommonConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version v2.3
 * @ClassName:TokenUtils.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: Md5加密处理
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Component
public class TokenUtils {
    private static final Logger logger = LoggerFactory.getLogger(TokenUtils.class);

    @Autowired
    private CommonConfig commonConfig;

    /**
     * 验证agent的token和server的token是否一致
     *
     * @param agentJsonObject
     * @return
     */
    public boolean checkAgentToken(JSONObject agentJsonObject) {
        if (null == agentJsonObject) {
            return false;
        }
        String omToken = MD5Utils.GetMD5Code(commonConfig.getOmToken());
        String agentOmToken = agentJsonObject.getStr("omToken");
        if (StringUtils.isEmpty(agentOmToken)) {
            return false;
        }
        if (omToken.equals(agentOmToken)) {
            return true;
        }
        return false;

    }

}
