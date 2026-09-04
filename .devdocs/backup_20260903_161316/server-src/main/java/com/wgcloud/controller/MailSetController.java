package com.wgcloud.controller;

import com.wgcloud.entity.MailSet;
import com.wgcloud.service.LogInfoService;
import com.wgcloud.service.MailSetService;
import com.wgcloud.util.msg.WarnMailUtil;
import com.wgcloud.util.msg.WarnWebhookUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version v2.3
 * @ClassName:MailSetController.java
 * @author: http://www.wgstart.com
 * @date: 2019年11月16日
 * @Description: MailSetController.java
 * @Copyright: 2017-2024 wgcloud. All rights reserved.
 */
@Controller
@RequestMapping("/mailset")
public class MailSetController {


    private static final Logger logger = LoggerFactory.getLogger(MailSetController.class);

    @Resource
    private MailSetService mailSetService;
    @Resource
    private LogInfoService logInfoService;


    /**
     * 根据条件查询列表
     *
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "list")
    public String MailSetList(MailSet MailSet, Model model, HttpServletRequest request) {
        Map<String, Object> params = new HashMap<String, Object>();
        try {
            List<MailSet> list = mailSetService.selectAllByParams(params);
            if (list.size() > 0) {
                model.addAttribute("mailSet", list.get(0));
            }
        } catch (Exception e) {
            logger.error("查询邮件设置错误", e);
            logInfoService.save("查询邮件设置错误：", e.toString(), StaticKeys.LOG_ERROR);

        }
        String msg = request.getParameter("msg");
        if (!StringUtils.isEmpty(msg)) {
            if (msg.equals("save")) {
                model.addAttribute("msg", "保存成功");
            } else if (msg.equals("test")) {
                String result = request.getParameter("result");
                if ("success".equals(result)) {
                    model.addAttribute("msg", "测试邮件发送成功");
                } else {
                    model.addAttribute("msg", "测试邮件发送失败，请查看日志");
                }
            } else if (msg.equals("testWebhook")) {
                String result = request.getParameter("result");
                if ("success".equals(result)) {
                    model.addAttribute("msg", "Webhook 测试发送成功");
                } else {
                    model.addAttribute("msg", "Webhook 测试发送失败，请查看日志");
                }
            } else {
                model.addAttribute("msg", "删除成功");
            }
        } else {
            model.addAttribute("msg", "");
        }
        return "mail/view";
    }


    /**
     * 保存邮件设置信息
     *
     * @param MailSet
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "save")
    public String saveMailSet(MailSet mailSet, Model model, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(mailSet.getId())) {
                mailSetService.save(mailSet);
            } else {
                mailSetService.updateById(mailSet);
            }
            StaticKeys.mailSet = mailSet;
        } catch (Exception e) {
            logger.error("保存邮件设置信息错误：", e);
            logInfoService.save("邮件设置信息错误", e.toString(), StaticKeys.LOG_ERROR);
        }
        return "redirect:/mailset/list?msg=save";
    }

    @RequestMapping(value = "test")
    public String test(MailSet mailSet, Model model, HttpServletRequest request) {
        String result = "success";
        try {
            if (StringUtils.isEmpty(mailSet.getId())) {
                mailSetService.save(mailSet);
            } else {
                mailSetService.updateById(mailSet);
            }
            StaticKeys.mailSet = mailSet;
            result = WarnMailUtil.sendMail(mailSet.getToMail(), "WGCLOUD测试邮件发送", "WGCLOUD测试邮件发送");
        } catch (Exception e) {
            logger.error("测试邮件设置信息错误：", e);
            logInfoService.save("测试邮件设置信息错误", e.toString(), StaticKeys.LOG_ERROR);
        }
        return "redirect:/mailset/list?msg=test&result=" + result;
    }

    /**
     * 测试钉钉/企业微信 Webhook 告警发送
     */
    @RequestMapping(value = "testWebhook")
    public String testWebhook(MailSet mailSet, Model model, HttpServletRequest request) {
        String result = "success";
        try {
            if (StringUtils.isEmpty(mailSet.getId())) {
                mailSetService.save(mailSet);
            } else {
                mailSetService.updateById(mailSet);
            }
            StaticKeys.mailSet = mailSet;
            // 未启用 Webhook 开关时直接判定失败，避免"点了测试、提示成功、实际什么都没发"
            if (!"1".equals(mailSet.getSendWebhook())) {
                result = "fail";
                logInfoService.save("测试 Webhook 告警失败", "Webhook 告警开关处于停用状态，请先启用后再测试", StaticKeys.LOG_ERROR);
            } else if (!WarnWebhookUtil.sendWarn("OpsMonitor Webhook 测试", "如果你收到了这条消息，说明 Webhook 告警通道配置正常。")) {
                result = "fail";
            }
        } catch (Exception e) {
            logger.error("测试 Webhook 告警失败：", e);
            logInfoService.save("测试 Webhook 告警失败", e.toString(), StaticKeys.LOG_ERROR);
            result = "fail";
        }
        return "redirect:/mailset/list?msg=testWebhook&result=" + result;
    }

    /**
     * 删除告警邮件信息
     *
     * @param id
     * @param model
     * @param request
     * @param redirectAttributes
     * @return
     */
    @RequestMapping(value = "del")
    public String delete(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String errorMsg = "删除告警邮件设置错误：";
        try {
            if (!StringUtils.isEmpty(request.getParameter("id"))) {
                mailSetService.deleteById(request.getParameter("id").split(","));
                StaticKeys.mailSet = null;
            }
        } catch (Exception e) {
            logger.error(errorMsg, e);
            logInfoService.save(errorMsg, e.toString(), StaticKeys.LOG_ERROR);
        }

        return "redirect:/mailset/list?msg=save";
    }


}
