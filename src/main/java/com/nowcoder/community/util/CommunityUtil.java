package com.nowcoder.community.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.Map;
import java.util.UUID;

public class CommunityUtil {

    //生成随机字符串，在很多地方可用，比如说上传头像的文件名，注册时的激活码等。
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    //MD5加密，MD5算法：只能加密，不能解密
    //hello -> abc123def456
    //hello + salt(五位随机的字符串）-> 黑客无法知晓的密码
    public static String md5(String key){
        if(StringUtils.isBlank(key)){
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

    public static String getJSONString(int code, String msg, Map<String , Object> map){
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("msg", msg);
        if(map != null){
            for(String key : map.keySet()){
                json.put(key, map.get(key));
            }
        }
        return json.toJSONString();
    }
    //方法重载
    public static String getJSONString(int code, String msg){
        return getJSONString(code, msg,null );
    }

    public static String getJsonString(int code){
        return getJSONString(code, null, null);
        //把json字符串传给浏览器，浏览器将之转换为js对象，以此实现前后端的交互
    }

}
