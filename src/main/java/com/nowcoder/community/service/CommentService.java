package com.nowcoder.community.service;

import com.nowcoder.community.dao.CommentMapper;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit){
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    public int findCommentCount(int entityType, int entityId){
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    //处理增加评论的业务（这个方法包括两步，第一步是添加评论，第二步是更新评论的数量，他们是一个事务）
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        if(comment == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        //把html标签进行转义，敏感词过滤，再存入数据库中
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));

        int rows = commentMapper.insertComment(comment);

        //添加完评论后，更新帖子的评论数量
        if(comment.getEntityType() == ENTITY_TYPE_POST){
           int count = commentMapper.selectCountByEntity(ENTITY_TYPE_POST, comment.getEntityId());
           discussPostService.updateCommentCount(comment.getEntityId(), count);
        }
        return rows;
    }

    public Comment findCommentById(int id){
        return commentMapper.selectCommentById(id);
    }
}
