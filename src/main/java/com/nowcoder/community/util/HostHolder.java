package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;
/**
 *持有用户的信息，用于代替session对象的。
 */
@Component
public class HostHolder {

    private ThreadLocal<User> users = new ThreadLocal<>();  //实现线程的隔离

    public void setUser(User user){
        users.set(user);
    }

    public User getUser(){
        return users.get();
    }

    //请求结束时，清理map
    public void clear(){
        users.remove();
    }


}
