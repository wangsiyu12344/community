package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    //在每次请求前判断你是否登录，如果有@LoginRequired注解
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod){  //判断请求是否是方法
            HandlerMethod handlerMethod = (HandlerMethod) handler;  //强制类型转换
            Method method = handlerMethod.getMethod(); //得到该方法
            //得到该方法的LoginRequired注解，如果该方法没有LoginRequired注解的话，返回null。
            LoginRequired annotation = method.getAnnotation(LoginRequired.class);
            if(annotation != null && hostHolder.getUser() == null){  //需要登录却没有登录
                response.sendRedirect(request.getContextPath() + "/login");
                return false; //不接着往下执行
            }
        }
        return true;//往下执行请求

    }
}
