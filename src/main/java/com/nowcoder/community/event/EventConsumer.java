package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticSearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.management.monitor.StringMonitor;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

//消费者是被动触发的，只要队列中有数据就会自动处理。所以我们只要主动调用producer就可以。
@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticSearchService elasticSearchService;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    //消费事件的方法，写一个方法，处理三个主题（评论，关注，点赞),把消息放入数据库中
    @KafkaListener(topics = {TOPIC_CONMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record){
        if(record==null || record.value()==null){
            logger.error("消息的内容为空!");
            return;
        }
        //解析JSON字符串为event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event==null){
            logger.error("消息的格式错误!");
            return;
        }
        //构造一个Message数据插入到Message表中,发送系统消息，from_id永远为1。
        //发送系统消息(站内通知),把event转化为message
        Message message = new Message();
        message.setFromId(SYSTEM_USERID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());
        //如果event的map中有数据:遍历其中的数据，放入content这个map中
        if(!event.getData().isEmpty()){
            for(String key : event.getData().keySet()){
                content.put(key, event.getData().get(key));
            }
        }
        //把这个map转化为JSON字符串，存入message对象的content中，再插入Message表内
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    //消费发帖，发贴评论的事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        if(record==null || record.value()==null){
            logger.error("消息的内容为空!");
            return;
        }
        //解析JSON字符串为event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event==null){
            logger.error("消息的格式错误!");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticSearchService.saveDiscussPost(post);

    }

    //消费删帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handleDeleteMessage(ConsumerRecord record){
        if(record==null || record.value()==null){
            logger.error("消息的内容为空!");
            return;
        }
        //解析JSON字符串为event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event==null){
            logger.error("消息的格式错误!");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticSearchService.deleteDiscussPost(post.getId());

    }

    //消费分享事件
    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord record){
        if(record==null || record.value()==null){
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event==null){
            logger.error("消息的格式错误!");
            return;
        }
        String htmlUrl = (String)event.getData().get("htmlUrl");
        String fileName = (String)event.getData().get("fileName");
        String suffix = (String)event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 75 " + htmlUrl + " "+ wkImageStorage + "/" +fileName
                + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功" + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败" + e.getMessage());
        }

        //启用定时器，监视该图片，一旦图片生成了，就上传至七牛云服务器。
        UploadTask task = new UploadTask(fileName, suffix);
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
        task.setFuture(future);
    }

    class UploadTask implements  Runnable{

        //文件名称
        private String fileName;
        //文件后缀
        private String suffix;
        //启动任务的返回值，可以用来停止定时器
        private Future future;
        //开始时间
        private long startTime;
        //上传次数
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            //生成图片失败
            if(System.currentTimeMillis()-startTime >30000){
                logger.error("执行时间过长，终止任务" + fileName);
                future.cancel(true);
                return;
            }
            //上传失败
            if(uploadTimes >= 3){
                logger.error("上传次数过多，终止任务" + fileName);
                future.cancel(true);
                return;
            }
            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if(file.exists()){
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
                //设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJsonString(0));
                //生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);

                //指定上传机房
                UploadManager manager = new UploadManager(new Configuration(Zone.zone2()));
                try{
                    //开始上传图片
                    Response response = manager.put(path, fileName, uploadToken, null, "image/"+ suffix.substring(suffix.lastIndexOf(".")+1), false);
                    //处理响应结果
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if(json == null || json.get("code")==null || !json.get("code").toString().equals("0")){
                        logger.info(String.format("第[%d]次上传失败[%s]", uploadTimes, fileName));
                    }else{
                        logger.info(String.format("第[%d]次上传成功[%s]", uploadTimes,fileName));
                        future.cancel(true);
                    }
                }catch (QiniuException e){
                    logger.info(String.format("第%d次上传失败[%s]", uploadTimes,fileName));
                }

            }else{
                logger.info("等待图片生成["+fileName+"].");
            }
        }

        public void setFuture(Future future) {
            this.future = future;
        }
    }

}
