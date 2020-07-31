package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    @Autowired
    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    //caffeine核心接口：Cache, 子接口:LoadingCache(同步缓存), AsyncLoadingCache
    //帖子列表的缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    //帖子总行数的缓存
    private LoadingCache<Integer , Integer> postRowsCache;

    //定义初始化方法,在构造方法之后执行
    @PostConstruct
    public void init(){
        //初始化帖子列表的缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        //查询数据库得到初始化的方法
                        if(key == null || key.length()==0){
                            throw new IllegalArgumentException("参数错误!");
                        }
                       String[] params = key.split(":");
                        if(params == null || params.length != 2){
                            throw new IllegalArgumentException("参数错误!");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        //二级缓存 ：Redis ，如果Redis中没有该数据，再访问数据库
                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectionDiscussPosts(0, offset, limit, 1);
                    }
                });
        //初始化帖子总数的缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }


    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode){
        //只想缓存访问首页时的最热帖子，即orderMode==1并且userId==0时。
        //userId==0&&orderMode==1时，才启用缓存
        if(userId==0&&orderMode==1){
            return postListCache.get(offset + ":" + limit);
        }
        //否则，不启用缓存，从数据库中访问数据
        logger.debug("load post list from DB.");
        return discussPostMapper.selectionDiscussPosts(userId, offset, limit, orderMode);
    }

    public int findDiscussPostRows(int userId){
        //userId==0时说明是访问主页的情况。
        if(userId==0){
            return postRowsCache.get(userId);
        }
        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost discussPost){
        if(discussPost == null){
            throw new IllegalArgumentException("参数不能为空");
        }

        //转义HTML标签
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));

        //过滤敏感词
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));

        return discussPostMapper.insertDiscussPost(discussPost);
    }


    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    //更新帖子的数量
    public int updateCommentCount(int id, int commentCount){
        return discussPostMapper.updateCommentCount(id, commentCount);
    }


    public int updateType(int id, int type){
        return discussPostMapper.updateType(id, type);
    }
    public int updateStatus(int id, int status){
        return discussPostMapper.updateStatus(id, status);

    }
    public int updateScore(int id , double score){
        return discussPostMapper.updateScore(id, score);
    }
}
