package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    public void follow(int userId, int entityType, int entityId){
        //事务：第一步是粉丝关注该实体，第二步是该实体拥有该粉丝
        redisTemplate.execute(new SessionCallback(){

            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                //启用事务
                operations.multi();
                //
                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());

                return operations.exec();
            }
        });
    }
    //取关
    public void unfollow(int userId, int entityType, int entityId){
        redisTemplate.execute(new SessionCallback(){

            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                //启用事务
                operations.multi();
                //
                operations.opsForZSet().remove(followeeKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);

                return operations.exec();
            }
        });
    }
    //统计某个用户关注的实体的数量
    //followee:userId:entityType -> zset(entityId, now) ,写上时间的原因是为了方便排序
    public long findFolloweeCount(int userId, int entityType){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    //查询实体（帖子，用户等）的粉丝数量
    //follower:entityType:entityId -> zset(userId, now),写上时间的原因是为了方便排序
    public long findFollowerCount(int entityType, int entityId){
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    //显示当前用户是否已关注该实体，score方法为查询某个key的某个值的分数，如果该值不存在，则返回null
    public boolean hasFollowed(int userId, int entityType, int entityId){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        Double score = redisTemplate.opsForZSet().score(followeeKey, entityId);
        return score != null;
    }

    //查询某个用户关注的人
    public List<Map<String, Object>> findFollowees(int userId, int offset , int limit){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        //得到他关注的人的id集合
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        if(targetIds == null){
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for(Integer targetId : targetIds){
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            //取到这个用户对应的分数(时间)
            Double now = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime", new Date(now.longValue()));
            list.add(map);
        }
        return list;
    }
    //查询某个用户的粉丝
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit){
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset+limit-1);
        if(targetIds == null){
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for(Integer targetId : targetIds){
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            //取到这个用户对应的分数(时间)
            Double now = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(now.longValue()));
            list.add(map);
        }
        return list;
    }
}
