package com.opsmonitor.controller;

import com.opsmonitor.entity.AlarmRecord;
import com.opsmonitor.entity.SystemInfo;
import com.opsmonitor.service.AlarmRecordService;
import com.opsmonitor.service.ApiTokenService;
import com.opsmonitor.service.AppInfoService;
import com.opsmonitor.service.AuditLogService;
import com.opsmonitor.service.LogInfoService;
import com.opsmonitor.service.SystemInfoService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName:OpenApiController.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 开放 REST API（供第三方系统集成，token 鉴权）
 * <p>
 * 鉴权方式：请求头 X-API-Token 或参数 apiToken，值为 OPEN_API_TOKEN 表中启用且未过期的令牌。
 * <p>
 * 接口列表：
 * - GET /openapi/hosts          主机列表（可带 hostname 模糊）
 * - GET /openapi/host?id=xxx    单台主机详情
 * - GET /openapi/alarms         告警记录（可带 state/alarmType）
 * - GET /openapi/summary        全局汇总（主机数/在线数/告警数等）
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Controller
@RequestMapping("/openapi")
public class OpenApiController {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiController.class);

    @Resource
    private ApiTokenService apiTokenService;
    @Resource
    private SystemInfoService systemInfoService;
    @Resource
    private AlarmRecordService alarmRecordService;
    @Resource
    private AppInfoService appInfoService;
    @Resource
    private LogInfoService logInfoService;
    @Resource
    private AuditLogService auditLogService;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * token 鉴权
     */
    private boolean checkToken(HttpServletRequest request) {
        String token = request.getHeader("X-API-Token");
        if (StringUtils.isEmpty(token)) {
            token = request.getParameter("apiToken");
        }
        return apiTokenService.valid(token);
    }

    private Map<String, Object> fail(int code, String msg) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("code", code);
        m.put("msg", msg);
        return m;
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("code", 0);
        m.put("msg", "success");
        m.put("data", data);
        return m;
    }

    /**
     * 主机列表
     */
    @RequestMapping("hosts")
    @ResponseBody
    public Map<String, Object> hosts(HttpServletRequest request) {
        if (!checkToken(request)) {
            return fail(401, "token invalid");
        }
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            String hostname = request.getParameter("hostname");
            if (!StringUtils.isEmpty(hostname)) {
                params.put("hostname", hostname.trim());
            }
            List<SystemInfo> list = systemInfoService.selectAllByParams(params);
            List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
            if (list != null) {
                for (SystemInfo s : list) {
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put("id", s.getId());
                    item.put("hostname", s.getHostname());
                    item.put("version", s.getVersion());
                    item.put("cpuCoreNum", s.getCpuCoreNum());
                    item.put("cpuPer", s.getCpuPer());
                    item.put("memPer", s.getMemPer());
                    item.put("diskPer", s.getDiskPer());
                    item.put("state", s.getState());
                    item.put("remark", s.getRemark());
                    item.put("createTime", s.getCreateTime() == null ? "" : SDF.format(s.getCreateTime()));
                    data.add(item);
                }
            }
            return ok(data);
        } catch (Exception e) {
            logger.error("开放API-主机列表错误", e);
            return fail(500, e.toString());
        }
    }

    /**
     * 单台主机详情
     */
    @RequestMapping("host")
    @ResponseBody
    public Map<String, Object> host(HttpServletRequest request) {
        if (!checkToken(request)) {
            return fail(401, "token invalid");
        }
        try {
            String id = request.getParameter("id");
            if (StringUtils.isEmpty(id)) {
                return fail(400, "id required");
            }
            SystemInfo s = systemInfoService.selectById(id);
            if (s == null) {
                return fail(404, "host not found");
            }
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", s.getId());
            item.put("hostname", s.getHostname());
            item.put("version", s.getVersion());
            item.put("cpuCoreNum", s.getCpuCoreNum());
            item.put("cpuPer", s.getCpuPer());
            item.put("memPer", s.getMemPer());
            item.put("diskPer", s.getDiskPer());
            item.put("state", s.getState());
            item.put("remark", s.getRemark());
            item.put("createTime", s.getCreateTime() == null ? "" : SDF.format(s.getCreateTime()));
            return ok(item);
        } catch (Exception e) {
            logger.error("开放API-主机详情错误", e);
            return fail(500, e.toString());
        }
    }

    /**
     * 告警记录列表
     */
    @RequestMapping("alarms")
    @ResponseBody
    public Map<String, Object> alarms(HttpServletRequest request) {
        if (!checkToken(request)) {
            return fail(401, "token invalid");
        }
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
            List<AlarmRecord> list = alarmRecordService.selectAllByParams(params);
            List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
            if (list != null) {
                for (AlarmRecord a : list) {
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put("id", a.getId());
                    item.put("alarmType", a.getAlarmType());
                    item.put("hostname", a.getHostname());
                    item.put("title", a.getTitle());
                    item.put("content", a.getContent());
                    item.put("level", a.getLevel());
                    item.put("state", a.getState());
                    item.put("createTime", a.getCreateTime() == null ? "" : SDF.format(a.getCreateTime()));
                    data.add(item);
                }
            }
            return ok(data);
        } catch (Exception e) {
            logger.error("开放API-告警列表错误", e);
            return fail(500, e.toString());
        }
    }

    /**
     * 全局汇总
     */
    @RequestMapping("summary")
    @ResponseBody
    public Map<String, Object> summary(HttpServletRequest request) {
        if (!checkToken(request)) {
            return fail(401, "token invalid");
        }
        try {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("serverTime", SDF.format(new java.util.Date()));

            // 主机统计
            Map<String, Object> hostParams = new HashMap<String, Object>();
            List<SystemInfo> hosts = systemInfoService.selectAllByParams(hostParams);
            int total = hosts == null ? 0 : hosts.size();
            int online = 0;
            if (hosts != null) {
                for (SystemInfo s : hosts) {
                    if ("1".equals(s.getState())) {
                        online++;
                    }
                }
            }
            result.put("hostTotal", total);
            result.put("hostOnline", online);
            result.put("hostDown", total - online);

            // 进程统计
            try {
                Map<String, Object> appParams = new HashMap<String, Object>();
                result.put("appTotal", appInfoService.selectAllByParams(appParams).size());
            } catch (Exception e) {
                result.put("appTotal", 0);
            }

            // 告警统计
            Map<String, Object> alarmParams = new HashMap<String, Object>();
            result.put("alarmTotal", alarmRecordService.selectAllByParams(alarmParams).size());
            alarmParams.put("state", "0");
            result.put("alarmUnhandled", alarmRecordService.selectAllByParams(alarmParams).size());

            // 未读运行日志数（LOG_INFO state=1 视为错误日志）
            try {
                Map<String, Object> logParams = new HashMap<String, Object>();
                logParams.put("state", com.opsmonitor.util.staticvar.StaticKeys.LOG_ERROR);
                result.put("logError", logInfoService.countByParams(logParams));
            } catch (Exception e) {
                result.put("logError", 0);
            }

            return ok(result);
        } catch (Exception e) {
            logger.error("开放API-汇总错误", e);
            return fail(500, e.toString());
        }
    }
}
