package com.wgcloud.controller;

import com.github.pagehelper.PageInfo;
import com.wgcloud.entity.HeathMonitor;
import com.wgcloud.service.HeathMonitorService;
import com.wgcloud.service.LogInfoService;
import com.wgcloud.util.PageUtil;
import com.wgcloud.util.staticvar.StaticKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @version v2.3
 * @ClassName:HeathMonitorController.java
 * @author: http://www.wgstart.com
 * @date: 2019年11月16日
 * @Description: HeathMonitorController.java
 * @Copyright: 2017-2024 wgcloud. All rights reserved.
 */
@Controller
@RequestMapping("/heathMonitor")
public class HeathMonitorController {


    private static final Logger logger = LoggerFactory.getLogger(HeathMonitorController.class);

    /**
     * 监控类型：TCP 端口
     */
    private static final String MONITOR_TYPE_TCP = "2";

    /**
     * TCP 连接超时默认值（毫秒）
     */
    private static final Integer DEFAULT_TIMEOUT_MS = 3000;

    private static final int MIN_TIMEOUT_MS = 100;

    private static final int MAX_TIMEOUT_MS = 30000;

    /**
     * 允许 IPv4 地址或域名
     */
    private static final Pattern IP_OR_HOST_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3})$"
                    + "|^(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*)$");

    @Resource
    private HeathMonitorService heathMonitorService;
    @Resource
    private LogInfoService logInfoService;


    /**
     * 根据条件查询心跳监控列表
     *
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "list")
    public String heathMonitorList(HeathMonitor HeathMonitor, Model model, HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        try {
            // 列表页筛选条件：此前固定传空 Map，页面上的筛选参数从未生效
            String appName = StringUtils.trimToEmpty(request.getParameter("appName"));
            if (!appName.isEmpty()) {
                params.put("appName", appName);
            }
            String monitorType = StringUtils.trimToEmpty(request.getParameter("monitorType"));
            if (!monitorType.isEmpty()) {
                params.put("monitorType", monitorType);
            }
            String heathStatus = StringUtils.trimToEmpty(request.getParameter("heathStatus"));
            if (!heathStatus.isEmpty()) {
                // "other" 表示筛选所有非 200 的异常状态，其余值按原样下发
                if ("other".equals(heathStatus)) {
                    params.put("heathStatusNotOk", "1");
                } else {
                    params.put("heathStatus", heathStatus);
                }
            }

            PageInfo pageInfo = heathMonitorService.selectByParams(params, HeathMonitor.getPage(), HeathMonitor.getPageSize());
            PageUtil.initPageNumber(pageInfo, model);

            // 分页链接需要携带当前筛选条件，否则翻页后筛选会丢失
            StringBuilder pageUrl = new StringBuilder("/heathMonitor/list?1=1");
            try {
                if (!appName.isEmpty()) {
                    pageUrl.append("&appName=").append(URLEncoder.encode(appName, "UTF-8"));
                }
                if (!monitorType.isEmpty()) {
                    pageUrl.append("&monitorType=").append(URLEncoder.encode(monitorType, "UTF-8"));
                }
                if (!heathStatus.isEmpty()) {
                    pageUrl.append("&heathStatus=").append(URLEncoder.encode(heathStatus, "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                logger.error("构造分页链接失败", e);
            }
            model.addAttribute("pageUrl", pageUrl.toString());

            // 回显筛选条件到搜索表单
            model.addAttribute("queryAppName", appName);
            model.addAttribute("queryMonitorType", monitorType);
            model.addAttribute("queryHeathStatus", heathStatus);
            model.addAttribute("page", pageInfo);
        } catch (Exception e) {
            logger.error("查询服务心跳监控错误", e);
            logInfoService.save("查询心跳监控错误", e.toString(), StaticKeys.LOG_ERROR);

        }
        return "heath/list";
    }


    /**
     * 保存心跳监控信息
     *
     * @param HeathMonitor
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "save")
    public String saveHeathMonitor(HeathMonitor HeathMonitor, Model model, HttpServletRequest request) {
        // 保存前做服务端校验：前端仅靠隐藏表单项切换，无法阻止非法值直接提交
        String validateMsg = validateHeathMonitor(HeathMonitor);
        if (validateMsg != null) {
            model.addAttribute("errorMsg", validateMsg);
            model.addAttribute("heathMonitor", HeathMonitor);
            return "heath/add";
        }
        try {
            if (StringUtils.isEmpty(HeathMonitor.getId())) {
                heathMonitorService.save(HeathMonitor);
            } else {
                heathMonitorService.updateById(HeathMonitor);
            }

        } catch (Exception e) {
            logger.error("保存服务心跳监控错误：", e);
            logInfoService.save(HeathMonitor.getAppName(), "保存心跳监控错误：" + e.toString(), StaticKeys.LOG_ERROR);
        }
        return "redirect:/heathMonitor/list";
    }

    /**
     * 校验监控配置合法性，并清理与当前监控类型无关的残留字段。
     * <p>
     * 表单中 HTTP 与 TCP 两组输入是同时提交的（前端仅用 display 隐藏），
     * 若不清理，切换监控类型后旧值会残留在库中，造成列表页展示与检测逻辑不一致。
     *
     * @param heathMonitor 待校验对象
     * @return 校验通过返回 null，否则返回错误提示
     */
    private String validateHeathMonitor(HeathMonitor heathMonitor) {
        if (StringUtils.isEmpty(heathMonitor.getAppName())) {
            return "服务名称不能为空";
        }
        String type = StringUtils.isEmpty(heathMonitor.getMonitorType()) ? "1" : heathMonitor.getMonitorType().trim();
        heathMonitor.setMonitorType(type);

        if (MONITOR_TYPE_TCP.equals(type)) {
            if (StringUtils.isEmpty(heathMonitor.getHostIp())) {
                return "TCP 端口检测必须填写目标主机";
            }
            if (!IP_OR_HOST_PATTERN.matcher(heathMonitor.getHostIp().trim()).matches()) {
                return "目标主机格式不正确，请填写 IP 地址或域名";
            }
            if (heathMonitor.getPort() == null) {
                return "TCP 端口检测必须填写目标端口";
            }
            if (heathMonitor.getPort() < 1 || heathMonitor.getPort() > 65535) {
                return "目标端口必须在 1-65535 之间";
            }
            if (heathMonitor.getTimeoutMs() == null) {
                heathMonitor.setTimeoutMs(DEFAULT_TIMEOUT_MS);
            } else if (heathMonitor.getTimeoutMs() < MIN_TIMEOUT_MS || heathMonitor.getTimeoutMs() > MAX_TIMEOUT_MS) {
                return "连接超时必须在 " + MIN_TIMEOUT_MS + " - " + MAX_TIMEOUT_MS + " 毫秒之间";
            }
            // 清理 HTTP 字段
            heathMonitor.setHeathUrl("");
        } else {
            if (StringUtils.isEmpty(heathMonitor.getHeathUrl())) {
                return "HTTP 接口检测必须填写服务接口 URL";
            }
            if (!heathMonitor.getHeathUrl().trim().toLowerCase().startsWith("http")) {
                return "服务接口 URL 必须以 http 或 https 开头";
            }
            // 清理 TCP 字段，端口置空避免与 HTTP 目标混淆
            heathMonitor.setHostIp("");
            heathMonitor.setPort(null);
            heathMonitor.setTimeoutMs(null);
        }
        return null;
    }


    /**
     * 查看该心跳监控
     *
     * @param HeathMonitor
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "edit")
    public String edit(Model model, HttpServletRequest request) {
        String errorMsg = "编辑服务心跳监控：";
        String id = request.getParameter("id");
        HeathMonitor heathMonitor = new HeathMonitor();
        if (StringUtils.isEmpty(id)) {
            model.addAttribute("heathMonitor", heathMonitor);
            return "heath/add";
        }

        try {
            heathMonitor = heathMonitorService.selectById(id);
            model.addAttribute("heathMonitor", heathMonitor);
        } catch (Exception e) {
            logger.error(errorMsg, e);
            logInfoService.save(heathMonitor.getAppName(), errorMsg + e.toString(), StaticKeys.LOG_ERROR);
        }
        return "heath/add";
    }

    /**
     * 查看该心跳监控
     *
     * @param HeathMonitor
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "view")
    public String view(Model model, HttpServletRequest request) {
        String errorMsg = "查看服务心跳监控：";
        String id = request.getParameter("id");
        String date = request.getParameter("date");
        HeathMonitor heathMonitor = new HeathMonitor();
        try {
            heathMonitor = heathMonitorService.selectById(id);
            model.addAttribute("heathMonitor", heathMonitor);
        } catch (Exception e) {
            logger.error(errorMsg, e);
            logInfoService.save(heathMonitor.getAppName(), errorMsg + e.toString(), StaticKeys.LOG_ERROR);
        }
        return "heath/view";
    }


    /**
     * 删除心跳监控
     *
     * @param id
     * @param model
     * @param request
     * @param redirectAttributes
     * @return
     */
    @RequestMapping(value = "del")
    public String delete(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String errorMsg = "删除服务心跳监控错误：";
        HeathMonitor HeathMonitor = new HeathMonitor();
        try {
            if (!StringUtils.isEmpty(request.getParameter("id"))) {
                HeathMonitor = heathMonitorService.selectById(request.getParameter("id"));
                logInfoService.save("删除服务心跳监控：" + HeathMonitor.getAppName(), "删除服务心跳监控：" + HeathMonitor.getAppName() + "：" + HeathMonitor.getHeathUrl(), StaticKeys.LOG_ERROR);
                heathMonitorService.deleteById(request.getParameter("id").split(","));
            }
        } catch (Exception e) {
            logger.error(errorMsg, e);
            logInfoService.save(HeathMonitor.getAppName(), errorMsg + e.toString(), StaticKeys.LOG_ERROR);
        }

        return "redirect:/heathMonitor/list";
    }


}
