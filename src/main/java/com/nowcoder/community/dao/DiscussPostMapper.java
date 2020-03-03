package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    //通过userId来查找他所发表的帖子, 有关分页相关的信息:offset表示每页的起始行号， limit表示每页多少条信息
    List<DiscussPost> selectionDiscussPosts(int userId, int offset, int limit);
    //返回的一共多少条数据
    //@Param用于给参数取别名，如果方法只有一个参数并且在<if>里使用，那么必须加别名。
    int selectDiscussPostRows(@Param("userId") int userId);


}
