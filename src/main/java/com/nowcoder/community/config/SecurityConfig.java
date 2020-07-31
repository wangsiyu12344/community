package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {


    //忽略对静态资源的拦截
    @Override
    public void configure(WebSecurity web) throws Exception {
       web.ignoring().antMatchers("/resources/**");

    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //授权
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add/",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                ).hasAnyAuthority(AUTHORITY_USER, AUTHORITY_ADMIN, AUTHORITY_MODERATOR)
                .antMatchers("/discuss/top","/discuss/wonderful")
                .hasAnyAuthority(AUTHORITY_MODERATOR)
                .antMatchers("/discuss/delete",
                        "/data/**")
                .hasAnyAuthority(AUTHORITY_ADMIN)

                //除了这些请求以外，其他任何请求都允许。
                .anyRequest().permitAll()
                .and().csrf().disable();

        //权限不够时的处理
        http.exceptionHandling()
                //若你未登录，需要认证，进入authenticationEntryPoint组件中
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if("XMLHttpRequest".equals(xRequestedWith)){
                            //异步请求    //plain表示普通的字符串，但是需要是JSON格式
                            response.setContentType("application/plain;charset=utf8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你还没有登录!"));
                        }else{
                            //若不是异步请求，直接重定向（返回html）
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                //登录却权限不足时
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if("XMLHttpRequest".equals(xRequestedWith)){
                            //异步请求    //plain表示普通的字符串，但是需要是JSON格式
                            response.setContentType("application/plain;charset=utf8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限!"));
                        }else{
                            //若不是异步请求，直接重定向（返回html）
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });

        //security底层会自动拦截logout请求，security是filter实现的，在拦截器之前
        //覆盖它默认的逻辑，才能执行我们自己的代码
        http.logout().logoutUrl("/securitylogout");

    }
}
