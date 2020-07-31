package com.nowcoder.community;

import org.junit.Test;


import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ThreadPoolTests {

    public static final Logger logger = LoggerFactory.getLogger(ThreadPoolTests.class);

    //JDK普通线程池
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    //JDK执行定时任务的线程池
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    //spring线程池
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    //Spring可执行定时任务的线程池
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;


    private void sleep(long m){
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //1.JDK的线程池
    @Test
    public void testExecutorService(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello, ExecutorService");
            }
        };
        for(int i = 0; i< 10; i++){
            //当调用submit方法时，线程池会分配一个线程来执行task
            executorService.submit(task);

        }
        sleep(10000);
    }
    //2.JDK定时任务线程池
    @Test
    public void testScheduledExecutorService(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello, ScheduledExecutorService");
            }
        };
        //以固定的频率执行
        scheduledExecutorService.scheduleAtFixedRate(task, 10000, 1000, TimeUnit.MILLISECONDS);

        sleep(30000);
    }

    //3.spring自带的普通线程池
    @Test
    public void testThreadPoolTaskExecutor(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello, ThreadPoolTaskExecutor");
            }
        };
        for(int i = 0; i<10 ; i++){
            taskExecutor.submit(task);
        }
        sleep(10000);
    }
    //4.Spring定时任务线程池
    @Test
    public void testThreadPoolTaskScheduler(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello, ThreadPoolTaskExecutor");
            }
        };
        Date startTime = new Date(System.currentTimeMillis()+10000);
        taskScheduler.scheduleAtFixedRate(task, startTime, 1000);

        sleep(30000);
    }

}
