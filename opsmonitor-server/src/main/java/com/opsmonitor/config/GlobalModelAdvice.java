package com.opsmonitor.config;

import com.opsmonitor.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 全局模板变量注入：把数据库里的系统名称、版权等信息注入到所有 Thymeleaf 模板
 * 模板里用 ${systemName} ${systemShortName} ${copyrightInfo} 读取
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired
    private SystemConfigService systemConfigService;

    @ModelAttribute("systemName")
    public String getSystemName() {
        return systemConfigService.getValue("systemName", "OpsMonitor运维监控系统");
    }

    @ModelAttribute("systemShortName")
    public String getSystemShortName() {
        return systemConfigService.getValue("systemShortName", "OpsMonitor");
    }

    @ModelAttribute("copyrightInfo")
    public String getCopyright() {
        return systemConfigService.getValue("copyright", "©2026 OpsMonitor. All Rights Reserved.");
    }
}
