package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.awt.*;
import java.util.*;
import java.util.List;

@Controller
@RequestMapping(path = "/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private UserService userService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content){
        User user = hostHolder.getUser();
        if(user == null){
            return CommunityUtil.getJSONString(403, "你还没有登录!");
        }
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);

        //触发发帖事件，把新发布的帖子存入ES服务器中
        Event event = new Event().setTopic(TOPIC_PUBLISH).setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(discussPost.getId());
        //发布事件
        eventProducer.fireEvent(event);
        //计算帖子的分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, discussPost.getId());



        //报错的情况，将来统一处理。
        return CommunityUtil.getJSONString(0, "发布成功!");
    }

    //帖子详情，不仅要查询帖子页面，还要查询该帖子下的评论
    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        //查询帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);
        //查帖子的作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);
        //点赞数量
        long likeCount = likeService.findEntityLikeCount(1, discussPostId);
        model.addAttribute("likeCount", likeCount);
        //点赞状态
        int likeStatus = hostHolder.getUser()==null? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), 1, discussPostId);
        model.addAttribute("likeStatus", likeStatus);


        //查帖子评论的信息]
        //设置分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + post.getId());
        //得到帖子的评论数量（与回复数量区分开)
        page.setRows(post.getCommentCount());
        //得到帖子的评论集合
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        //commentVoList是评论的列表
        List<Map<String , Object>>  commentVoList = new ArrayList<>();
        if(commentList != null){
            //遍历，得到每一个帖子对象
            for(Comment comment : commentList){
                //commentVO是一个评论的VO
                Map<String, Object> commentVo = new HashMap<>();
                User u = userService.findUserById(comment.getUserId());
                //评论及评论的用户放入commentVo中
                commentVo.put("comment", comment);
                commentVo.put("user", u);
                //点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                //点赞状态
                likeStatus = hostHolder.getUser()==null? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);

                //查找每一条评论下的回复，放入commentVo中
                List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                //回复的Vo列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if(replyList != null){
                    //循环每条评论下的回复，查找其有没有回复的回复
                    for(Comment reply : replyList){
                        Map<String, Object> replyVo = new HashMap<>();
                        replyVo.put("reply", reply);
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        //回复的目标,判断target_id是不是为0,如果为0，则没有回复具体的某一个人
                        User target = reply.getTargetId() == 0? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);

                        //点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        //点赞状态
                        likeStatus = hostHolder.getUser()==null? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);

                        replyVoList.add(replyVo);
                    }
                }

                commentVo.put("replys", replyVoList);
                //回复数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);
                //往集合中添加commentVo
                commentVoList.add(commentVo);
            }

        }

        model.addAttribute("comments", commentVoList);

        return  "/site/discuss-detail";
    }

    //置顶
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateType(id,1 );
        //同步到ES中
        //触发置顶事件，把新置顶的帖子存入ES服务器中
        Event event = new Event().setTopic(TOPIC_PUBLISH).setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJsonString(0);
    }
    //加精
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){
        discussPostService.updateStatus(id,1 );
        //同步到ES中
        //触发加精事件，把新加精的帖子存入ES服务器中
        Event event = new Event().setTopic(TOPIC_PUBLISH).setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
        eventProducer.fireEvent(event);
        //算分
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);

        return CommunityUtil.getJsonString(0);
    }

    //删除(拉黑)
    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateStatus(id,2 );
        //同步到ES中
        //触发删帖事件
        Event event = new Event().setTopic(TOPIC_DELETE).setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJsonString(0);
    }
}
