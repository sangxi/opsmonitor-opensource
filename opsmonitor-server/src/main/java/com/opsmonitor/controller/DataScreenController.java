package com.opsmonitor.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.*;
import com.opsmonitor.service.*;
import com.opsmonitor.util.FormatUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @version v2.0
 * @ClassName:DataScreenController.java
 * @author: OpsMonitor Team
 * @Description: 监控大屏（自研）——页面 + 聚合数据接口
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Controller
@RequestMapping(value = "/datascreen")
public class DataScreenController {

    private static final Logger logger = LoggerFactory.getLogger(DataScreenController.class);

    @Autowired
    private SystemInfoService systemInfoService;
    @Autowired
    private AppInfoService appInfoService;
    @Autowired
    private HeathMonitorService heathMonitorService;
    @Autowired
    private LogInfoService logInfoService;
    @Autowired
    private DbInfoService dbInfoService;
    @Autowired
    private DbTableService dbTableService;

    /**
     * 大屏页面（无需登录，可投屏）
     */
    @RequestMapping("")
    public String screen() {
        return "datascreen/screen";
    }

    /**
     * 聚合数据接口：返回大屏全部图表所需 JSON
     */
    @RequestMapping("/data")
    @ResponseBody
    public String data(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> params = new HashMap<String, Object>();
        try {
            List<SystemInfo> hostList = systemInfoService.selectAllByParams(params);

            // ---------- 核心指标 ----------
            int totalHost = hostList == null ? 0 : hostList.size();
            int onlineHost = 0;
            int offlineHost = 0;
            int cpuHigh = 0;   // CPU>80%
            int memHigh = 0;   // 内存>80%
            double cpuSum = 0, memSum = 0;
            Map<String, Integer> osMap = new LinkedHashMap<String, Integer>();
            List<Object> hostPerf = new ArrayList<Object>();  // 主机CPU/内存TOP
            if (hostList != null) {
                for (SystemInfo h : hostList) {
                    if (StringUtils.isEmpty(h.getState()) || "1".equals(h.getState())) {
                        onlineHost++;
                    } else {
                        offlineHost++;
                    }
                    // 系统类型分布（按 version 粗分类）
                    String osKey = classifyOs(h.getVersionDetail() == null ? h.getVersion() : h.getVersionDetail());
                    osMap.put(osKey, osMap.containsKey(osKey) ? osMap.get(osKey) + 1 : 1);
                    // 性能汇总
                    double cpu = h.getCpuPer() == null ? 0 : h.getCpuPer();
                    double mem = h.getMemPer() == null ? 0 : h.getMemPer();
                    cpuSum += cpu;
                    memSum += mem;
                    if (cpu > 80) cpuHigh++;
                    if (mem > 80) memHigh++;
                    // TOP 明细
                    JSONObject jo = new JSONObject();
                    jo.set("hostname", h.getHostname());
                    jo.set("cpu", FormatUtil.formatDouble(cpu, 1));
                    jo.set("mem", FormatUtil.formatDouble(mem, 1));
                    jo.set("state", h.getState());
                    hostPerf.add(jo);
                }
            }
            // 主机CPU使用率降序
            hostPerf.sort((a, b) -> Double.compare(
                    Double.valueOf(((JSONObject) b).getDouble("cpu")),
                    Double.valueOf(((JSONObject) a).getDouble("cpu"))));

            result.put("totalHost", totalHost);
            result.put("onlineHost", onlineHost);
            result.put("offlineHost", offlineHost);
            result.put("cpuHigh", cpuHigh);
            result.put("memHigh", memHigh);
            result.put("avgCpu", hostList == null || hostList.isEmpty() ? 0 : FormatUtil.formatDouble(cpuSum / hostList.size(), 1));
            result.put("avgMem", hostList == null || hostList.isEmpty() ? 0 : FormatUtil.formatDouble(memSum / hostList.size(), 1));

            // ---------- 系统类型分布 ----------
            List<Object> osList = new ArrayList<Object>();
            for (Map.Entry<String, Integer> e : osMap.entrySet()) {
                JSONObject jo = new JSONObject();
                jo.set("name", e.getKey());
                jo.set("value", e.getValue());
                osList.add(jo);
            }
            result.put("osList", osList);

            // ---------- 主机状态分布（在线/离线/高CPU/高内存）----------
            List<Object> statusList = new ArrayList<Object>();
            statusList.add(statusItem("在线主机", onlineHost, "#22c55e"));
            statusList.add(statusItem("离线主机", offlineHost, "#ef4444"));
            statusList.add(statusItem("CPU>80%", cpuHigh, "#f59e0b"));
            statusList.add(statusItem("内存>80%", memHigh, "#8b5cf6"));
            result.put("statusList", statusList);

            // ---------- 主机性能TOP ----------
            result.put("hostPerf", hostPerf.size() > 10 ? hostPerf.subList(0, 10) : hostPerf);

            // ---------- 进程监控 ----------
            params.clear();
            int totalApp = appInfoService.countByParams(params);
            result.put("totalApp", totalApp);
            params.put("cpuPer", 80);
            int appHigh = appInfoService.countByParams(params);
            result.put("appHigh", appHigh);

            // ---------- 服务接口 ----------
            params.clear();
            int totalHeath = heathMonitorService.countByParams(params);
            result.put("totalHeath", totalHeath);
            params.put("heathStatus", "200");
            int heathOk = heathMonitorService.countByParams(params);
            result.put("heathOk", heathOk);
            result.put("heathErr", totalHeath - heathOk);

            // ---------- 数据源 / 数据表 ----------
            params.clear();
            int dbInfoSize = dbInfoService.countByParams(params);
            result.put("dbInfoSize", dbInfoSize);
            Long dbTableSum = dbTableService.sumByParams(params);
            result.put("dbTableSum", dbTableSum == null ? 0 : dbTableSum);

            // ---------- 最近日志（滚动）----------
            params.clear();
            PageInfo<LogInfo> logPage = logInfoService.selectByParams(params, 1, 20);
            List<Object> logList = new ArrayList<Object>();
            if (logPage != null && logPage.getList() != null) {
                for (LogInfo log : logPage.getList()) {
                    JSONObject jo = new JSONObject();
                    jo.set("time", log.getCreateTime() == null ? "" : log.getCreateTime().toString().substring(0, Math.min(19, log.getCreateTime().toString().length())));
                    jo.set("host", log.getHostname());
                    jo.set("state", log.getState());
                    String content = log.getInfoContent() == null ? "" : log.getInfoContent();
                    jo.set("content", content.length() > 40 ? content.substring(0, 40) : content);
                    logList.add(jo);
                }
            }
            result.put("logList", logList);

            // ---------- 服务接口明细 ----------
            params.clear();
            List<HeathMonitor> heathList = heathMonitorService.selectAllByParams(params);
            List<Object> heathDetail = new ArrayList<Object>();
            if (heathList != null) {
                for (HeathMonitor h : heathList) {
                    if (heathDetail.size() >= 8) break;
                    JSONObject jo = new JSONObject();
                    jo.set("name", h.getAppName());
                    jo.set("status", h.getHeathStatus());
                    jo.set("url", h.getHeathUrl());
                    heathDetail.add(jo);
                }
            }
            result.put("heathDetail", heathDetail);

        } catch (Exception e) {
            logger.error("监控大屏数据接口异常：", e);
            result.put("error", e.toString());
        }
        return JSONUtil.toJsonStr(result);
    }

    private JSONObject statusItem(String name, int value, String color) {
        JSONObject jo = new JSONObject();
        jo.set("name", name);
        jo.set("value", value);
        jo.set("color", color);
        return jo;
    }

    /**
     * 系统类型归类：Linux / Windows / macOS / BSD / 其他
     */
    private String classifyOs(String detail) {
        if (StringUtils.isEmpty(detail)) {
            return "未知";
        }
        String d = detail.toLowerCase();
        if (d.contains("windows")) return "Windows";
        if (d.contains("darwin") || d.contains("mac")) return "macOS";
        if (d.contains("ubuntu")) return "Ubuntu";
        if (d.contains("debian")) return "Debian";
        if (d.contains("centos")) return "CentOS";
        if (d.contains("red hat") || d.contains("redhat")) return "RedHat";
        if (d.contains("suse") || d.contains("opensuse")) return "SUSE";
        if (d.contains("fedora")) return "Fedora";
        if (d.contains("freebsd")) return "FreeBSD";
        if (d.contains("alpine")) return "Alpine";
        if (d.contains("linux")) return "Linux";
        return "其他";
    }
}
