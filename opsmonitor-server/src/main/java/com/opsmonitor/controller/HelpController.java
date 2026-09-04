package com.opsmonitor.controller;

import com.opsmonitor.entity.Help;
import com.opsmonitor.entity.HelpCategory;
import com.opsmonitor.service.HelpCategoryService;
import com.opsmonitor.service.HelpService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version v2.0
 * @ClassName:HelpController.java
 * @author: OpsMonitor Team
 * @Description: 使用教程
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Controller
@RequestMapping(value = "/help")
public class HelpController {

    private static final Logger logger = LoggerFactory.getLogger(HelpController.class);

    @Autowired
    private HelpService helpService;
    @Autowired
    private HelpCategoryService helpCategoryService;

    /**
     * 教程列表（分类 + 文章）
     */
    @RequestMapping("/list")
    public String list(Model model, HttpServletRequest request, HttpSession session) {
        session.setAttribute("menuActive", "71");
        try {
            List<HelpCategory> categoryList = helpCategoryService.selectAll();
            model.addAttribute("categoryList", categoryList);

            // 默认选中第一个分类
            String activeCategory = request.getParameter("category");
            if (StringUtils.isEmpty(activeCategory) && categoryList != null && !categoryList.isEmpty()) {
                activeCategory = categoryList.get(0).getId();
            }

            Map<String, Object> params = new HashMap<String, Object>();
            if (!StringUtils.isEmpty(activeCategory)) {
                params.put("categoryId", activeCategory);
            }
            List<Help> helpList = helpService.selectAllByParams(params);
            model.addAttribute("helpList", helpList);
            model.addAttribute("activeCategory", activeCategory);
        } catch (Exception e) {
            logger.error("教程列表查询错误：", e);
        }
        return "help/list";
    }

    /**
     * 教程详情
     */
    @RequestMapping("/view")
    public String view(Model model, HttpServletRequest request, HttpSession session) {
        session.setAttribute("menuActive", "71");
        String id = request.getParameter("id");
        if (StringUtils.isEmpty(id)) {
            return "error/500";
        }
        try {
            Help help = helpService.selectById(id);
            if (help == null) {
                return "error/500";
            }
            model.addAttribute("help", help);
            List<HelpCategory> categoryList = helpCategoryService.selectAll();
            model.addAttribute("categoryList", categoryList);
            model.addAttribute("activeCategory", help.getCategoryId());
            // 同分类的其他文章
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("categoryId", help.getCategoryId());
            List<Help> relatedList = helpService.selectAllByParams(params);
            model.addAttribute("relatedList", relatedList);
        } catch (Exception e) {
            logger.error("教程详情查询错误：", e);
        }
        return "help/view";
    }

    /**
     * 各系统部署（Linux/Windows/macOS/Docker/宝塔/1Panel/ARM）
     */
    @RequestMapping("/deploy")
    public String deploy(Model model, HttpServletRequest request, HttpSession session) {
        session.setAttribute("menuActive", "71");
        return listByKeyword(model, "cat_", "help/list");
    }

    /**
     * 告警配置说明
     */
    @RequestMapping("/alarm")
    public String alarm(Model model, HttpServletRequest request, HttpSession session) {
        session.setAttribute("menuActive", "71");
        model.addAttribute("forceCategory", "cat_alarm");
        return list(model, request, session);
    }

    /**
     * 常见问题
     */
    @RequestMapping("/faq")
    public String faq(Model model, HttpServletRequest request, HttpSession session) {
        session.setAttribute("menuActive", "71");
        model.addAttribute("forceCategory", "cat_faq");
        return list(model, request, session);
    }

    private String listByKeyword(Model model, String prefix, String view) {
        try {
            List<HelpCategory> categoryList = helpCategoryService.selectAll();
            model.addAttribute("categoryList", categoryList);
            String activeCategory = null;
            if (categoryList != null) {
                for (HelpCategory c : categoryList) {
                    if (c.getId() != null && c.getId().startsWith(prefix)) {
                        activeCategory = c.getId();
                        break;
                    }
                }
            }
            Map<String, Object> params = new HashMap<String, Object>();
            if (activeCategory != null) {
                params.put("categoryId", activeCategory);
            }
            List<Help> helpList = helpService.selectAllByParams(params);
            model.addAttribute("helpList", helpList);
            model.addAttribute("activeCategory", activeCategory);
        } catch (Exception e) {
            logger.error("教程列表查询错误：", e);
        }
        return view;
    }
}
