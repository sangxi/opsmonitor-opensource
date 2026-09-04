package com.opsmonitor.controller;

import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.AuditLog;
import com.opsmonitor.service.AuditLogService;
import com.opsmonitor.util.CodeUtil;
import com.opsmonitor.util.PageUtil;
import com.opsmonitor.util.staticvar.StaticKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName:AuditController.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 操作审计控制器
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Controller
@RequestMapping("/audit")
public class AuditController {

    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);

    @Resource
    private AuditLogService auditLogService;

    /**
     * 操作审计列表
     */
    @RequestMapping(value = "list")
    public String list(AuditLog auditLog, Model model, HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        try {
            StringBuffer url = new StringBuffer();
            if (!StringUtils.isEmpty(auditLog.getAction())) {
                params.put("action", auditLog.getAction().trim());
                url.append("&action=").append(auditLog.getAction().trim());
            }
            if (!StringUtils.isEmpty(auditLog.getAccount())) {
                params.put("account", auditLog.getAccount().trim());
                url.append("&account=").append(CodeUtil.escape(auditLog.getAccount().trim()));
            }
            if (!StringUtils.isEmpty(auditLog.getContent())) {
                params.put("content", CodeUtil.unescape(auditLog.getContent()).trim());
                url.append("&content=").append(CodeUtil.escape(auditLog.getContent()));
            }
            PageInfo pageInfo = auditLogService.selectByParams(params, auditLog.getPage(), auditLog.getPageSize());
            PageUtil.initPageNumber(pageInfo, model);

            model.addAttribute("pageUrl", "/audit/list?1=1" + url.toString());
            model.addAttribute("page", pageInfo);
            model.addAttribute("auditLog", auditLog);
        } catch (Exception e) {
            logger.error("查询审计日志错误", e);
            // DB 异常兜底：确保模板不因 page 缺失而渲染中断（避免空白页）
            model.addAttribute("pageUrl", "/audit/list?1=1");
            model.addAttribute("page", new PageInfo());
            model.addAttribute("auditLog", auditLog);
        }
        return "audit/list";
    }

    /**
     * 清空审计日志
     */
    @RequestMapping(value = "clear")
    public String clear(HttpServletRequest request, HttpSession session) {
        try {
            // 逐条删除（避免单条超长 SQL）
            Map<String, Object> params = new HashMap<String, Object>();
            com.github.pagehelper.PageHelper.startPage(1, 500);
            PageInfo pageInfo = auditLogService.selectByParams(params, 1, 500);
            for (Object o : pageInfo.getList()) {
                AuditLog a = (AuditLog) o;
                auditLogService.deleteById(new String[]{a.getId()});
            }
        } catch (Exception e) {
            logger.error("清空审计日志错误", e);
        }
        return "redirect:/audit/list";
    }

    /**
     * 导出审计日志（CSV）
     */
    @RequestMapping(value = "export")
    public void export(HttpServletRequest request, javax.servlet.http.HttpServletResponse response) {
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            String action = request.getParameter("action");
            if (!StringUtils.isEmpty(action)) {
                params.put("action", action.trim());
            }
            java.util.List<AuditLog> list = null;
            try {
                list = auditLogService.selectAllByParams(params);
            } catch (Exception ignore) {
            }
            if (list == null) {
                list = new java.util.ArrayList<AuditLog>();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("\uFEFF"); // BOM 防止 Excel 中文乱码
            sb.append("时间,账号,操作类型,操作内容,来源IP,结果\n");
            for (AuditLog a : list) {
                sb.append(a.getCreateTime() == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(a.getCreateTime())).append(",");
                sb.append(csv(a.getAccount())).append(",");
                sb.append(csv(a.getAction())).append(",");
                sb.append(csv(a.getContent())).append(",");
                sb.append(csv(a.getIp())).append(",");
                sb.append(csv(a.getResult())).append("\n");
            }
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=opsmonitor_audit_" + System.currentTimeMillis() + ".csv");
            response.getWriter().write(sb.toString());
        } catch (Exception e) {
            logger.error("导出审计日志错误", e);
        }
    }

    private String csv(String v) {
        if (v == null) {
            return "";
        }
        String s = v.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            return "\"" + s + "\"";
        }
        return s;
    }
}
