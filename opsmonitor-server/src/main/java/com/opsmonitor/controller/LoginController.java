package com.opsmonitor.controller;

import com.opsmonitor.config.CommonConfig;
import com.opsmonitor.entity.AccountInfo;
import com.opsmonitor.service.AuditLogService;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version v2.3
 * @ClassName:LoginController.java
 * @author: OpsMonitor Team
 * @date: 2019年11月16日
 * @Description: LoginController.java
 * @Copyright: 2017-2026 OpsMonitor. All rights reserved.
 */
@Controller
@RequestMapping(value = "/login")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Resource
    private CommonConfig commonConfig;

    @Resource
    private AuditLogService auditLogService;

    /**
     * 转向到登录页面
     *
     * @param model
     * @param request
     * @return
     */
    @RequestMapping("toLogin")
    public String toLogin(Model model, HttpServletRequest request) {
        return "login/login";
    }

    /**
     * 登出系统
     *
     * @param model
     * @param request
     * @return
     */
    @RequestMapping("loginOut")
    public String loginOut(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();
        try {
            AccountInfo acc = (AccountInfo) session.getAttribute(StaticKeys.LOGIN_KEY);
            auditLogService.save(acc == null ? "" : acc.getAccount(), "logout", "退出系统", request.getRemoteAddr(), "success");
        } catch (Exception ignored) {
        }
        session.invalidate();
        return "redirect:/login/toLogin";
    }

    // 登录失败次数记录，key为「IP|用户名」，value为失败次数
    private static final Map<String, Integer> loginFailMap = new ConcurrentHashMap<>();
    // 登录失败次数限制
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    // 登录失败锁定时间（毫秒）
    private static final long LOGIN_LOCK_TIME = 30 * 60 * 1000;
    // 登录失败锁定记录，key为「IP|用户名」，value为锁定时间戳
    private static final Map<String, Long> loginLockMap = new ConcurrentHashMap<>();

    /**
     * 失败记录条目上限。
     * <p>
     * 原实现以用户名为唯一维度且无任何容量限制，攻击者用大量随机用户名发起登录即可让
     * 这两个 Map 无限膨胀，最终导致内存耗尽。超过上限时整体清空——此时保留的
     * 都是未触发锁定的低频失败记录，清空的代价可以接受。
     */
    private static final int MAX_FAIL_RECORD_SIZE = 10000;

    /**
     * 管理员登录验证
     * <p>
     * 验证码安全策略：
     * 1. 校验通过/失败后立即从 session 清除，避免重放使用
     * 2. 校验生成时间戳，超过 CODE_EXPIRE_MS（5分钟）失效
     * 3. 登录失败时回传 needRefreshCode 标志，前端自动刷新图片
     *
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "login")
    public String login(Model model, HttpServletRequest request) {
        String userName = request.getParameter("userName");
        String passwd = request.getParameter("md5pwd");
        String code = request.getParameter("code");
        HttpSession session = request.getSession();
        // 失败计数维度：IP + 用户名，避免单一维度被滥用
        String lockKey = buildLockKey(request, userName);
        try {
            if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(passwd) && !StringUtils.isEmpty(code)) {
                // 检查是否被锁定
                if (isLocked(lockKey)) {
                    model.addAttribute("error", "登录失败次数过多，请30分钟后再试");
                    model.addAttribute("needRefreshCode", true);
                    return "login/login";
                }

                // 校验验证码：先取 session 中的值与生成时间，立即消费（无论成功失败都清除）
                String sessionCode = (String) session.getAttribute(StaticKeys.SESSION_CODE);
                Long codeGenTime = (Long) session.getAttribute(StaticKeys.SESSION_CODE_TIME);
                session.removeAttribute(StaticKeys.SESSION_CODE);
                session.removeAttribute(StaticKeys.SESSION_CODE_TIME);

                if (StringUtils.isEmpty(sessionCode)) {
                    model.addAttribute("error", "验证码已失效，请刷新后重试");
                    model.addAttribute("needRefreshCode", true);
                    return "login/login";
                }
                // 有效期校验
                if (codeGenTime == null || System.currentTimeMillis() - codeGenTime > StaticKeys.CODE_EXPIRE_MS) {
                    model.addAttribute("error", "验证码已过期，请刷新后重试");
                    model.addAttribute("needRefreshCode", true);
                    return "login/login";
                }
                // 大小写不敏感比较
                if (!code.equalsIgnoreCase(sessionCode)) {
                    model.addAttribute("error", "验证码错误");
                    model.addAttribute("needRefreshCode", true);
                    return "login/login";
                }

                AccountInfo accountInfo = new AccountInfo();
                // 验证密码（前端已使用MD5加密）
                if (passwd.equals(com.opsmonitor.util.shorturl.MD5.GetMD5Code(commonConfig.getAdmindPwd())) && StaticKeys.ADMIN_ACCOUNT.equals(userName)) {
                    // 登录成功，清除失败次数记录
                    loginFailMap.remove(lockKey);
                    loginLockMap.remove(lockKey);

                    accountInfo.setAccount(StaticKeys.ADMIN_ACCOUNT);
                    accountInfo.setId(StaticKeys.ADMIN_ACCOUNT);
                    accountInfo.setRole("admin"); // 设置管理员角色
                    request.getSession().setAttribute(StaticKeys.LOGIN_KEY, accountInfo);
                    // 记录登录审计
                    try {
                        auditLogService.save(StaticKeys.ADMIN_ACCOUNT, "login", "登录系统", request.getRemoteAddr(), "success");
                    } catch (Exception ignored) {
                    }
                    return "redirect:/dash/main";
                } else {
                    // 记录登录失败审计
                    try {
                        auditLogService.save(userName == null ? "" : userName, "login", "登录失败", request.getRemoteAddr(), "fail");
                    } catch (Exception ignored) {
                    }
                    // 登录失败，记录失败次数
                    recordLoginFail(lockKey);
                    int failCount = loginFailMap.getOrDefault(lockKey, 0);
                    int remainingCount = MAX_LOGIN_FAIL_COUNT - failCount;
                    if (remainingCount > 0) {
                        model.addAttribute("error", "帐号或者密码错误，还有" + remainingCount + "次机会");
                    } else {
                        model.addAttribute("error", "登录失败次数过多，请30分钟后再试");
                    }
                    model.addAttribute("needRefreshCode", true);
                }
            }
        } catch (Exception e) {
            logger.error("登录异常：", e);
            model.addAttribute("needRefreshCode", true);
        }
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(passwd) || StringUtils.isEmpty(code)) {
            model.addAttribute("error", "请填写完整的登录信息");
            model.addAttribute("needRefreshCode", true);
        }
        return "login/login";
    }
    
    /**
     * 构造登录失败计数的维度：IP + 用户名。
     * <p>
     * 只按用户名计数，攻击者用 5 次错误密码就能把管理员永久锁死（连续锁定形成拒绝服务）；
     * 只按 IP 计数，在 NAT 或反向代理出口下会误伤同出口的正常用户。故两者结合。
     * <p>
     * 此处仅取 TCP 对端地址（remoteAddr），不信任 X-Forwarded-For——该头可被伪造，
     * 攻击者伪造即可绕过锁定策略。
     *
     * @param request  当前请求
     * @param userName 登录用户名
     * @return 计数维度 key
     */
    private String buildLockKey(HttpServletRequest request, String userName) {
        String ip = request.getRemoteAddr();
        if (StringUtils.isEmpty(ip)) {
            ip = "unknown";
        }
        return ip + "|" + userName;
    }

    /**
     * 记录登录失败次数
     *
     * @param lockKey 计数维度 key（IP|用户名）
     */
    private void recordLoginFail(String lockKey) {
        int failCount = loginFailMap.getOrDefault(lockKey, 0) + 1;
        loginFailMap.put(lockKey, failCount);

        // 如果失败次数达到限制，锁定账号
        if (failCount >= MAX_LOGIN_FAIL_COUNT) {
            loginLockMap.put(lockKey, System.currentTimeMillis());
        }

        // 容量保护：防止用大量随机用户名灌爆内存
        if (loginFailMap.size() > MAX_FAIL_RECORD_SIZE) {
            logger.warn("登录失败记录数达到上限 {}，整体清空以释放内存", MAX_FAIL_RECORD_SIZE);
            loginFailMap.clear();
            loginLockMap.clear();
        }
    }

    /**
     * 检查是否被锁定
     *
     * @param lockKey 计数维度 key（IP|用户名）
     * @return 是否被锁定
     */
    private boolean isLocked(String lockKey) {
        Long lockTime = loginLockMap.get(lockKey);
        if (lockTime == null) {
            return false;
        }

        // 检查锁定时间是否过期
        if (System.currentTimeMillis() - lockTime > LOGIN_LOCK_TIME) {
            // 锁定时间过期，清除锁定记录和失败次数
            loginLockMap.remove(lockKey);
            loginFailMap.remove(lockKey);
            return false;
        }

        return true;
    }


}
