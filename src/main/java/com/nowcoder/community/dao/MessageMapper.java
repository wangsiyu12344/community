package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper {

    //查询当前用户的会话列表,针对每个会话只返回最新的私信
    List<Message> selectConversations(int userId, int offset, int limit);

    //查询当前用户的会话数量
    int selectConversationCount(int userId);

    //查询某个会话的具体内容
    List<Message> selectLetters(String conversationId, int offset, int limit);
    //查询该会话所包含的私信数量
    int selectLetterCount(String conversationId);

    //查询未读的消息数量（某个用户的）
    int selectLetterUnreadCount(int userId, String conversationId);

    //发私信
    int insertMessage(Message message);

    //设置未读消息为已读(修改消息的状态)
    int updateStatus(List<Integer> ids, int status);

    //查询某个主题的最新通知
    Message selectLatestNotice(int userId, String topic);
    //查询某个主题所包含的通知的总数
    int selectNoticeCount(int userId, String topic);
    //显示未读的通知数量
    int selectNoticeUnreadCount(int userId, String topic);

    //查询某个主题的通知列表
    List<Message> selectNotices(int userId, String topic, int offset, int limit);



}
