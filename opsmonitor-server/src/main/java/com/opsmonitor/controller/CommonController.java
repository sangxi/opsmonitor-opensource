package com.opsmonitor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @version v2.3
 * @ClassName:CommonController.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: CommonController.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Controller
@RequestMapping(value = "/common/error")
public class CommonController {


    /**
     * 转向到404页面
     *
     * @param model
     * @param request
     * @return
     */
    @RequestMapping("404")
    public String to404(Model model, HttpServletRequest request) {
        return "error/404";
    }

    /**
     * 转向到500页面
     *
     * @param model
     * @param request
     * @return
     */
    @RequestMapping("500")
    public String to500(Model model, HttpServletRequest request) {
        return "error/500";
    }

}