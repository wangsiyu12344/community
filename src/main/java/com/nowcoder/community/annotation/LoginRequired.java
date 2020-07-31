package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//该注解的执行范围：方法
@Target(ElementType.METHOD)
//该注解的执行时机:运行时
@Retention(RetentionPolicy.RUNTIME)
//在需要该注解的方法上写上该注解即可。这个注解的功能是：判断该方法是否是需要登陆才能使用的方法。
public @interface LoginRequired {

}
