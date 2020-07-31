package com.nowcoder.community.service;

import com.nowcoder.community.dao.elasticsearch.DiscussRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ElasticSearchService {

    @Autowired
    private DiscussRepository discussRepository;

    @Autowired          //要想高亮显示，要使用这个组件
    private ElasticsearchTemplate elasticTemplate;

    //向ES服务器提交新生的帖子
    public void saveDiscussPost(DiscussPost discussPost){
        discussRepository.save(discussPost);
    }

    //删除存在ES服务器的帖子
    public void deleteDiscussPost(int id){
        discussRepository.deleteById(id);
    }

    //搜索方法
    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit){
        SearchQuery searchQuery = (SearchQuery) new NativeSearchQueryBuilder()
                //构造搜索条件
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                //构造排序条件
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                //构造分页条件 第一个条件：第几页。第二个条件，每页显示几条
                .withPageable(PageRequest.of(current, limit))
                //构造高亮显示的字段
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        return elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {

            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                SearchHits hits = response.getHits();
                if(hits.getTotalHits()<=0){
                    return null;
                }
                List<DiscussPost> list = new ArrayList<>();
                for(SearchHit hit : hits){
                    //hit被封装为了map
                    DiscussPost discussPost = new DiscussPost();
                    String id = hit.getSourceAsMap().get("id").toString();
                    discussPost.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    discussPost.setUserId(Integer.valueOf(userId));
                    //这是原始的title,不是带有高亮标签的title
                    String title = hit.getSourceAsMap().get("title").toString();
                    discussPost.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    discussPost.setTitle(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    discussPost.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    discussPost.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    discussPost.setCommentCount(Integer.valueOf(commentCount));

                    //处理高亮显示的结果
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if(titleField != null){
                        discussPost.setTitle(titleField.getFragments()[0].toString());
                    }
                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if(contentField != null){
                        discussPost.setContent(contentField.getFragments()[0].toString());
                    }


                    list.add(discussPost);
                }
                return new AggregatedPageImpl(list,pageable,hits.getTotalHits(), response.getAggregations(),response.getScrollId(),hits.getMaxScore());
            }

            @Override
            public <T> T mapSearchHit(SearchHit searchHit, Class<T> aClass) {
                return null;
            }
        });

    }

}
