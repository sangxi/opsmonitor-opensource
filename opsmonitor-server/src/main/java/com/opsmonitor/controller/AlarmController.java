package com.opsmonitor.controller;

import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.AlarmRecord;
import com.opsmonitor.service.AlarmRecordService;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName:AlarmController.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 告警中心控制器
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Controller
@RequestMapping("/alarm")
public class AlarmController {

    private static final Logger logger = LoggerFactory.getLogger(AlarmController.class);

    @Resource
    private AlarmRecordService alarmRecordService;
    @Resource
    private AuditLogService auditLogService;

    /**
     * 告警中心列表
     */
    @RequestMapping(value = "list")
    public String list(AlarmRecord alarmRecord, Model model, HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        try {
            StringBuffer url = new StringBuffer();
            if (!StringUtils.isEmpty(alarmRecord.getAlarmType())) {
                params.put("alarmType", alarmRecord.getAlarmType().trim());
                url.append("&alarmType=").append(alarmRecord.getAlarmType().trim());
            }
            if (!StringUtils.isEmpty(alarmRecord.getState())) {
                params.put("state", alarmRecord.getState().trim());
                url.append("&state=").append(alarmRecord.getState().trim());
            }
            if (!StringUtils.isEmpty(alarmRecord.getTitle())) {
                params.put("title", CodeUtil.unescape(alarmRecord.getTitle()).trim());
                url.append("&title=").append(CodeUtil.escape(alarmRecord.getTitle()));
            }
            PageInfo pageInfo = alarmRecordService.selectByParams(params, alarmRecord.getPage(), alarmRecord.getPageSize());
            PageUtil.initPageNumber(pageInfo, model);

            model.addAttribute("pageUrl", "/alarm/list?1=1" + url.toString());
            model.addAttribute("page", pageInfo);
            model.addAttribute("alarmRecord", alarmRecord);

            // 各状态数量概览
            Map<String, Long> stateMap = new HashMap<String, Long>();
            stateMap.put("0", 0L);
            stateMap.put("1", 0L);
            stateMap.put("2", 0L);
            List<Map<String, Object>> stateCounts = alarmRecordService.countGroupByState();
            if (stateCounts != null) {
                for (Map<String, Object> sc : stateCounts) {
                    String st = String.valueOf(sc.get("STATE"));
                    Object cnt = sc.get("CNT");
                    long n = cnt == null ? 0 : Long.parseLong(String.valueOf(cnt));
                    stateMap.put(st, n);
                }
            }
            model.addAttribute("state0", stateMap.get("0"));
            model.addAttribute("state1", stateMap.get("1"));
            model.addAttribute("state2", stateMap.get("2"));
        } catch (Exception e) {
            logger.error("查询告警记录错误", e);
            // DB 异常兜底：确保模板不因 page 缺失而渲染中断（避免空白页）
            model.addAttribute("pageUrl", "/alarm/list?1=1");
            model.addAttribute("page", new PageInfo());
            model.addAttribute("alarmRecord", alarmRecord);
            model.addAttribute("state0", 0L);
            model.addAttribute("state1", 0L);
            model.addAttribute("state2", 0L);
        }
        return "alarm/list";
    }

    /**
     * 处理告警（标记已处理/已忽略）
     */
    @RequestMapping(value = "handle")
    public String handle(HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) {
        String id = request.getParameter("id");
        String state = request.getParameter("state");
        try {
            if (!StringUtils.isEmpty(id) && !StringUtils.isEmpty(state)) {
                alarmRecordService.updateState(id, state);
            }
            // 审计
            com.opsmonitor.entity.AccountInfo account = (com.opsmonitor.entity.AccountInfo) session.getAttribute(StaticKeys.LOGIN_KEY);
            String accountName = account == null ? "unknown" : account.getAccount();
            auditLogService.save(accountName, "config", "处理告警：" + id + " 状态=" + state, request.getRemoteAddr(), "success");
        } catch (Exception e) {
            logger.error("处理告警错误", e);
        }
        return "redirect:/alarm/list";
    }

    /**
     * 批量处理：全部标记为已处理
     */
    @RequestMapping(value = "handleAll")
    public String handleAll(HttpServletRequest request, HttpSession session) {
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("state", "0");
            List<AlarmRecord> list = alarmRecordService.selectAllByParams(params);
            for (AlarmRecord r : list) {
                alarmRecordService.updateState(r.getId(), "1");
            }
            com.opsmonitor.entity.AccountInfo account = (com.opsmonitor.entity.AccountInfo) session.getAttribute(StaticKeys.LOGIN_KEY);
            String accountName = account == null ? "unknown" : account.getAccount();
            auditLogService.save(accountName, "config", "批量处理全部未处理告警", request.getRemoteAddr(), "success");
        } catch (Exception e) {
            logger.error("批量处理告警错误", e);
        }
        return "redirect:/alarm/list";
    }

    /**
     * 删除告警记录
     */
    @RequestMapping(value = "del")
    public String delete(HttpServletRequest request, HttpSession session) {
        String errorMsg = "删除告警记录错误：";
        try {
            if (!StringUtils.isEmpty(request.getParameter("id"))) {
                alarmRecordService.deleteById(request.getParameter("id").split(","));
            }
            com.opsmonitor.entity.AccountInfo account = (com.opsmonitor.entity.AccountInfo) session.getAttribute(StaticKeys.LOGIN_KEY);
            String accountName = account == null ? "unknown" : account.getAccount();
            auditLogService.save(accountName, "config", "删除告警记录", request.getRemoteAddr(), "success");
        } catch (Exception e) {
            logger.error(errorMsg, e);
        }
        return "redirect:/alarm/list";
    }

    /**
     * 告警详情（AJAX JSON）
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
            AlarmRecord record = null;
            try {
                record = alarmRecordService.selectById(id);
            } catch (Exception ignore) {
            }
            if (record == null) {
                result.put("code", 1);
                return result;
            }
            result.put("code", 0);
            result.put("id", record.getId());
            result.put("alarmType", record.getAlarmType());
            result.put("hostname", record.getHostname());
            result.put("title", record.getTitle());
            result.put("content", record.getContent());
            result.put("level", record.getLevel());
            result.put("createTime", record.getCreateTime() == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(record.getCreateTime()));
        } catch (Exception e) {
            logger.error("查询告警详情错误", e);
            result.put("code", 1);
        }
        return result;
    }

    /**
     * 未处理告警数量（AJAX，供顶栏/角标刷新）
     */
    @RequestMapping(value = "unhandledCount")
    @ResponseBody
    public Map<String, Object> unhandledCount() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", 0);
        result.put("count", alarmRecordService.countUnhandled());
        return result;
    }
}
