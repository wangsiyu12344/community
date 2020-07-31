package com.nowcoder.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class KafkaTests {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Test
    public void testKafka(){
        kafkaProducer.sendMessage("test", "你好");
        kafkaProducer.sendMessage("test", "在吗?");
        try {
            Thread.sleep(1000 );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
@Component
class KafkaProducer {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    public void sendMessage(String topic, String content){
        kafkaTemplate.send(topic,content);
    }

}
@Component
class Kafkaconsumer{

    @KafkaListener(topics = {"test"})
    //获得的消息封装在ConsumerRecord中，消费者消费是被动的，一旦队列中有消息，就自动处理
    public void handleMessage(ConsumerRecord record){
        System.out.println(record.value());
    }
}