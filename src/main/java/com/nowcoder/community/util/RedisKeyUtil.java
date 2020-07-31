package com.nowcoder.community.util;

import org.springframework.stereotype.Component;

public class RedisKeyUtil {

    private static final String SPLIT = ":";
    //实体（帖子和评论)赞的前缀
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    //某个人的点赞数前缀
    private static final String PREFIX_USER_LIKE = "like:user";
    //关注的目标
    private static final String PREFIX_FOLLOWEE = "followee";
    //关注别人的人:粉丝
    private static final String PREFIX_FOLLOWER = "follower";
    //存验证码到redis上
    private static final String PREFIX_KAPTCHA = "kaptcha";

    private static final String PREFIX_TICKET = "ticket";

    private static final String PREFIX_USER = "user";
    //unique visitor
    private static final String PREFIX_UV = "uv";
    //daily active user
    private static final String PREFIX_DAU = "dau";

    private static final String PREFIX_POST = "post";

    //某个实体的赞
    //like:entity:entityType:entityId ->用set来保存value，set中装入userId
    public static String getEntityLikeKey(int entityType, int entityId){
        return PREFIX_ENTITY_LIKE + SPLIT +entityType + SPLIT + entityId;
    }

    //某个用户的赞
    //like:user:userId -> 存一个数
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    //某个用户关注的实体(人，帖子等）
    //followee:userId:entityType -> zset(entityId, now) ,写上时间的原因是为了方便排序
    public static String getFolloweeKey(int userId, int entityType){
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    //某个实体拥有的粉丝
    //follower:entityType:entityId -> zset(userId, now),写上时间的原因是为了方便排序
    public static String getFollowerKey(int entityType, int entityId){
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }
    //拼验证码的key
    //传入一个Cookie给用户，用来标识某个用户，因为在登录前还不知道那个用户是谁,自然无法取到他的userId。
    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    //登录的凭证
    public static String getTicketKey(String ticket){
        return PREFIX_TICKET + SPLIT + ticket;
    }

    //用户
    public static String getUserKey(int userId){
        return PREFIX_USER + SPLIT + userId;
    }

    //返回对应的key:单日UV
    public static String getUVKey(String date){
        return PREFIX_UV + SPLIT + date;

    }
    //区间UV
    public static String getUVKey(String startDate, String endDate){
        return PREFIX_UV + SPLIT + startDate +SPLIT + endDate;
    }

    //单日的活跃用户
    public static String getDAUKey(String date){
        return PREFIX_DAU + SPLIT +date;
    }
    //区间活跃用户
    public static String getDAUKey(String startDate, String endDate){
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }


    //返回统计帖子分数的key
    public static String getPostScoreKey(){
        return PREFIX_POST + SPLIT + "score";
    }
}
