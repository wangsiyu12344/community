package com.nowcoder.community.config;

import com.nowcoder.community.controller.interceptor.DataInceptor;
import com.nowcoder.community.controller.interceptor.LoginRequiredInterceptor;
import com.nowcoder.community.controller.interceptor.LoginTicketInterceptor;
import com.nowcoder.community.controller.interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration                      //如果要使用拦截器，配置类必须实现WebMvcConfigurer接口
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;

    //@Autowired
   // private LoginRequiredInterceptor loginRequiredInterceptor;

    @Autowired
    private MessageInterceptor messageInterceptor;

    @Autowired
    private DataInceptor dataInceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js","/**/*.png", "/**/*.jpg", "/**/*.jepg");
        //registry.addInterceptor(loginRequiredInterceptor)
                //不去拦截静态的资源
                //.excludePathPatterns("/**/*.css", "/**/*.js","/**/*.png", "/**/*.jpg", "/**/*.jepg");
        registry.addInterceptor(messageInterceptor)
                //不去拦截静态的资源
                .excludePathPatterns("/**/*.css", "/**/*.js","/**/*.png", "/**/*.jpg", "/**/*.jepg");
        registry.addInterceptor(dataInceptor)
                //不去拦截静态的资源
                .excludePathPatterns("/**/*.css", "/**/*.js","/**/*.png", "/**/*.jpg", "/**/*.jepg");
    }
}
