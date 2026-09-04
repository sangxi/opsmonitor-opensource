package com.opsmonitor.controller;

import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.ReportLog;
import com.opsmonitor.service.AuditLogService;
import com.opsmonitor.service.ReportLogService;
import com.opsmonitor.service.ReportService;
import com.opsmonitor.service.SystemConfigService;
import com.opsmonitor.util.PageUtil;
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
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName:ReportController.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 定时报表控制器（报表记录列表/手动发送/删除/配置）
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Controller
@RequestMapping("/report")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Resource
    private ReportLogService reportLogService;
    @Resource
    private ReportService reportService;
    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private AuditLogService auditLogService;

    /**
     * 报表记录列表
     */
    @RequestMapping(value = "list")
    public String list(ReportLog reportLog, Model model, HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        try {
            StringBuffer url = new StringBuffer();
            if (!StringUtils.isEmpty(reportLog.getReportType())) {
                params.put("reportType", reportLog.getReportType().trim());
                url.append("&reportType=").append(reportLog.getReportType().trim());
            }
            if (!StringUtils.isEmpty(reportLog.getSendStatus())) {
                params.put("sendStatus", reportLog.getSendStatus().trim());
                url.append("&sendStatus=").append(reportLog.getSendStatus().trim());
            }
            PageInfo pageInfo = reportLogService.selectByParams(params, reportLog.getPage(), reportLog.getPageSize());
            PageUtil.initPageNumber(pageInfo, model);

            model.addAttribute("pageUrl", "/report/list?1=1" + url.toString());
            model.addAttribute("page", pageInfo);
            model.addAttribute("reportLog", reportLog);
            // 报表配置
            model.addAttribute("reportEnable", systemConfigService.getValue("reportEnable", "1"));
            model.addAttribute("reportMail", systemConfigService.getValue("reportMail", ""));
            model.addAttribute("reportTime", systemConfigService.getValue("reportTime", "08:30"));
        } catch (Exception e) {
            logger.error("查询报表记录错误", e);
            // DB 异常兜底：确保模板不因 page 缺失而渲染中断（避免空白页）
            model.addAttribute("pageUrl", "/report/list?1=1");
            model.addAttribute("page", new PageInfo());
            model.addAttribute("reportLog", reportLog);
            model.addAttribute("reportEnable", "1");
            model.addAttribute("reportMail", "");
            model.addAttribute("reportTime", "08:30");
        }
        return "report/list";
    }

    /**
     * 保存报表配置（开关/收件人/发送时间）
     */
    @ResponseBody
    @RequestMapping(value = "saveConfig")
    public String saveConfig(HttpServletRequest request, HttpSession session) {
        try {
            String reportEnable = request.getParameter("reportEnable");
            String reportMail = request.getParameter("reportMail");
            String reportTime = request.getParameter("reportTime");
            if (!StringUtils.isEmpty(reportEnable)) {
                com.opsmonitor.entity.SystemConfig cfg = systemConfigService.getByConfigKey("reportEnable");
                if (cfg != null) {
                    cfg.setConfigValue("1".equals(reportEnable) ? "1" : "0");
                    systemConfigService.updateById(cfg);
                }
            }
            if (reportMail != null) {
                com.opsmonitor.entity.SystemConfig cfg = systemConfigService.getByConfigKey("reportMail");
                if (cfg != null) {
                    cfg.setConfigValue(reportMail.trim());
                    systemConfigService.updateById(cfg);
                }
            }
            if (!StringUtils.isEmpty(reportTime)) {
                com.opsmonitor.entity.SystemConfig cfg = systemConfigService.getByConfigKey("reportTime");
                if (cfg != null) {
                    cfg.setConfigValue(reportTime.trim());
                    systemConfigService.updateById(cfg);
                }
            }
            // 审计
            com.opsmonitor.entity.AccountInfo account = (com.opsmonitor.entity.AccountInfo) session.getAttribute(StaticKeys.LOGIN_KEY);
            String accountName = account == null ? "unknown" : account.getAccount();
            auditLogService.save(accountName, "config", "修改定时报表配置", request.getRemoteAddr(), "success");
            return "success";
        } catch (Exception e) {
            logger.error("保存报表配置错误", e);
            return "error：" + e.toString();
        }
    }

    /**
     * 手动生成并发送报表
     */
    @ResponseBody
    @RequestMapping(value = "send")
    public String send(HttpServletRequest request, HttpSession session) {
        String reportType = request.getParameter("reportType");
        if (StringUtils.isEmpty(reportType)) {
            reportType = "manual";
        }
        String res = reportService.generateAndSend(reportType, null);
        // 审计
        com.opsmonitor.entity.AccountInfo account = (com.opsmonitor.entity.AccountInfo) session.getAttribute(StaticKeys.LOGIN_KEY);
        String accountName = account == null ? "unknown" : account.getAccount();
        auditLogService.save(accountName, "config", "手动发送报表(type=" + reportType + ")", request.getRemoteAddr(), res.startsWith("success") ? "success" : "error");
        return res;
    }

    /**
     * 查看报表正文（AJAX JSON，供预览弹窗）
     */
    @RequestMapping(value = "detail")
    @ResponseBody
    public Map<String, Object> detail(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            String id = request.getParameter("id");
            if (StringUtils.isEmpty(id)) {
                result.put("code", 1);
                return result;
            }
            ReportLog record = reportLogService.selectById(id);
            if (record == null) {
                result.put("code", 1);
                return result;
            }
            result.put("code", 0);
            result.put("title", record.getTitle());
            result.put("content", record.getContent());
            result.put("sendStatus", record.getSendStatus());
            result.put("receiver", record.getReceiver());
            result.put("createTime", record.getCreateTime() == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(record.getCreateTime()));
        } catch (Exception e) {
            logger.error("查询报表详情错误", e);
            result.put("code", 1);
        }
        return result;
    }

    /**
     * 删除报表记录
     */
    @RequestMapping(value = "del")
    public String delete(HttpServletRequest request, HttpSession session) {
        try {
            if (!StringUtils.isEmpty(request.getParameter("id"))) {
                reportLogService.deleteById(request.getParameter("id").split(","));
            }
            com.opsmonitor.entity.AccountInfo account = (com.opsmonitor.entity.AccountInfo) session.getAttribute(StaticKeys.LOGIN_KEY);
            String accountName = account == null ? "unknown" : account.getAccount();
            auditLogService.save(accountName, "config", "删除报表记录", request.getRemoteAddr(), "success");
        } catch (Exception e) {
            logger.error("删除报表记录错误", e);
        }
        return "redirect:/report/list";
    }
}
