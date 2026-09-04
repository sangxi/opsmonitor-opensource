package com.opsmonitor.controller;

import com.opsmonitor.entity.SystemInfo;
import com.opsmonitor.service.LogInfoService;
import com.opsmonitor.service.SystemInfoService;
import com.opsmonitor.util.staticvar.StaticKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:HostInfoController.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: 主机备注/分组/标签信息
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Controller
@RequestMapping("/host")
public class HostInfoController {


    private static final Logger logger = LoggerFactory.getLogger(HostInfoController.class);


    @Resource
    private SystemInfoService systemInfoService;
    @Resource
    private LogInfoService logInfoService;


    /**
     * 保存主机备注/分组/标签信息
     *
     * @param SystemInfo
     * @param model
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "save")
    public String saveHostInfo(SystemInfo SystemInfo, Model model, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(SystemInfo.getId())) {
                systemInfoService.save(SystemInfo);
            } else {
                SystemInfo ho = systemInfoService.selectById(SystemInfo.getId());
                if (ho != null) {
                    if (SystemInfo.getRemark() != null) {
                        ho.setRemark(SystemInfo.getRemark());
                    }
                    if (SystemInfo.getGroupName() != null) {
                        ho.setGroupName(SystemInfo.getGroupName());
                    }
                    if (SystemInfo.getTags() != null) {
                        ho.setTags(SystemInfo.getTags());
                    }
                    systemInfoService.updateById(ho);
                }
            }

        } catch (Exception e) {
            logger.error("保存主机备注信息错误：", e);
            logInfoService.save(SystemInfo.getHostname(), "保存主机备注信息错误：" + e.toString(), StaticKeys.LOG_ERROR);
        }
        return "success";
    }


    /**
     * 新增主机（手动注册）
     *
     * @param systemInfo
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "add")
    public String addHostInfo(SystemInfo systemInfo, HttpServletRequest request) {
        String hostname = "";
        try {
            hostname = systemInfo.getHostname();
            if (StringUtils.isEmpty(hostname)) {
                return "error：主机IP/名称不能为空";
            }
            hostname = hostname.trim();
            // 校验是否已存在同名主机
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("hostname", hostname);
            List<SystemInfo> existList = systemInfoService.selectAllByParams(params);
            if (existList != null && !existList.isEmpty()) {
                return "error：主机 [" + hostname + "] 已存在，请勿重复添加";
            }
            systemInfo.setHostname(hostname);
            if (StringUtils.isEmpty(systemInfo.getState())) {
                systemInfo.setState("1");
            }
            systemInfoService.save(systemInfo);
            logInfoService.save(hostname, "手动新增主机：" + hostname, "0");
            return "success";
        } catch (Exception e) {
            logger.error("新增主机错误：", e);
            logInfoService.save(hostname, "新增主机错误：" + e.toString(), StaticKeys.LOG_ERROR);
            return "error：" + e.toString();
        }
    }


    /**
     * 主机分组列表（去重，供筛选下拉）
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "groups")
    public List<String> groupList() {
        try {
            return systemInfoService.selectGroups();
        } catch (Exception e) {
            logger.error("查询主机分组列表错误：", e);
            return new java.util.ArrayList<String>();
        }
    }

}
