package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;

import com.nowcoder.community.dao.elasticsearch.DiscussRepository;

import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticSearchTests {

    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private DiscussRepository discussRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    public void testInsert(){
        discussRepository.save(discussMapper.selectDiscussPostById(241));
        discussRepository.save(discussMapper.selectDiscussPostById(242));
        discussRepository.save(discussMapper.selectDiscussPostById(243));
    }

    @Test
    public void testInsertList(){
        discussRepository.saveAll(discussMapper.selectionDiscussPosts(101, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectionDiscussPosts(102, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectionDiscussPosts(103, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectionDiscussPosts(111, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectionDiscussPosts(112, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectionDiscussPosts(131, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectionDiscussPosts(132, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectionDiscussPosts(133, 0, 100, 0));
        discussRepository.saveAll(discussMapper.selectionDiscussPosts(134, 0, 100, 0));
    }

    @Test
    //更改内容
    public void testupdate(){
        DiscussPost post = discussMapper.selectDiscussPostById(231);
        post.setContent("我是新人，使劲灌水");
        discussRepository.save(post);
    }

    @Test
    //删除
    public void testDelete(){
        //discussRepository.deleteById(231);
        //discussRepository.deleteAll();
    }

    @Test
    //搜索
    public void testSearchByRepository(){
        SearchQuery searchQuery = (SearchQuery) new NativeSearchQueryBuilder()
                //构造搜索条件
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                //构造排序条件
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                //构造分页条件 第一个条件：第几页。第二个条件，每页显示几条
                .withPageable(PageRequest.of(0, 10))
                //构造高亮显示的字段
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        //底层获取到了高亮显示的值，但没有做进一步的处理。可以用template的方法。
        //elasticTemplate.queryForPage(searchQuery, class, SearchResultMapper)
        Page<DiscussPost> page = discussRepository.search(searchQuery);
        //一共多少条数据
        System.out.println(page.getTotalElements());
        //一共多少页
        System.out.println(page.getTotalPages());
        //当前处在第几页
        System.out.println(page.getNumber());
        //每页显示多少数据
        System.out.println(page.getSize());
        //打印搜索出来的数据
        for(DiscussPost post : page){
            System.out.println(post);
        }



    }
    @Test
    //用elasticTemplate来实现搜索，更加完善(结果可以加标签)
    public void testSearchByTemplate(){
        SearchQuery searchQuery = (SearchQuery) new NativeSearchQueryBuilder()
                //构造搜索条件
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                //构造排序条件
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                //构造分页条件 第一个条件：第几页。第二个条件，每页显示几条
                .withPageable(PageRequest.of(0, 10))
                //构造高亮显示的字段
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        Page<DiscussPost> page = elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {

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
        //一共多少条数据
        System.out.println(page.getTotalElements());
        //一共多少页
        System.out.println(page.getTotalPages());
        //当前处在第几页
        System.out.println(page.getNumber());
        //每页显示多少数据
        System.out.println(page.getSize());
        //打印搜索出来的数据
        for(DiscussPost post : page){
            System.out.println(post);
        }

    }


}
