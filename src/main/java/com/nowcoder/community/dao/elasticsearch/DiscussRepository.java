package com.nowcoder.community.dao.elasticsearch;

import com.nowcoder.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

//ES可以被看成一个特殊的数据库
@Repository                                                     //你要处理的实体类和其主键类型
public interface DiscussRepository extends ElasticsearchRepository<DiscussPost, Integer> {


}
