package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    //实现点赞的业务方法 + 记录用户获得的数量,entityUserId是被赞的人的userId
    public void like(int userId, int entityType, int entityId, int entityUserId){
 //       String entityLikekey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
 //       //判断userId在不在集合set中，若存在，那么他已点过赞，则取消点赞，若不存在，那么他没赞过，则点赞
 //       boolean isMember = redisTemplate.opsForSet().isMember(entityLikekey, userId);
 //       if(isMember){
 //           //已经点过赞
 //           redisTemplate.opsForSet().remove(entityLikekey, userId);
 //       }else{
 //           redisTemplate.opsForSet().add(entityLikekey, userId);
 //       }
 //   }
           redisTemplate.execute(new SessionCallback(){
               @Override
               public Object execute(RedisOperations operations) throws DataAccessException {
                   String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                   String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
                   //查询操作必须写在启用事务之前。
                   boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);
                   operations.multi();
                   if(isMember){
                       //该用户已经赞过
                       operations.opsForSet().remove(entityLikeKey, userId);
                       operations.opsForValue().decrement(userLikeKey);
                   }else {
                       operations.opsForSet().add(entityLikeKey, userId);
                       operations.opsForValue().increment(userLikeKey);
                   }

                   return operations.exec();
               }

           });
    }


    //查询某实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId){
        String entityLikekey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikekey);
    }

    //查询某人对某实体的点赞状态 1:点了赞 ， 0：没点赞
    public int findEntityLikeStatus(int userId, int entityType, int entityId){
        String entityLikekey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikekey, userId)?1:0;
    }

    //查询某用户获得的赞的数量
    public int findUserLikeCount(int userId){
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count =  (Integer)redisTemplate.opsForValue().get(userLikeKey);
        return count == null? 0 : count;
    }

}
