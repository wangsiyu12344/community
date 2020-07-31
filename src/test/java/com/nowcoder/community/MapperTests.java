package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTests {
    @Autowired
    private UserMapper userMapper;

    @Test
    public void testSelectUser(){
       User user = userMapper.selectById(101);
        System.out.println(user);
    }

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Test
    public void testSelectPosts(){
        //若传入的Id为0，那么说明不按id查询。
        List<DiscussPost> discussPosts = discussPostMapper.selectionDiscussPosts(101, 0, 10, 0);
        for(DiscussPost post: discussPosts){
            System.out.println(post);
        }

        int rows = discussPostMapper.selectDiscussPostRows(101);
        System.out.println(rows);
    }


}
