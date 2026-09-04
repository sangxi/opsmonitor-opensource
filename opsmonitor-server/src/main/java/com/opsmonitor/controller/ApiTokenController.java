package com.opsmonitor.controller;

import com.github.pagehelper.PageInfo;
import com.opsmonitor.entity.ApiToken;
import com.opsmonitor.service.ApiTokenService;
import com.opsmonitor.service.AuditLogService;
import com.opsmonitor.util.PageUtil;
import com.opsmonitor.util.UUIDUtil;
import com.opsmonitor.util.staticvar.StaticKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName:ApiTokenController.java
 * @author: OpsMonitor Team
 * @date: 2026年09月03日
 * @Description: 开放 API 令牌管理
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Controller
@RequestMapping("/apiToken")
public class ApiTokenController {

    private static final Logger logger = LoggerFactory.getLogger(ApiTokenController.class);

    @Resource
    private ApiTokenService apiTokenService;
    @Resource
    private AuditLogService auditLogService;

    /**
     * 令牌列表
     */
    @RequestMapping(value = "list")
    public String list(ApiToken apiToken, Model model, HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        try {
            PageInfo pageInfo = apiTokenService.selectByParams(params, apiToken.getPage(), apiToken.getPageSize());
            PageUtil.initPageNumber(pageInfo, model);
            model.addAttribute("pageUrl", "/apiToken/list?1=1");
            model.addAttribute("page", pageInfo);
            model.addAttribute("apiToken", apiToken);
        } catch (Exception e) {
            logger.error("查询 API 令牌错误", e);
        }
        String msg = request.getParameter("msg");
        if (!StringUtils.isEmpty(msg)) {
            if ("save".equals(msg)) {
                model.addAttribute("msg", "保存成功");
            } else if ("del".equals(msg)) {
                model.addAttribute("msg", "删除成功");
            } else if ("regenerate".equals(msg)) {
                model.addAttribute("msg", "已重新生成令牌");
            } else {
                model.addAttribute("msg", "");
            }
        } else {
            model.addAttribute("msg", "");
        }
        return "apiToken/list";
    }

    /**
     * 新增/保存令牌
     */
    @RequestMapping(value = "save")
    public String save(ApiToken apiToken, Model model, HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            // 处理过期时间字符串（表单提交的是 yyyy-MM-dd）
            String expireDateStr = request.getParameter("expireDateStr");
            if (!StringUtils.isEmpty(expireDateStr)) {
                try {
                    apiToken.setExpireTime(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(expireDateStr));
                } catch (Exception ignore) {
                }
            }
            if (StringUtils.isEmpty(apiToken.getId())) {
                // 新增：生成随机令牌
                if (StringUtils.isEmpty(apiToken.getToken())) {
                    apiToken.setToken("om_" + UUIDUtil.getUUID().replace("-", "").substring(0, 24));
                }
                apiToken.setCreateTime(new Date());
                if (StringUtils.isEmpty(apiToken.getStatus())) {
                    apiToken.setStatus("0");
                }
                apiTokenService.save(apiToken);
            } else {
                apiTokenService.updateById(apiToken);
            }
            auditLogService.save(getAccount(session), "config", "新增/修改 API 令牌：" + apiToken.getName(), request.getRemoteAddr(), "success");
        } catch (Exception e) {
            logger.error("保存 API 令牌错误：", e);
        }
        return "redirect:/apiToken/list?msg=save";
    }

    /**
     * 重新生成令牌
     */
    @RequestMapping(value = "regenerate")
    public String regenerate(HttpServletRequest request, HttpSession session) {
        try {
            String id = request.getParameter("id");
            if (!StringUtils.isEmpty(id)) {
                ApiToken token = apiTokenService.selectById(id);
                if (token != null) {
                    token.setToken("om_" + UUIDUtil.getUUID().replace("-", "").substring(0, 24));
                    apiTokenService.updateById(token);
                    auditLogService.save(getAccount(session), "config", "重新生成 API 令牌：" + token.getName(), request.getRemoteAddr(), "success");
                }
            }
        } catch (Exception e) {
            logger.error("重新生成 API 令牌错误：", e);
        }
        return "redirect:/apiToken/list?msg=regenerate";
    }

    /**
     * 删除令牌
     */
    @RequestMapping(value = "del")
    public String delete(HttpServletRequest request, HttpSession session) {
        try {
            if (!StringUtils.isEmpty(request.getParameter("id"))) {
                apiTokenService.deleteById(request.getParameter("id").split(","));
                auditLogService.save(getAccount(session), "config", "删除 API 令牌", request.getRemoteAddr(), "success");
            }
        } catch (Exception e) {
            logger.error("删除 API 令牌错误：", e);
        }
        return "redirect:/apiToken/list?msg=del";
    }

    private String getAccount(HttpSession session) {
        try {
            com.opsmonitor.entity.AccountInfo acc = (com.opsmonitor.entity.AccountInfo) session.getAttribute(StaticKeys.LOGIN_KEY);
            return acc == null ? "unknown" : acc.getAccount();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
