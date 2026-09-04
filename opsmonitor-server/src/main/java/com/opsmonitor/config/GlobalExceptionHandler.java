package com.opsmonitor.config;

import cn.hutool.json.JSONUtil;
import com.opsmonitor.dto.MessageDto;
import com.opsmonitor.service.LogInfoService;
import com.opsmonitor.util.staticvar.StaticKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器，用于捕获和处理系统中的所有异常
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Resource
    private LogInfoService logInfoService;
    
    /**
     * 处理所有未捕获的异常
     * @param request 请求对象
     * @param e 异常对象
     * @return 错误页面
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(HttpServletRequest request, Exception e) {
        logger.error("系统异常：", e);
        logInfoService.save("系统异常", e.toString(), StaticKeys.LOG_ERROR);
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("error", "系统内部错误，请联系管理员");
        modelAndView.addObject("url", request.getRequestURL());
        modelAndView.setViewName("error/500");
        
        return modelAndView;
    }
    
    /**
     * 处理Ajax请求的异常
     * @param request 请求对象
     * @param e 异常对象
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public String handleRuntimeException(HttpServletRequest request, RuntimeException e) {
        logger.error("运行时异常：", e);
        logInfoService.save("运行时异常", e.toString(), StaticKeys.LOG_ERROR);
        
        MessageDto messageDto = new MessageDto();
        messageDto.setCode("1");
        messageDto.setMsg("系统内部错误，请联系管理员");
        
        return JSONUtil.toJsonStr(messageDto);
    }
}
