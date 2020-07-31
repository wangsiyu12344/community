package com.nowcoder.community.config;

import com.nowcoder.community.quartz.AlphaJob;
import com.nowcoder.community.quartz.PostScoreRefreshJob;
import org.apache.ibatis.javassist.Loader;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

//配置 ->第一次的时候被读取到初始到内存中。若进行持久化则存入DB中。->scheduler之后都是根据存入的数据来调度
@Configuration
public class QuartzConfig {
    //BeanFactory : Ioc容器的顶层接口
    //FactoryBean:可简化Bean的实例化过程：
    //1.通过FactoryBean封装Bean的实例化过程
    //2.将FactoryBean装配到spring容器里
    //3.将FactoryBean注入给其他Bean
    //4.该Bean得到的是FactoryBean所管理的对象实例

    //配置jobDetail
   // @Bean
    public JobDetailFactoryBean alphaJobDetail(){
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(AlphaJob.class);
        factoryBean.setName("alphaJob");
        factoryBean.setGroup("alphaJobGroup");
        factoryBean.setDurability(true);
        //任务是否可恢复
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }
    //配置Trigger（SimpleTriggerFactoryBean, CronTriggerFactoryBean)
   // @Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail){
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(alphaJobDetail);
        factoryBean.setName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        //执行频率
        factoryBean.setRepeatInterval(3000);
        //存储job的状态
        factoryBean.setJobDataAsMap(new JobDataMap());
        return factoryBean;
    }
    //刷新帖子分数的任务
    //配置jobDetail
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail(){
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("PostScoreRefreshJob");
        factoryBean.setGroup("communityJobGroup");
        factoryBean.setDurability(true);
        //任务是否可恢复
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail){
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshJobDetail");
        factoryBean.setGroup("communityTriggerGroup");
        //执行频率
        factoryBean.setRepeatInterval(1000*60*5);
        //存储job的状态
        factoryBean.setJobDataAsMap(new JobDataMap());
        return factoryBean;
    }
}
