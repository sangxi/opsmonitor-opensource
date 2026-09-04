package com.wgcloud.controller;

import com.wgcloud.config.MailConfig;
import com.wgcloud.entity.SystemConfig;
import com.wgcloud.service.SystemConfigService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 系统配置 Controller（后台自定义名称等）
 */
@Controller
@RequestMapping("/systemConfig")
public class SystemConfigController {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigController.class);

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private MailConfig mailConfig;

    /**
     * 配置列表页
     */
    @RequestMapping("/list")
    public String list(Model model, HttpSession session) {
        session.setAttribute("menuActive", "61");
        try {
            List<SystemConfig> configs = systemConfigService.selectAll();
            model.addAttribute("configs", configs);
        } catch (Exception e) {
            logger.error("查询系统配置失败", e);
        }
        return "systemConfig/list";
    }

    /**
     * 保存配置
     */
    @RequestMapping("/save")
    public String save(@RequestParam("id") String[] ids,
                       @RequestParam("configValue") String[] configValues) {
        try {
            for (int i = 0; i < ids.length; i++) {
                SystemConfig config = new SystemConfig();
                config.setId(ids[i]);
                config.setConfigValue(configValues[i]);
                systemConfigService.updateById(config);
            }
            logger.info("系统配置已更新，更新条数：{}", ids.length);
            // 重新加载告警阈值到 MailConfig
            reloadThresholds();
        } catch (Exception e) {
            logger.error("保存系统配置失败", e);
        }
        return "redirect:/systemConfig/list";
    }

    /**
     * 从 SYSTEM_CONFIG 重新加载 CPU/内存告警阈值到 MailConfig
     */
    private void reloadThresholds() {
        try {
            String cpuVal = systemConfigService.getValue("cpuWarnVal", null);
            if (!StringUtils.isEmpty(cpuVal)) {
                mailConfig.setCpuWarnVal(Double.parseDouble(cpuVal.trim()));
            }
            String memVal = systemConfigService.getValue("memWarnVal", null);
            if (!StringUtils.isEmpty(memVal)) {
                mailConfig.setMemWarnVal(Double.parseDouble(memVal.trim()));
            }
            logger.info("告警阈值已重新加载：CPU={}%, 内存={}%", mailConfig.getCpuWarnVal(), mailConfig.getMemWarnVal());
        } catch (Exception e) {
            logger.error("重新加载告警阈值失败：", e);
        }
    }
}
