package com.opsmonitor.controller;

import com.opsmonitor.entity.AlarmRecord;
import com.opsmonitor.entity.SystemInfo;
import com.opsmonitor.service.AlarmRecordService;
import com.opsmonitor.service.AuditLogService;
import com.opsmonitor.service.SystemInfoService;
import com.opsmonitor.util.staticvar.StaticKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName:ExportController.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 数据导出控制器（CSV，UTF-8 BOM 兼容 Excel）
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Controller
@RequestMapping("/export")
public class ExportController {

    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    @Resource
    private SystemInfoService systemInfoService;
    @Resource
    private AlarmRecordService alarmRecordService;
    @Resource
    private AuditLogService auditLogService;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 主机清单导出
     */
    @RequestMapping("hosts")
    public void exportHosts(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            String hostname = request.getParameter("hostname");
            if (!StringUtils.isEmpty(hostname)) {
                params.put("hostname", hostname.trim());
            }
            List<SystemInfo> list = null;
            try {
                list = systemInfoService.selectAllByParams(params);
            } catch (Exception ignore) {
            }
            if (list == null) {
                list = new ArrayList<SystemInfo>();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("\uFEFF");
            sb.append("主机名,系统版本,CPU核数,CPU使用率%,内存使用率%,磁盘使用率%,状态,最后上报时间,备注\n");
            for (SystemInfo s : list) {
                sb.append(csv(s.getHostname())).append(",");
                sb.append(csv(s.getVersion())).append(",");
                sb.append(csv(s.getCpuCoreNum())).append(",");
                sb.append(fmt(s.getCpuPer())).append(",");
                sb.append(fmt(s.getMemPer())).append(",");
                sb.append(fmt(s.getDiskPer())).append(",");
                sb.append("1".equals(s.getState()) ? "在线" : "下线").append(",");
                sb.append(s.getCreateTime() == null ? "" : SDF.format(s.getCreateTime())).append(",");
                sb.append(csv(s.getRemark())).append("\n");
            }
            auditLogService.save(getAccount(session), "export", "导出主机清单(" + list.size() + "条)", request.getRemoteAddr(), "success");
            writeCsv(response, sb.toString(), "opsmonitor_hosts_" + System.currentTimeMillis() + ".csv");
        } catch (Exception e) {
            logger.error("导出主机清单错误", e);
        }
    }

    /**
     * 告警记录导出
     */
    @RequestMapping("alarms")
    public void exportAlarms(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            String state = request.getParameter("state");
            if (!StringUtils.isEmpty(state)) {
                params.put("state", state.trim());
            }
            String alarmType = request.getParameter("alarmType");
            if (!StringUtils.isEmpty(alarmType)) {
                params.put("alarmType", alarmType.trim());
            }
            List<AlarmRecord> list = null;
            try {
                list = alarmRecordService.selectAllByParams(params);
            } catch (Exception ignore) {
            }
            if (list == null) {
                list = new ArrayList<AlarmRecord>();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("\uFEFF");
            sb.append("时间,类型,主机,标题,级别,状态\n");
            for (AlarmRecord a : list) {
                sb.append(a.getCreateTime() == null ? "" : SDF.format(a.getCreateTime())).append(",");
                sb.append(csv(typeName(a.getAlarmType()))).append(",");
                sb.append(csv(a.getHostname())).append(",");
                sb.append(csv(a.getTitle())).append(",");
                sb.append(levelName(a.getLevel())).append(",");
                sb.append(stateName(a.getState())).append("\n");
            }
            auditLogService.save(getAccount(session), "export", "导出告警记录(" + list.size() + "条)", request.getRemoteAddr(), "success");
            writeCsv(response, sb.toString(), "opsmonitor_alarms_" + System.currentTimeMillis() + ".csv");
        } catch (Exception e) {
            logger.error("导出告警记录错误", e);
        }
    }

    private String getAccount(HttpSession session) {
        try {
            com.opsmonitor.entity.AccountInfo acc = (com.opsmonitor.entity.AccountInfo) session.getAttribute(StaticKeys.LOGIN_KEY);
            return acc == null ? "unknown" : acc.getAccount();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private void writeCsv(HttpServletResponse response, String content, String filename) throws Exception {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + filename);
        response.getWriter().write(content);
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

    private String fmt(Double d) {
        return d == null ? "" : String.valueOf(d);
    }

    private String typeName(String t) {
        if (t == null) return "";
        if ("cpu".equals(t)) return "CPU告警";
        if ("mem".equals(t)) return "内存告警";
        if ("disk".equals(t)) return "磁盘告警";
        if ("hostdown".equals(t)) return "主机下线";
        if ("appdown".equals(t)) return "进程下线";
        if ("heath".equals(t)) return "服务接口";
        if ("intrusion".equals(t)) return "入侵检测";
        return t;
    }

    private String levelName(String l) {
        if (l == null) return "";
        if ("error".equals(l)) return "严重";
        if ("warn".equals(l)) return "警告";
        return "提示";
    }

    private String stateName(String s) {
        if (s == null) return "";
        if ("0".equals(s)) return "未处理";
        if ("1".equals(s)) return "已处理";
        return "已忽略";
    }
}
