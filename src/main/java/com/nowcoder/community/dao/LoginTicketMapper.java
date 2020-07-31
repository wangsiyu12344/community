package com.nowcoder.community.dao;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
@Deprecated
public interface LoginTicketMapper {
    //用注解方式实现,可以少写xml文件,系统会自动拼接这几个字符串（注意字符串直接要打个空格)
    //用注解方式也支持动态sql

    @Insert({
            "insert into login_ticket (user_id, ticket, status, expired) ",
            "values (#{userId}, #{ticket}, #{status}, #{expired})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({
            "select id, user_id, ticket, status, expired ",
            "from login_ticket ",
            "where ticket = #{ticket}"
    })
    LoginTicket selectByTicket(String ticket);

    @Update({
            "update login_ticket set status = #{status} where ticket=#{ticket}"
    })
    //退出时，凭证失效(改变status)
    int updateStatus(String ticket, int status);



}
