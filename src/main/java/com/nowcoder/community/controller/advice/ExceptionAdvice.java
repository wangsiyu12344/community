package com.nowcoder.community.controller.advice;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

@ControllerAdvice(annotations = Controller.class)  //用于统一处理带有@Controller注解的方法产生的异常
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler({Exception.class})
    public void handleException(Exception e, HttpServletResponse response, HttpServletRequest request) throws IOException {  //异常为Contoller中抛出的
        logger.error("服务器发生异常"+ e.getMessage());
        //e.getStackTrace:打印出错误栈信息，返回值为一个数组
        for(StackTraceElement element: e.getStackTrace()){
            logger.error(element.toString());
        }
        //分别处理同步的和异步的请求。
        //判断请求是同步请求还是异步请求:
        String xRequestedWith = request.getHeader("x-requested-with");
        if("XMLHttpRequest".equals(xRequestedWith)){
            response.setContentType("application/plain;charset=utf-8");  //向浏览器返回的是一个普通的字符串
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常"));
        }else{
            response.sendRedirect(request.getContextPath()+"/error");
        }
    }

}
